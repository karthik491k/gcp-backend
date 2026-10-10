package com.logancee.globalcart.service;

import com.logancee.globalcart.dto.CreateOrderPayload;
import com.logancee.globalcart.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final SpannerDataService spannerDataService;

    public OrderService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public Order createOrder(String userEmail, CreateOrderPayload payload) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        if (payload.getItems() == null || payload.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order with an empty shopping bag.");
        }

        // Server-Side Price & Inventory Validation
        double validatedSubtotal = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < payload.getItems().size(); i++) {
            CartItem cartItem = payload.getItems().get(i);
            Product product = spannerDataService.getProductById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProduct().getName()));

            if (!product.isInStock() || product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Product " + product.getName() + " is out of stock for requested quantity.");
            }

            double itemTotalPrice = product.getPrice() * cartItem.getQuantity();
            validatedSubtotal += itemTotalPrice;

            orderItems.add(OrderItem.builder()
                    .id("oi-" + System.currentTimeMillis() + "-" + i)
                    .productId(product.getId())
                    .productName(product.getName())
                    .productImage(product.getImages().get(0))
                    .brand(product.getBrand())
                    .color(cartItem.getColor())
                    .size(cartItem.getSize())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotalPrice)
                    .build());

            // Reserve/Deduct Inventory happens atomically inside spannerDataService.saveOrder(...)
        }

        double validatedShippingFee = payload.getShippingFee();
        double validatedTax = Math.round(validatedSubtotal * 0.05);
        double validatedTotal = Math.max(0, validatedSubtotal - payload.getDiscount() + validatedShippingFee + validatedTax);

        String orderId = "ord-" + UUID.randomUUID().toString().substring(0, 8);
        String orderNumber = "LOG-" + (100000 + new Random().nextInt(900000));

        List<OrderStatusTimeline> timeline = Arrays.asList(
                OrderStatusTimeline.builder().status(OrderStatus.ORDER_PLACED).completed(true).description("Order successfully confirmed").timestamp(new Date().toString()).build(),
                OrderStatusTimeline.builder().status(OrderStatus.CONFIRMED).completed(true).description("Payment authorized & queued for fulfillment").timestamp(new Date().toString()).build(),
                OrderStatusTimeline.builder().status(OrderStatus.PACKED).completed(false).description("Awaiting luxury packing").build(),
                OrderStatusTimeline.builder().status(OrderStatus.SHIPPED).completed(false).description("Awaiting dispatch").build(),
                OrderStatusTimeline.builder().status(OrderStatus.OUT_FOR_DELIVERY).completed(false).description("Out for delivery").build(),
                OrderStatusTimeline.builder().status(OrderStatus.DELIVERED).completed(false).description("Estimated within 3 business days").build()
        );

        Order newOrder = Order.builder()
                .id(orderId)
                .orderNumber(orderNumber)
                .userId(user.getId())
                .items(orderItems)
                .shippingAddress(payload.getShippingAddress())
                .paymentMethod(payload.getPaymentMethod())
                .paymentStatus(payload.getPaymentMethod() == PaymentMethodType.COD ? PaymentStatus.PENDING : PaymentStatus.COMPLETED)
                .orderStatus(OrderStatus.ORDER_PLACED)
                .timeline(timeline)
                .subtotal(validatedSubtotal)
                .discount(payload.getDiscount())
                .shippingFee(validatedShippingFee)
                .tax(validatedTax)
                .totalAmount(validatedTotal)
                .createdAt(new Date().toString())
                .estimatedDelivery("3 Business Days from today")
                .trackingNumber("BD" + (1000000 + new Random().nextInt(9000000)) + "IN")
                .canCancel(true)
                .canReturn(false)
                .build();

        spannerDataService.saveOrder(newOrder);

        // Clear User Cart after successful order placement
        spannerDataService.getUserCart(user.getId()).getItems().clear();

        return newOrder;
    }

    public List<Order> getOrdersForUser(String userEmail) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated"));
        return spannerDataService.getUserOrders(user.getId());
    }

    public Order getOrderById(String orderId) {
        return spannerDataService.getOrderById(orderId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Order not found with id: " + orderId));
    }

    public Order getOrderByIdForUser(String userEmail, String orderId, Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
        Order order = getOrderById(orderId);
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated"));

        boolean isAdmin = authorities != null && authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

        if (!isAdmin && !order.getUserId().equals(user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied to order details");
        }
        return order;
    }

    public Order cancelOrder(String userEmail, String orderId, String reason) {
        Order order = getOrderById(orderId);
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not authenticated"));

        if (!order.getUserId().equals(user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Access denied to order cancellation");
        }

        return spannerDataService.cancelOrderTransactional(orderId, reason);
    }
}
