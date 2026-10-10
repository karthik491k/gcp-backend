package com.logancee.globalcart.service;

import com.logancee.globalcart.model.Product;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private final SpannerDataService spannerDataService;

    public InventoryService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public Map<String, Object> checkStock(String productId, int qty) {
        Product prod = spannerDataService.getProductById(productId).orElse(null);
        Map<String, Object> res = new HashMap<>();
        res.put("productId", productId);
        res.put("inStock", prod != null && prod.isInStock() && prod.getStockQuantity() >= qty);
        res.put("availableQuantity", prod != null ? prod.getStockQuantity() : 0);
        return res;
    }

    public boolean reserveStock(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return true;
        }

        // 1. Validate stock availability for all requested items
        for (Map<String, Object> item : items) {
            String productId = (String) item.get("productId");
            Number qtyNum = (Number) item.get("quantity");
            int quantity = qtyNum != null ? qtyNum.intValue() : 0;
            if (productId == null || quantity <= 0) continue;

            Product p = spannerDataService.getProductById(productId).orElse(null);
            if (p == null || !p.isInStock() || p.getStockQuantity() < quantity) {
                return false;
            }
        }

        // 2. Safe decrement stock
        for (Map<String, Object> item : items) {
            String productId = (String) item.get("productId");
            Number qtyNum = (Number) item.get("quantity");
            int quantity = qtyNum != null ? qtyNum.intValue() : 0;
            if (productId == null || quantity <= 0) continue;

            spannerDataService.getProductById(productId).ifPresent(p -> {
                int newStock = Math.max(0, p.getStockQuantity() - quantity);
                spannerDataService.updateStock(productId, newStock);
            });
        }
        return true;
    }
}
