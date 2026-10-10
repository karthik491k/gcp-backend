package com.logancee.globalcart.dto;

import lombok.Data;

public class CartItemRequest {

    @Data
    public static class AddCartItemRequest {
        private String productId;
        private String color;
        private String size;
        private int quantity = 1;
    }

    @Data
    public static class UpdateCartItemRequest {
        private int quantity;
        private String color;
        private String size;
    }

    @Data
    public static class ApplyCouponRequest {
        private String code;
    }
}
