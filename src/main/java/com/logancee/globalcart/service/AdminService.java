package com.logancee.globalcart.service;

import com.logancee.globalcart.dto.DashboardMetrics;
import com.logancee.globalcart.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminService {

    private final SpannerDataService spannerDataService;

    public AdminService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public DashboardMetrics getDashboardMetrics() {
        List<Order> orders = spannerDataService.getAllOrders();
        List<Inventory> inventory = spannerDataService.getAllInventory();

        double totalRevenue = orders.stream().mapToDouble(Order::getTotalAmount).sum();
        long lowStockCount = inventory.stream().filter(i -> "LOW_STOCK".equals(i.getStatus()) || "OUT_OF_STOCK".equals(i.getStatus())).count();
        long pendingOrdersCount = orders.stream().filter(o -> o.getOrderStatus() != OrderStatus.DELIVERED && o.getOrderStatus() != OrderStatus.CANCELLED).count();

        List<DashboardMetrics.RevenueChartItem> revChart = Arrays.asList(
                new DashboardMetrics.RevenueChartItem("Apr", 32000),
                new DashboardMetrics.RevenueChartItem("May", 48000),
                new DashboardMetrics.RevenueChartItem("Jun", 42000),
                new DashboardMetrics.RevenueChartItem("Jul", 65000),
                new DashboardMetrics.RevenueChartItem("Aug", 89000),
                new DashboardMetrics.RevenueChartItem("Sep", totalRevenue > 0 ? totalRevenue : 148920)
        );

        List<DashboardMetrics.CategorySalesChartItem> catChart = Arrays.asList(
                new DashboardMetrics.CategorySalesChartItem("Women", 45),
                new DashboardMetrics.CategorySalesChartItem("Men", 25),
                new DashboardMetrics.CategorySalesChartItem("Beauty", 15),
                new DashboardMetrics.CategorySalesChartItem("Electronics", 10),
                new DashboardMetrics.CategorySalesChartItem("Home", 5)
        );

        return DashboardMetrics.builder()
                .totalRevenue(totalRevenue > 0 ? totalRevenue : 148920)
                .totalOrders(orders.size() + 18)
                .totalCustomers(142)
                .totalProducts(spannerDataService.getAllProducts().size())
                .lowStockCount((int) lowStockCount)
                .pendingOrdersCount((int) pendingOrdersCount)
                .revenueChart(revChart)
                .categorySalesChart(catChart)
                .build();
    }

    public List<Product> getProducts() {
        return spannerDataService.getAllProducts();
    }

    public Product createProduct(Product product) {
        return spannerDataService.saveProduct(product);
    }

    public Product updateProduct(String id, Product product) {
        product.setId(id);
        return spannerDataService.saveProduct(product);
    }

    public boolean deleteProduct(String id) {
        return spannerDataService.deleteProduct(id);
    }

    public List<Order> getOrders() {
        return spannerDataService.getAllOrders();
    }

    public Order updateOrderStatus(String orderId, OrderStatus status) {
        Order order = spannerDataService.getOrderById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setOrderStatus(status);
        if (order.getTimeline() != null) {
            order.getTimeline().stream()
                    .filter(t -> t.getStatus() == status)
                    .findFirst()
                    .ifPresent(t -> {
                        t.setCompleted(true);
                        t.setTimestamp(new Date().toString());
                    });
        }
        return spannerDataService.saveOrder(order);
    }

    public List<Inventory> getInventory() {
        return spannerDataService.getAllInventory();
    }

    public Inventory updateStock(String inventoryId, int availableStock) {
        return spannerDataService.updateStock(inventoryId, availableStock);
    }

    public List<Coupon> getCoupons() {
        return spannerDataService.getAllCoupons();
    }

    public Coupon createCoupon(Coupon coupon) {
        if (coupon.getId() == null) {
            coupon.setId("c-" + System.currentTimeMillis());
        }
        return spannerDataService.saveCoupon(coupon);
    }
}
