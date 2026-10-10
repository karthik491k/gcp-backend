package com.logancee.globalcart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {
    private double totalRevenue;
    private int totalOrders;
    private int totalCustomers;
    private int totalProducts;
    private int lowStockCount;
    private int pendingOrdersCount;
    private List<RevenueChartItem> revenueChart;
    private List<CategorySalesChartItem> categorySalesChart;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RevenueChartItem {
        private String month;
        private double amount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategorySalesChartItem {
        private String category;
        private int value;
    }
}
