package com.logancee.globalcart.service;

import com.logancee.globalcart.model.Product;
import com.logancee.globalcart.model.User;
import com.logancee.globalcart.model.WishlistItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistService {

    private final SpannerDataService spannerDataService;

    public WishlistService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public List<WishlistItem> getWishlist(String userEmail) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return spannerDataService.getUserWishlist(user.getId());
    }

    public List<WishlistItem> addToWishlist(String userEmail, String productId) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        Product product = spannerDataService.getProductById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return spannerDataService.addToWishlist(user.getId(), product);
    }

    public List<WishlistItem> removeFromWishlist(String userEmail, String productId) {
        User user = spannerDataService.findUserByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        return spannerDataService.removeFromWishlist(user.getId(), productId);
    }
}
