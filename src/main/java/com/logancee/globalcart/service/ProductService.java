package com.logancee.globalcart.service;

import com.logancee.globalcart.dto.PaginatedResponse;
import com.logancee.globalcart.model.Brand;
import com.logancee.globalcart.model.Category;
import com.logancee.globalcart.model.Product;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final SpannerDataService spannerDataService;

    public ProductService(SpannerDataService spannerDataService) {
        this.spannerDataService = spannerDataService;
    }

    public PaginatedResponse<Product> getProducts(
            String category,
            String subcategory,
            List<String> brand,
            List<String> color,
            List<String> size,
            Double minPrice,
            Double maxPrice,
            Double rating,
            Boolean inStock,
            Integer minDiscount,
            String sortBy,
            String searchQuery,
            Integer page,
            Integer limit
    ) {
        List<Product> filtered = spannerDataService.getAllProducts();

        if (category != null && !category.trim().isEmpty()) {
            String catLower = category.toLowerCase();
            filtered = filtered.stream()
                    .filter(p -> p.getCategory().toLowerCase().equals(catLower))
                    .collect(Collectors.toList());
        }

        if (subcategory != null && !subcategory.trim().isEmpty()) {
            String subLower = subcategory.toLowerCase();
            filtered = filtered.stream()
                    .filter(p -> p.getSubcategory().toLowerCase().equals(subLower))
                    .collect(Collectors.toList());
        }

        if (brand != null && !brand.isEmpty()) {
            List<String> brandLowers = brand.stream().map(String::toLowerCase).collect(Collectors.toList());
            filtered = filtered.stream()
                    .filter(p -> brandLowers.contains(p.getBrand().toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (color != null && !color.isEmpty()) {
            List<String> colorLowers = color.stream().map(String::toLowerCase).collect(Collectors.toList());
            filtered = filtered.stream()
                    .filter(p -> p.getColorOptions() != null && p.getColorOptions().stream()
                            .anyMatch(c -> colorLowers.stream().anyMatch(cl -> c.getName().toLowerCase().contains(cl))))
                    .collect(Collectors.toList());
        }

        if (size != null && !size.isEmpty()) {
            List<String> sizeLowers = size.stream().map(String::toLowerCase).collect(Collectors.toList());
            filtered = filtered.stream()
                    .filter(p -> p.getSizes() != null && p.getSizes().stream()
                            .anyMatch(s -> sizeLowers.contains(s.toLowerCase())))
                    .collect(Collectors.toList());
        }

        if (minPrice != null) {
            filtered = filtered.stream()
                    .filter(p -> p.getPrice() >= minPrice)
                    .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            filtered = filtered.stream()
                    .filter(p -> p.getPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        if (rating != null) {
            filtered = filtered.stream()
                    .filter(p -> p.getRating() >= rating)
                    .collect(Collectors.toList());
        }

        if (inStock != null && inStock) {
            filtered = filtered.stream()
                    .filter(Product::isInStock)
                    .collect(Collectors.toList());
        }

        if (minDiscount != null) {
            filtered = filtered.stream()
                    .filter(p -> p.getDiscountPercentage() >= minDiscount)
                    .collect(Collectors.toList());
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = searchQuery.toLowerCase().trim();
            filtered = filtered.stream()
                    .filter(p -> p.getName().toLowerCase().contains(q) ||
                            p.getBrand().toLowerCase().contains(q) ||
                            p.getCategory().toLowerCase().contains(q) ||
                            p.getSubcategory().toLowerCase().contains(q) ||
                            (p.getTags() != null && p.getTags().stream().anyMatch(t -> t.toLowerCase().contains(q))))
                    .collect(Collectors.toList());
        }

        // Sorting
        if (sortBy != null) {
            switch (sortBy) {
                case "price_low":
                    filtered.sort(Comparator.comparingDouble(Product::getPrice));
                    break;
                case "price_high":
                    filtered.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                    break;
                case "newest":
                    filtered.sort((a, b) -> Boolean.compare(b.isNew(), a.isNew()));
                    break;
                case "rating":
                    filtered.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
                    break;
                case "discount":
                    filtered.sort((a, b) -> Integer.compare(b.getDiscountPercentage(), a.getDiscountPercentage()));
                    break;
                default:
                    filtered.sort((a, b) -> Integer.compare(b.getReviewCount(), a.getReviewCount()));
            }
        }

        int p = page != null && page > 0 ? page : 1;
        int l = limit != null && limit > 0 ? limit : 12;
        int startIndex = (p - 1) * l;
        int total = filtered.size();
        List<Product> paginated = startIndex < total ? filtered.subList(startIndex, Math.min(startIndex + l, total)) : Collections.emptyList();

        return PaginatedResponse.<Product>builder()
                .content(paginated)
                .totalElements(total)
                .totalPages((int) Math.ceil((double) total / l))
                .pageNumber(p)
                .pageSize(l)
                .build();
    }

    public Product getProductById(String id) {
        return spannerDataService.getProductById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Product not found with id: " + id));
    }

    public Map<String, Object> searchProducts(String query) {
        String q = query != null ? query.toLowerCase().trim() : "";
        Map<String, Object> result = new HashMap<>();
        if (q.isEmpty()) {
            result.put("products", Collections.emptyList());
            result.put("categories", Collections.emptyList());
            result.put("brands", Collections.emptyList());
            return result;
        }

        List<Product> products = spannerDataService.getAllProducts().stream()
                .filter(p -> p.getName().toLowerCase().contains(q) ||
                        p.getBrand().toLowerCase().contains(q) ||
                        p.getCategory().toLowerCase().contains(q) ||
                        p.getSubcategory().toLowerCase().contains(q) ||
                        (p.getTags() != null && p.getTags().stream().anyMatch(t -> t.toLowerCase().contains(q))))
                .collect(Collectors.toList());

        List<Category> categories = spannerDataService.getAllCategories().stream()
                .filter(c -> c.getName().toLowerCase().contains(q) || c.getSlug().toLowerCase().contains(q))
                .collect(Collectors.toList());

        List<Brand> brands = spannerDataService.getAllBrands().stream()
                .filter(b -> b.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());

        result.put("products", products);
        result.put("categories", categories);
        result.put("brands", brands);
        return result;
    }

    public List<Category> getCategories() {
        return spannerDataService.getAllCategories();
    }

    public List<Brand> getBrands() {
        return spannerDataService.getAllBrands();
    }
}
