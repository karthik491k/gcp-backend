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
public class Review {
    private String id;
    private String productId;
    private String userId;
    private String userName;
    private String userAvatar;
    private int rating;
    private String title;
    private String comment;
    private String date;
    private List<String> images;
    private boolean verifiedPurchase;
    private int helpfulCount;
}
