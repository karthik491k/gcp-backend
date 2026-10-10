package com.logancee.globalcart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    private String id;
    private String code;
    private int discountPercentage;
    private double maxDiscount;
    private double minOrderAmount;
    private String expiryDate;
    private int usageLimit;
    private int usageCount;
    private boolean isActive;
}
