package com.logancee.globalcart.controller;

import com.logancee.globalcart.dto.CreateOrderPayload;
import com.logancee.globalcart.model.Order;
import com.logancee.globalcart.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(Authentication auth, @RequestBody CreateOrderPayload payload) {
        return ResponseEntity.ok(orderService.createOrder(auth.getName(), payload));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getOrdersForUser(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(Authentication auth, @PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderByIdForUser(auth.getName(), id, auth.getAuthorities()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(Authentication auth, @PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(orderService.cancelOrder(auth.getName(), id, reason));
    }
}
