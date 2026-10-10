package com.logancee.globalcart.controller;

import com.logancee.globalcart.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> checkStock(@PathVariable String productId, @RequestParam(defaultValue = "1") int qty) {
        return ResponseEntity.ok(inventoryService.checkStock(productId, qty));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Boolean> reserveStock(@RequestBody Map<String, List<Map<String, Object>>> body) {
        return ResponseEntity.ok(inventoryService.reserveStock(body.get("items")));
    }
}
