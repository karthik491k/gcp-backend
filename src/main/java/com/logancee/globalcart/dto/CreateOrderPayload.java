package com.logancee.globalcart.dto;

import com.logancee.globalcart.model.Address;
import com.logancee.globalcart.model.CartItem;
import com.logancee.globalcart.model.PaymentMethodType;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderPayload {
    private Address shippingAddress;
    private PaymentMethodType paymentMethod;
    private List<CartItem> items;
    private double subtotal;
    private double discount;
    private double shippingFee;
    private double tax;
    private double totalAmount;
}
