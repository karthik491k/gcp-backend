package com.logancee.globalcart.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private int rating;
    private String title;
    private String comment;
}
