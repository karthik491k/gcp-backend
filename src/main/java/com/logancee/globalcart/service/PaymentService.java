package com.logancee.globalcart.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    public Map<String, Object> createPaymentOrder(double amount, String orderId) {
        Map<String, Object> result = new HashMap<>();
        String txnId = "SIM-TXN-" + System.currentTimeMillis();
        result.put("gateway", "SIMULATED_PAYMENT_GATEWAY");
        result.put("transactionId", txnId);
        result.put("amount", amount);
        result.put("currency", "INR");
        result.put("status", "CREATED");
        return result;
    }

    public Map<String, Object> verifyPayment(Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        boolean isFail = payload != null && "FAIL".equalsIgnoreCase(payload.get("simulateStatus"));
        String status = isFail ? "FAILED" : "SUCCESS";
        String txnId = payload != null && payload.containsKey("transactionId") ? payload.get("transactionId") : "SIM-TXN-SUCCESS";

        result.put("gateway", "SIMULATED_PAYMENT_GATEWAY");
        result.put("status", status);
        result.put("transactionId", txnId);
        result.put("message", isFail ? "Simulated payment declined." : "Simulated payment processed successfully.");
        return result;
    }
}
