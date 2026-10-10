package com.logancee.globalcart.controller;

import com.logancee.globalcart.dto.CartItemRequest;
import com.logancee.globalcart.model.Cart;
import com.logancee.globalcart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCart(auth.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<Cart> addItem(Authentication auth, @RequestBody CartItemRequest.AddCartItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(auth.getName(), req));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<Cart> updateQuantity(Authentication auth, @PathVariable String productId, @RequestBody CartItemRequest.UpdateCartItemRequest req) {
        return ResponseEntity.ok(cartService.updateQuantity(auth.getName(), productId, req));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Cart> removeItem(Authentication auth, @PathVariable String productId,
                                           @RequestParam(required = false) String color,
                                           @RequestParam(required = false) String size) {
        return ResponseEntity.ok(cartService.removeItem(auth.getName(), productId, color, size));
    }

    @DeleteMapping
    public ResponseEntity<Cart> clearCart(Authentication auth) {
        return ResponseEntity.ok(cartService.clearCart(auth.getName()));
    }

    @PostMapping("/coupon")
    public ResponseEntity<Map<String, Object>> applyCoupon(Authentication auth, @RequestBody CartItemRequest.ApplyCouponRequest req) {
        return ResponseEntity.ok(cartService.applyCoupon(auth.getName(), req.getCode()));
    }
}
