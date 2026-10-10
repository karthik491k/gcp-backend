package com.logancee.globalcart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String id;
    private String productId;
    private String productName;
    private String productImage;
    private String brand;
    private String color;
    private String size;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
}
