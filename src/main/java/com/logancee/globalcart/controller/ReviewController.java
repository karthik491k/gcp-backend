package com.logancee.globalcart.controller;

import com.logancee.globalcart.dto.ReviewRequest;
import com.logancee.globalcart.model.Review;
import com.logancee.globalcart.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getProductReviews(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<Review> addReview(Authentication auth, @PathVariable String productId, @RequestBody ReviewRequest req) {
        return ResponseEntity.ok(reviewService.addReview(auth.getName(), productId, req));
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<Integer> voteHelpful(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.voteHelpful(id));
    }
}
