package com.logancee.globalcart.controller;

import com.logancee.globalcart.dto.PaginatedResponse;
import com.logancee.globalcart.model.Brand;
import com.logancee.globalcart.model.Category;
import com.logancee.globalcart.model.Product;
import com.logancee.globalcart.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<PaginatedResponse<Product>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) List<String> brand,
            @RequestParam(required = false) List<String> color,
            @RequestParam(required = false) List<String> size,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer limit
    ) {
        return ResponseEntity.ok(productService.getProducts(
                category, subcategory, brand, color, size, minPrice, maxPrice, rating, inStock, minDiscount, sortBy, searchQuery, page, limit
        ));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/products/search")
    public ResponseEntity<Map<String, Object>> searchProducts(@RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(productService.searchProducts(q));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(productService.getCategories());
    }

    @GetMapping("/brands")
    public ResponseEntity<List<Brand>> getBrands() {
        return ResponseEntity.ok(productService.getBrands());
    }
}
