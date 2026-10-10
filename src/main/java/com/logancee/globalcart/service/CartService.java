package com.logancee.globalcart.service;

import com.logancee.globalcart.dto.CartItemRequest;
import com.logancee.globalcart.model.Cart;
import com.logancee.globalcart.model.CartItem;
import com.logancee.globalcart.model.Coupon;
import com.logancee.globalcart.model.Product;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CartService {

    private final SpannerDataService spannerDataService;

    public CartService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public Cart getCart(String userEmail) {
        String userId = getUserId(userEmail);
        return spannerDataService.getUserCart(userId);
    }

    public Cart addItem(String userEmail, CartItemRequest.AddCartItemRequest req) {
        String userId = getUserId(userEmail);
        Cart cart = spannerDataService.getUserCart(userId);
        Product product = spannerDataService.getProductById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.isInStock() || product.getStockQuantity() < req.getQuantity()) {
            throw new RuntimeException("Requested quantity unavailable in stock");
        }

        List<CartItem> items = cart.getItems();
        if (items == null) {
            items = new ArrayList<>();
            cart.setItems(items);
        }

        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getProduct().getId().equals(product.getId())
                        && Objects.equals(i.getColor(), req.getColor())
                        && Objects.equals(i.getSize(), req.getSize()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.getQuantity());
            item.setSubtotal(item.getQuantity() * product.getPrice());
        } else {
            CartItem newItem = CartItem.builder()
                    .id("item-" + System.currentTimeMillis())
                    .product(product)
                    .color(req.getColor() != null ? req.getColor() : "Standard")
                    .size(req.getSize() != null ? req.getSize() : "Standard")
                    .quantity(req.getQuantity())
                    .price(product.getPrice()) // Server-side validated price
                    .subtotal(product.getPrice() * req.getQuantity())
                    .build();
            items.add(newItem);
        }

        return spannerDataService.saveCart(userId, cart);
    }

    public Cart updateQuantity(String userEmail, String productId, CartItemRequest.UpdateCartItemRequest req) {
        String userId = getUserId(userEmail);
        Cart cart = spannerDataService.getUserCart(userId);

        if (req.getQuantity() <= 0) {
            return removeItem(userEmail, productId, req.getColor(), req.getSize());
        }

        Product product = spannerDataService.getProductById(productId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Product not found: " + productId));

        if (req.getQuantity() > product.getStockQuantity()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Requested quantity (" + req.getQuantity() + ") exceeds available stock (" + product.getStockQuantity() + ") for " + product.getName());
        }

        List<CartItem> items = cart.getItems();
        if (items != null) {
            items.stream()
                    .filter(i -> i.getProduct().getId().equals(productId)
                            && (req.getColor() == null || Objects.equals(i.getColor(), req.getColor()))
                            && (req.getSize() == null || Objects.equals(i.getSize(), req.getSize())))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setQuantity(req.getQuantity());
                        item.setSubtotal(item.getPrice() * req.getQuantity());
                    });
        }

        return spannerDataService.saveCart(userId, cart);
    }

    public Cart removeItem(String userEmail, String productId, String color, String size) {
        String userId = getUserId(userEmail);
        Cart cart = spannerDataService.getUserCart(userId);

        if (cart.getItems() != null) {
            cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId)
                    && (color == null || color.isEmpty() || Objects.equals(i.getColor(), color))
                    && (size == null || size.isEmpty() || Objects.equals(i.getSize(), size)));
        }

        return spannerDataService.saveCart(userId, cart);
    }

    public Cart clearCart(String userEmail) {
        String userId = getUserId(userEmail);
        Cart cart = spannerDataService.getUserCart(userId);
        cart.getItems().clear();
        cart.setAppliedCoupon(null);
        cart.setCouponCode(null);
        return spannerDataService.saveCart(userId, cart);
    }

    public Map<String, Object> applyCoupon(String userEmail, String code) {
        String userId = getUserId(userEmail);
        Cart cart = spannerDataService.getUserCart(userId);

        Coupon coupon = spannerDataService.findCouponByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid or expired promo code."));

        if (!coupon.isActive()) {
            throw new RuntimeException("Promo code is no longer active.");
        }

        if (cart.getSubtotal() < coupon.getMinOrderAmount()) {
            throw new RuntimeException("Minimum order amount of ₹" + coupon.getMinOrderAmount() + " required to use this coupon.");
        }

        cart.setCouponCode(coupon.getCode());
        cart.setAppliedCoupon(coupon);
        Cart updated = spannerDataService.saveCart(userId, cart);

        Map<String, Object> response = new HashMap<>();
        response.put("cart", updated);
        response.put("message", "Coupon " + coupon.getCode() + " applied successfully! Saved " + coupon.getDiscountPercentage() + "%");
        return response;
    }

    private String getUserId(String email) {
        return spannerDataService.findUserByEmail(email)
                .map(u -> u.getId())
                .orElse("user-guest");
    }
}
