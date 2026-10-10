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
public class Order {
    private String id;
    private String orderNumber;
    private String userId;
    private List<OrderItem> items;
    private Address shippingAddress;
    private PaymentMethodType paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private List<OrderStatusTimeline> timeline;
    private double subtotal;
    private double discount;
    private double shippingFee;
    private double tax;
    private double totalAmount;
    private String createdAt;
    private String estimatedDelivery;
    private String trackingNumber;
    private Boolean canCancel;
    private Boolean canReturn;
}
