package com.logancee.globalcart.controller;

import com.logancee.globalcart.model.WishlistItem;
import com.logancee.globalcart.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<List<WishlistItem>> getWishlist(Authentication auth) {
        return ResponseEntity.ok(wishlistService.getWishlist(auth.getName()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<List<WishlistItem>> addToWishlist(Authentication auth, @PathVariable String productId) {
        return ResponseEntity.ok(wishlistService.addToWishlist(auth.getName(), productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<List<WishlistItem>> removeFromWishlist(Authentication auth, @PathVariable String productId) {
        return ResponseEntity.ok(wishlistService.removeFromWishlist(auth.getName(), productId));
    }
}
