package com.logancee.globalcart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String createdAt;
    private boolean read;
    private String type; // ORDER, WISHLIST, PROMO, SYSTEM
    private String link;
}
