package com.logancee.globalcart.controller;

import com.logancee.globalcart.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPaymentOrder(@RequestBody Map<String, Object> req) {
        double amount = Double.parseDouble(req.getOrDefault("amount", 0.0).toString());
        String orderId = (String) req.get("orderId");
        return ResponseEntity.ok(paymentService.createPaymentOrder(amount, orderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(paymentService.verifyPayment(payload));
    }
}
