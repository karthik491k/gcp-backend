package com.logancee.globalcart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    private String id;
    private String productId;
    private String productName;
    private String sku;
    private String category;
    private int availableStock;
    private int reservedStock;
    private int totalSold;
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
}
