package com.logancee.globalcart.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String name;
    private String brand;
    private String category;
    private String subcategory;
    private double price;
    private double originalPrice;
    private int discountPercentage;
    private double rating;
    private int reviewCount;
    private String description;
    private Map<String, String> specifications;
    private String sizeAndFit;
    private String shippingInfo;
    private String returnPolicy;
    private List<ColorOption> colorOptions;
    private List<String> sizes;
    private List<String> images;
    private String hoverImage;

    @JsonProperty("isNew")
    private boolean isNew;

    @JsonProperty("isTrending")
    private boolean isTrending;

    @JsonProperty("isBestSeller")
    private boolean isBestSeller;

    @JsonProperty("isFlashSale")
    private boolean isFlashSale;
    private String flashSaleEndTime;
    private boolean inStock;
    private int stockQuantity;
    private String sku;
    private List<String> tags;
}
