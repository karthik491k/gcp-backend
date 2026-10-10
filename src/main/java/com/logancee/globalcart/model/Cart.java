package com.logancee.globalcart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    private String id;
    private List<CartItem> items;
    private double subtotal;
    private double discount;
    private double shipping;
    private double tax;
    private double total;
    private String couponCode;
    private Coupon appliedCoupon;
}
