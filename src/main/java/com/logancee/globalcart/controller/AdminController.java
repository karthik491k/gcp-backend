package com.logancee.globalcart.controller;

import com.logancee.globalcart.dto.DashboardMetrics;
import com.logancee.globalcart.model.*;
import com.logancee.globalcart.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardMetrics> getDashboardMetrics() {
        return ResponseEntity.ok(adminService.getDashboardMetrics());
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(adminService.getProducts());
    }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(adminService.createProduct(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        return ResponseEntity.ok(adminService.updateProduct(id, product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Boolean> deleteProduct(@PathVariable String id) {
        return ResponseEntity.ok(adminService.deleteProduct(id));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrders() {
        return ResponseEntity.ok(adminService.getOrders());
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable String orderId, @RequestBody Map<String, String> body) {
        OrderStatus status = OrderStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(adminService.updateOrderStatus(orderId, status));
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<Inventory>> getInventory() {
        return ResponseEntity.ok(adminService.getInventory());
    }

    @PutMapping("/inventory/{inventoryId}")
    public ResponseEntity<Inventory> updateStock(@PathVariable String inventoryId, @RequestBody Map<String, Integer> body) {
        int availableStock = body.get("availableStock");
        return ResponseEntity.ok(adminService.updateStock(inventoryId, availableStock));
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<Coupon>> getCoupons() {
        return ResponseEntity.ok(adminService.getCoupons());
    }

    @PostMapping("/coupons")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(adminService.createCoupon(coupon));
    }
}
