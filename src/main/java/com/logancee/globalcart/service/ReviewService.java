package com.logancee.globalcart.service;

import com.logancee.globalcart.dto.ReviewRequest;
import com.logancee.globalcart.model.Review;
import com.logancee.globalcart.model.User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReviewService {

    private final SpannerDataService spannerDataService;

    public ReviewService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public List<Review> getProductReviews(String productId) {
        return spannerDataService.getProductReviews(productId);
    }

    public Review addReview(String userEmail, String productId, ReviewRequest req) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Review newReview = Review.builder()
                .id("rev-" + System.currentTimeMillis())
                .productId(productId)
                .userId(user.getId())
                .userName(user.getName())
                .userAvatar(user.getAvatar())
                .rating(req.getRating())
                .title(req.getTitle())
                .comment(req.getComment())
                .date("Today")
                .verifiedPurchase(true)
                .helpfulCount(0)
                .build();

        return spannerDataService.saveReview(newReview);
    }

    public int voteHelpful(String reviewId) {
        // Vote helpful counter increment
        return 1;
    }
}
