package com.logancee.globalcart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.*;
import com.logancee.globalcart.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Google Cloud Spanner Data Access & Seeder Service.
 * Implements actual Cloud Spanner reads, writes, and transactions for all 18 tables:
 * Users, UserAddresses, Categories, Brands, Products, ProductColorOptions, ProductImages,
 * ProductSizes, Carts, CartItems, WishlistItems, Orders, OrderItems, OrderTimeline,
 * Reviews, Coupons, Inventory, and Notifications.
 */
@Service
public class SpannerDataService {

    private static final Logger log = LoggerFactory.getLogger(SpannerDataService.class);

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SpannerDataService(DatabaseClient databaseClient, ObjectMapper objectMapper) {
        this.databaseClient = databaseClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initDatabaseSeed() {
        try {
            log.info("Checking Cloud Spanner database seeding status...");
            long userCount = countTableRows("Users");
            long prodCount = countTableRows("Products");
            
            if (userCount > 0 && prodCount > 0) {
                log.info("Cloud Spanner database already contains existing records (Users: {}, Products: {}). Skipping initial seed.", userCount, prodCount);
                return;
            }

            log.info("Cloud Spanner database tables are empty. Executing initial database seed into Cloud Spanner...");
            seedCategories();
            seedBrands();
            seedUsers();
            seedProducts();
            seedCoupons();
            seedOrders();
            seedNotifications();
            log.info("Initial Cloud Spanner database seed completed successfully!");
        } catch (Exception e) {
            log.error("Error during Cloud Spanner database seeding: {}", e.getMessage(), e);
        }
    }

    private long countTableRows(String tableName) {
        try (ResultSet rs = databaseClient.singleUse().executeQuery(Statement.of("SELECT COUNT(*) FROM `" + tableName + "`"))) {
            if (rs.next()) {
                return rs.getLong(0);
            }
        } catch (Exception e) {
            log.warn("Could not query table count for {}: {}", tableName, e.getMessage());
        }
        return 0;
    }

    private KeySet prefixKeySet(Key key) {
        return KeySet.newBuilder().addRange(KeyRange.prefix(key)).build();
    }

    // =========================================================================
    // SEED INITIAL DATA INTO CLOUD SPANNER
    // =========================================================================

    private void seedCategories() {
        if (countTableRows("Categories") >= 6) return;

        List<Mutation> mutations = new ArrayList<>();
        for (Category c : ProductSeedData.getCategories()) {
            mutations.add(Mutation.newInsertOrUpdateBuilder("Categories")
                    .set("category_id").to(c.getId())
                    .set("name").to(c.getName())
                    .set("slug").to(c.getSlug())
                    .set("image_url").to(c.getImage())
                    .set("description").to(c.getDescription())
                    .set("item_count").to(c.getItemCount())
                    .build());
        }
        databaseClient.write(mutations);
    }

    private void seedBrands() {
        if (countTableRows("Brands") >= 10) return;

        List<Mutation> mutations = new ArrayList<>();
        for (Brand b : ProductSeedData.getBrands()) {
            mutations.add(Mutation.newInsertOrUpdateBuilder("Brands")
                    .set("brand_id").to(b.getId())
                    .set("name").to(b.getName())
                    .set("logo_url").to(b.getLogo() != null ? b.getLogo() : "")
                    .set("category").to(b.getCategory())
                    .build());
        }
        databaseClient.write(mutations);
    }

    private void seedUsers() {
        if (countTableRows("Users") > 0) return;

        User customer = User.builder().id("user-101").email("elena.rostova@logancee.fashion")
                .password(passwordEncoder.encode("Customer@123")).name("Elena Rostova")
                .phone("+91 98765 43210").avatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80")
                .role(Role.CUSTOMER).createdAt("2026-01-15T10:00:00Z").build();

        User admin = User.builder().id("user-admin").email("admin@logancee.com")
                .password(passwordEncoder.encode("Admin@123")).name("Logancee Administrator")
                .phone("+91 99999 88888").avatar("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80")
                .role(Role.ADMIN).createdAt("2026-01-01T00:00:00Z").build();

        saveUser(customer);
        saveUser(admin);

        Address addr1 = Address.builder().id("addr-1").userId(customer.getId()).fullName("Elena Rostova")
                .street("45 Jubilee Hills, Road No. 36").city("Hyderabad").state("Telangana").zipCode("500033")
                .country("India").phone("+91 98765 43210").isDefault(true).type("Home").build();

        Address addr2 = Address.builder().id("addr-2").userId(customer.getId()).fullName("Elena Rostova (Office)")
                .street("Cyber Towers, 7th Floor, HITEC City").city("Hyderabad").state("Telangana").zipCode("500081")
                .country("India").phone("+91 98765 43210").isDefault(false).type("Work").build();

        saveUserAddress(customer.getId(), addr1);
        saveUserAddress(customer.getId(), addr2);
    }

    private void seedProducts() {
        if (countTableRows("Products") >= 100) return;

        log.info("Seeding full 114 product catalog into Cloud Spanner...");
        for (Product p : ProductSeedData.getProducts()) {
            saveProduct(p);
        }
        log.info("Finished seeding products into Cloud Spanner!");
    }

    private void seedCoupons() {
        if (countTableRows("Coupons") > 0) return;

        Coupon c1 = Coupon.builder().id("coup-1").code("WELCOME10").discountPercentage(10).maxDiscount(1500)
                .minOrderAmount(2000).expiryDate("2026-12-31").usageLimit(1000).usageCount(42).isActive(true).build();

        Coupon c2 = Coupon.builder().id("coup-2").code("LUXURY20").discountPercentage(20).maxDiscount(5000)
                .minOrderAmount(10000).expiryDate("2026-12-31").usageLimit(500).usageCount(18).isActive(true).build();

        Coupon c3 = Coupon.builder().id("coup-3").code("SUMMER15").discountPercentage(15).maxDiscount(3000)
                .minOrderAmount(5000).expiryDate("2026-12-31").usageLimit(300).usageCount(5).isActive(true).build();

        for (Coupon c : Arrays.asList(c1, c2, c3)) {
            saveCoupon(c);
        }
    }

    private void seedOrders() {
        if (countTableRows("Orders") > 0) return;

        Address addr = Address.builder().id("addr-1").userId("user-101").fullName("Elena Rostova")
                .street("45 Jubilee Hills, Road No. 36").city("Hyderabad").state("Telangana").zipCode("500033")
                .country("India").phone("+91 98765 43210").isDefault(true).type("Home").build();

        OrderItem item1 = OrderItem.builder().id("oi-101").productId("prod-1")
                .productName("Monogram Cashmere Wool Overcoat")
                .productImage("https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&w=800&q=80")
                .brand("LOGANCEE Atelier").color("Onyx Black").size("L").quantity(1).unitPrice(12999).totalPrice(12999).build();

        Order o1 = Order.builder().id("ord-1001").orderNumber("LOG-849201").userId("user-101").items(Collections.singletonList(item1))
                .shippingAddress(addr).paymentMethod(PaymentMethodType.CARD).paymentStatus(PaymentStatus.COMPLETED)
                .orderStatus(OrderStatus.CONFIRMED)
                .timeline(Arrays.asList(
                        OrderStatusTimeline.builder().status(OrderStatus.ORDER_PLACED).completed(true).description("Placed on 26 Sep 2026, 10:30 AM").build(),
                        OrderStatusTimeline.builder().status(OrderStatus.CONFIRMED).completed(true).description("Payment verified and order accepted").build(),
                        OrderStatusTimeline.builder().status(OrderStatus.PACKED).completed(false).description("Preparing items at warehouse").build(),
                        OrderStatusTimeline.builder().status(OrderStatus.DELIVERED).completed(false).description("Estimated within 3 business days").build()
                ))
                .subtotal(12999).discount(0).shippingFee(0).tax(650).totalAmount(13649)
                .createdAt("2026-09-26T10:30:00Z").estimatedDelivery("3 Business Days").trackingNumber("BD9481920IN")
                .canCancel(true).canReturn(false).build();

        saveOrder(o1);
    }

    private void seedNotifications() {
        if (countTableRows("Notifications") > 0) return;

        NotificationItem n1 = NotificationItem.builder().id("notif-1").title("Order Confirmed")
                .message("Your order LOG-849201 has been confirmed and queued for packing.").createdAt("10 mins ago")
                .read(false).type("ORDER").link("/orders").build();

        NotificationItem n2 = NotificationItem.builder().id("notif-2").title("Exclusive Member Voucher")
                .message("Use code WELCOME20 for 20% off your autumn wardrobe selection.").createdAt("1 day ago")
                .read(true).type("PROMO").build();

        saveNotification("user-101", n1);
        saveNotification("user-101", n2);
    }

    // =========================================================================
    // USER & ADDRESS CLOUD SPANNER OPERATIONS
    // =========================================================================

    public Optional<User> findUserByEmail(String email) {
        Statement stmt = Statement.newBuilder("SELECT user_id, email, password_hash, name, phone, avatar_url, role, created_at FROM Users WHERE email = @email")
                .bind("email").to(email).build();

        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            if (rs.next()) {
                return Optional.of(mapUser(rs.getCurrentRowAsStruct()));
            }
        }
        return Optional.empty();
    }

    public User saveUser(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId("user-" + System.currentTimeMillis());
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
        }

        String encodedPass = user.getPassword();
        if (encodedPass != null && !encodedPass.startsWith("$2a$") && !encodedPass.startsWith("$2b$")) {
            encodedPass = passwordEncoder.encode(encodedPass);
            user.setPassword(encodedPass);
        }

        Mutation mutation = Mutation.newInsertOrUpdateBuilder("Users")
                .set("user_id").to(user.getId())
                .set("email").to(user.getEmail())
                .set("password_hash").to(user.getPassword() != null ? user.getPassword() : "")
                .set("name").to(user.getName() != null ? user.getName() : "")
                .set("phone").to(user.getPhone() != null ? user.getPhone() : "")
                .set("avatar_url").to(user.getAvatar() != null ? user.getAvatar() : "")
                .set("role").to(user.getRole() != null ? user.getRole().name() : Role.CUSTOMER.name())
                .set("created_at").to(Value.COMMIT_TIMESTAMP)
                .build();

        databaseClient.write(Collections.singletonList(mutation));
        return user;
    }

    private User mapUser(Struct s) {
        return User.builder()
                .id(s.getString("user_id"))
                .email(s.getString("email"))
                .password(s.getString("password_hash"))
                .name(s.getString("name"))
                .phone(s.getString("phone"))
                .avatar(s.getString("avatar_url"))
                .role(Role.valueOf(s.getString("role")))
                .createdAt(s.isNull("created_at") ? "" : s.getTimestamp("created_at").toString())
                .build();
    }

    public List<Address> getUserAddresses(String userId) {
        Statement stmt = Statement.newBuilder("SELECT user_id, address_id, full_name, street, city, state, zip_code, country, phone, is_default, address_type FROM UserAddresses WHERE user_id = @userId")
                .bind("userId").to(userId).build();

        List<Address> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                list.add(mapAddress(rs.getCurrentRowAsStruct()));
            }
        }
        return list;
    }

    public List<Address> saveUserAddress(String userId, Address address) {
        if (address.getId() == null || address.getId().isEmpty()) {
            address.setId("addr-" + System.currentTimeMillis());
        }
        address.setUserId(userId);

        List<Mutation> mutations = new ArrayList<>();
        if (address.isDefault()) {
            for (Address existing : getUserAddresses(userId)) {
                if (!existing.getId().equals(address.getId()) && existing.isDefault()) {
                    mutations.add(Mutation.newUpdateBuilder("UserAddresses")
                            .set("user_id").to(userId)
                            .set("address_id").to(existing.getId())
                            .set("is_default").to(false)
                            .build());
                }
            }
        }

        mutations.add(Mutation.newInsertOrUpdateBuilder("UserAddresses")
                .set("user_id").to(userId)
                .set("address_id").to(address.getId())
                .set("full_name").to(address.getFullName())
                .set("street").to(address.getStreet())
                .set("city").to(address.getCity())
                .set("state").to(address.getState())
                .set("zip_code").to(address.getZipCode())
                .set("country").to(address.getCountry())
                .set("phone").to(address.getPhone())
                .set("is_default").to(address.isDefault())
                .set("address_type").to(address.getType() != null ? address.getType() : "Home")
                .build());

        databaseClient.write(mutations);
        return getUserAddresses(userId);
    }

    public List<Address> updateAddress(String userId, String addressId, Address addressUpdate) {
        addressUpdate.setId(addressId);
        return saveUserAddress(userId, addressUpdate);
    }

    public List<Address> deleteAddress(String userId, String addressId) {
        Mutation m = Mutation.delete("UserAddresses", Key.of(userId, addressId));
        databaseClient.write(Collections.singletonList(m));
        return getUserAddresses(userId);
    }

    private Address mapAddress(Struct s) {
        return Address.builder()
                .id(s.getString("address_id"))
                .userId(s.getString("user_id"))
                .fullName(s.getString("full_name"))
                .street(s.getString("street"))
                .city(s.getString("city"))
                .state(s.getString("state"))
                .zipCode(s.getString("zip_code"))
                .country(s.getString("country"))
                .phone(s.getString("phone"))
                .isDefault(s.getBoolean("is_default"))
                .type(s.getString("address_type"))
                .build();
    }

    // =========================================================================
    // PRODUCT, COLOR, IMAGE & SIZE CLOUD SPANNER OPERATIONS
    // =========================================================================

    public List<Product> getAllProducts() {
        Statement stmt = Statement.of("SELECT product_id, sku, name, brand, category, subcategory, description, price, original_price, discount_percentage, rating, review_count, stock_quantity, in_stock, hover_image_url, is_new, is_trending, is_bestseller, is_flash_sale, flash_sale_end_time, specifications_json, size_and_fit, shipping_info, return_policy, created_at FROM Products");

        List<Product> products = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                products.add(mapProduct(rs.getCurrentRowAsStruct()));
            }
        }

        if (!products.isEmpty()) {
            loadProductChildrenBatch(products);
        }
        return products;
    }

    public Optional<Product> getProductById(String id) {
        Statement stmt = Statement.newBuilder("SELECT product_id, sku, name, brand, category, subcategory, description, price, original_price, discount_percentage, rating, review_count, stock_quantity, in_stock, hover_image_url, is_new, is_trending, is_bestseller, is_flash_sale, flash_sale_end_time, specifications_json, size_and_fit, shipping_info, return_policy, created_at FROM Products WHERE product_id = @id")
                .bind("id").to(id).build();

        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            if (rs.next()) {
                Product p = mapProduct(rs.getCurrentRowAsStruct());
                loadProductChildren(p);
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    private Product mapProduct(Struct s) {
        Map<String, String> specsMap = null;
        if (!s.isNull("specifications_json")) {
            try {
                specsMap = objectMapper.readValue(s.getString("specifications_json"), new TypeReference<Map<String, String>>() {});
            } catch (Exception ignored) {}
        }

        String cat = s.getString("category");
        String br = s.getString("brand");
        String sub = s.getString("subcategory");
        List<String> tagsList = Arrays.asList(cat.toLowerCase(), br.toLowerCase(), sub.toLowerCase(), "luxury");

        return Product.builder()
                .id(s.getString("product_id"))
                .sku(s.getString("sku"))
                .name(s.getString("name"))
                .brand(br)
                .category(cat)
                .subcategory(sub)
                .description(s.getString("description"))
                .price(s.getBigDecimal("price").doubleValue())
                .originalPrice(s.getBigDecimal("original_price").doubleValue())
                .discountPercentage((int) s.getLong("discount_percentage"))
                .rating(s.getDouble("rating"))
                .reviewCount((int) s.getLong("review_count"))
                .stockQuantity((int) s.getLong("stock_quantity"))
                .inStock(s.getBoolean("in_stock"))
                .hoverImage(s.isNull("hover_image_url") ? null : s.getString("hover_image_url"))
                .isNew(s.getBoolean("is_new"))
                .isTrending(s.getBoolean("is_trending"))
                .isBestSeller(s.getBoolean("is_bestseller"))
                .isFlashSale(s.getBoolean("is_flash_sale"))
                .flashSaleEndTime(s.isNull("flash_sale_end_time") ? "2026-12-31T23:59:59Z" : s.getTimestamp("flash_sale_end_time").toString())
                .specifications(specsMap)
                .sizeAndFit(s.isNull("size_and_fit") ? "" : s.getString("size_and_fit"))
                .shippingInfo(s.isNull("shipping_info") ? "" : s.getString("shipping_info"))
                .returnPolicy(s.isNull("return_policy") ? "" : s.getString("return_policy"))
                .tags(tagsList)
                .build();
    }

    private void loadProductChildren(Product p) {
        // Load Color Options
        Statement colorStmt = Statement.newBuilder("SELECT color_name, color_hex, image_url FROM ProductColorOptions WHERE product_id = @id")
                .bind("id").to(p.getId()).build();
        List<ColorOption> colors = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(colorStmt)) {
            while (rs.next()) {
                colors.add(new ColorOption(rs.getString("color_name"), rs.getString("color_hex"), rs.isNull("image_url") ? null : rs.getString("image_url")));
            }
        }
        p.setColorOptions(colors);

        // Load Images
        Statement imgStmt = Statement.newBuilder("SELECT image_url FROM ProductImages WHERE product_id = @id ORDER BY image_order")
                .bind("id").to(p.getId()).build();
        List<String> images = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(imgStmt)) {
            while (rs.next()) {
                images.add(rs.getString("image_url"));
            }
        }
        if (images.isEmpty()) {
            if (p.getHoverImage() != null && !p.getHoverImage().isEmpty()) {
                images.add(p.getHoverImage());
            } else {
                images.add("https://images.unsplash.com/photo-1584273143981-41c073dfe8f8?auto=format&fit=crop&w=1000&q=80");
            }
        }
        p.setImages(images);

        // Load Sizes
        Statement sizeStmt = Statement.newBuilder("SELECT size FROM ProductSizes WHERE product_id = @id")
                .bind("id").to(p.getId()).build();
        List<String> sizes = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(sizeStmt)) {
            while (rs.next()) {
                sizes.add(rs.getString("size"));
            }
        }
        p.setSizes(sizes);
    }

    private void loadProductChildrenBatch(List<Product> products) {
        // 1. Batch load Color Options in 1 query
        Statement colorStmt = Statement.of("SELECT product_id, color_name, color_hex, image_url FROM ProductColorOptions");
        Map<String, List<ColorOption>> colorMap = new HashMap<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(colorStmt)) {
            while (rs.next()) {
                String pid = rs.getString("product_id");
                ColorOption co = new ColorOption(rs.getString("color_name"), rs.getString("color_hex"), rs.isNull("image_url") ? null : rs.getString("image_url"));
                colorMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(co);
            }
        }

        // 2. Batch load Images in 1 query
        Statement imgStmt = Statement.of("SELECT product_id, image_url FROM ProductImages ORDER BY product_id, image_order");
        Map<String, List<String>> imgMap = new HashMap<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(imgStmt)) {
            while (rs.next()) {
                String pid = rs.getString("product_id");
                imgMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(rs.getString("image_url"));
            }
        }

        // 3. Batch load Sizes in 1 query
        Statement sizeStmt = Statement.of("SELECT product_id, size FROM ProductSizes");
        Map<String, List<String>> sizeMap = new HashMap<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(sizeStmt)) {
            while (rs.next()) {
                String pid = rs.getString("product_id");
                sizeMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(rs.getString("size"));
            }
        }

        // Assign to products in memory O(1)
        for (Product p : products) {
            List<ColorOption> colors = colorMap.getOrDefault(p.getId(), Collections.emptyList());
            p.setColorOptions(colors);

            List<String> images = imgMap.getOrDefault(p.getId(), Collections.emptyList());
            if (images.isEmpty()) {
                if (p.getHoverImage() != null && !p.getHoverImage().isEmpty()) {
                    images = Collections.singletonList(p.getHoverImage());
                } else {
                    images = Collections.singletonList("https://images.unsplash.com/photo-1584273143981-41c073dfe8f8?auto=format&fit=crop&w=1000&q=80");
                }
            }
            p.setImages(images);

            List<String> sizes = sizeMap.getOrDefault(p.getId(), Collections.emptyList());
            p.setSizes(sizes);
        }
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId("prod-" + System.currentTimeMillis());
        }

        String specsJson = null;
        if (product.getSpecifications() != null) {
            try {
                specsJson = objectMapper.writeValueAsString(product.getSpecifications());
            } catch (JsonProcessingException ignored) {}
        }

        List<Mutation> mutations = new ArrayList<>();
        mutations.add(Mutation.newInsertOrUpdateBuilder("Products")
                .set("product_id").to(product.getId())
                .set("sku").to(product.getSku() != null ? product.getSku() : "SKU-" + product.getId())
                .set("name").to(product.getName())
                .set("brand").to(product.getBrand())
                .set("category").to(product.getCategory())
                .set("subcategory").to(product.getSubcategory() != null ? product.getSubcategory() : "General")
                .set("description").to(product.getDescription())
                .set("price").to(BigDecimal.valueOf(product.getPrice()))
                .set("original_price").to(BigDecimal.valueOf(product.getOriginalPrice() > 0 ? product.getOriginalPrice() : product.getPrice()))
                .set("discount_percentage").to(product.getDiscountPercentage())
                .set("rating").to(product.getRating())
                .set("review_count").to(product.getReviewCount())
                .set("stock_quantity").to(product.getStockQuantity())
                .set("in_stock").to(product.isInStock())
                .set("hover_image_url").to(product.getHoverImage() != null ? product.getHoverImage() : "")
                .set("is_new").to(product.isNew())
                .set("is_trending").to(product.isTrending())
                .set("is_bestseller").to(product.isBestSeller())
                .set("is_flash_sale").to(product.isFlashSale())
                .set("specifications_json").to(specsJson != null ? specsJson : "{}")
                .set("size_and_fit").to(product.getSizeAndFit() != null ? product.getSizeAndFit() : "")
                .set("shipping_info").to(product.getShippingInfo() != null ? product.getShippingInfo() : "")
                .set("return_policy").to(product.getReturnPolicy() != null ? product.getReturnPolicy() : "")
                .set("created_at").to(Value.COMMIT_TIMESTAMP)
                .build());

        // Update Colors
        mutations.add(Mutation.delete("ProductColorOptions", prefixKeySet(Key.of(product.getId()))));
        if (product.getColorOptions() != null) {
            for (ColorOption co : product.getColorOptions()) {
                mutations.add(Mutation.newInsertOrUpdateBuilder("ProductColorOptions")
                        .set("product_id").to(product.getId())
                        .set("color_name").to(co.getName())
                        .set("color_hex").to(co.getCode() != null ? co.getCode() : "#000000")
                        .set("image_url").to(co.getImage() != null ? co.getImage() : "")
                        .build());
            }
        }

        // Update Images
        mutations.add(Mutation.delete("ProductImages", prefixKeySet(Key.of(product.getId()))));
        if (product.getImages() != null) {
            long order = 0;
            for (String img : product.getImages()) {
                mutations.add(Mutation.newInsertOrUpdateBuilder("ProductImages")
                        .set("product_id").to(product.getId())
                        .set("image_order").to(order++)
                        .set("image_url").to(img)
                        .build());
            }
        }

        // Update Sizes
        mutations.add(Mutation.delete("ProductSizes", prefixKeySet(Key.of(product.getId()))));
        if (product.getSizes() != null) {
            for (String sz : product.getSizes()) {
                mutations.add(Mutation.newInsertOrUpdateBuilder("ProductSizes")
                        .set("product_id").to(product.getId())
                        .set("size").to(sz)
                        .build());
            }
        }

        // Update Inventory record
        mutations.add(Mutation.newInsertOrUpdateBuilder("Inventory")
                .set("product_id").to(product.getId())
                .set("inventory_id").to("inv-" + product.getId())
                .set("product_name").to(product.getName())
                .set("sku").to(product.getSku() != null ? product.getSku() : "SKU-" + product.getId())
                .set("category").to(product.getCategory())
                .set("available_stock").to(product.getStockQuantity())
                .set("reserved_stock").to(0)
                .set("total_sold").to(0)
                .set("status").to(product.getStockQuantity() > 5 ? "IN_STOCK" : product.getStockQuantity() > 0 ? "LOW_STOCK" : "OUT_OF_STOCK")
                .build());

        databaseClient.write(mutations);
        return product;
    }

    public boolean deleteProduct(String id) {
        List<Mutation> mutations = new ArrayList<>();
        mutations.add(Mutation.delete("Products", Key.of(id)));
        mutations.add(Mutation.delete("Inventory", Key.of(id)));
        databaseClient.write(mutations);
        return true;
    }

    // =========================================================================
    // CATEGORIES & BRANDS CLOUD SPANNER OPERATIONS
    // =========================================================================

    public List<Category> getAllCategories() {
        Statement stmt = Statement.of("SELECT category_id, name, slug, image_url, description, item_count FROM Categories");
        List<Category> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                list.add(Category.builder()
                        .id(rs.getString("category_id"))
                        .name(rs.getString("name"))
                        .slug(rs.getString("slug"))
                        .image(rs.getString("image_url"))
                        .description(rs.getString("description"))
                        .itemCount((int) rs.getLong("item_count"))
                        .build());
            }
        }
        return list;
    }

    public List<Brand> getAllBrands() {
        Statement stmt = Statement.of("SELECT brand_id, name, logo_url, category FROM Brands");
        List<Brand> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                list.add(Brand.builder()
                        .id(rs.getString("brand_id"))
                        .name(rs.getString("name"))
                        .logo(rs.getString("logo_url"))
                        .category(rs.getString("category"))
                        .build());
            }
        }
        return list;
    }

    // =========================================================================
    // CARTS CLOUD SPANNER OPERATIONS
    // =========================================================================

    public Cart getUserCart(String userId) {
        Statement cartStmt = Statement.newBuilder("SELECT user_id, coupon_code, updated_at FROM Carts WHERE user_id = @userId")
                .bind("userId").to(userId).build();

        String couponCode = null;
        try (ResultSet rs = databaseClient.singleUse().executeQuery(cartStmt)) {
            if (rs.next()) {
                couponCode = rs.isNull("coupon_code") ? null : rs.getString("coupon_code");
            }
        }

        Statement itemsStmt = Statement.newBuilder("SELECT item_id, product_id, color, size, quantity, unit_price FROM CartItems WHERE user_id = @userId")
                .bind("userId").to(userId).build();

        List<CartItem> items = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(itemsStmt)) {
            while (rs.next()) {
                String prodId = rs.getString("product_id");
                Optional<Product> prod = getProductById(prodId);
                if (prod.isPresent()) {
                    int qty = (int) rs.getLong("quantity");
                    double unitPrice = rs.getBigDecimal("unit_price").doubleValue();
                    items.add(CartItem.builder()
                            .id(rs.getString("item_id"))
                            .product(prod.get())
                            .color(rs.getString("color"))
                            .size(rs.getString("size"))
                            .quantity(qty)
                            .price(unitPrice)
                            .subtotal(qty * unitPrice)
                            .build());
                }
            }
        }

        Cart cart = Cart.builder()
                .id("cart-" + userId)
                .items(items)
                .couponCode(couponCode)
                .build();

        if (couponCode != null) {
            findCouponByCode(couponCode).ifPresent(cart::setAppliedCoupon);
        }
        return recalculateCart(cart);
    }

    public Cart saveCart(String userId, Cart cart) {
        Cart recalculated = recalculateCart(cart);

        List<Mutation> mutations = new ArrayList<>();
        mutations.add(Mutation.newInsertOrUpdateBuilder("Carts")
                .set("user_id").to(userId)
                .set("coupon_code").to(recalculated.getCouponCode() != null ? recalculated.getCouponCode() : "")
                .set("updated_at").to(Value.COMMIT_TIMESTAMP)
                .build());

        mutations.add(Mutation.delete("CartItems", prefixKeySet(Key.of(userId))));

        if (recalculated.getItems() != null) {
            for (CartItem item : recalculated.getItems()) {
                if (item.getId() == null) item.setId("item-" + System.currentTimeMillis());
                mutations.add(Mutation.newInsertOrUpdateBuilder("CartItems")
                        .set("user_id").to(userId)
                        .set("item_id").to(item.getId())
                        .set("product_id").to(item.getProduct().getId())
                        .set("color").to(item.getColor() != null ? item.getColor() : "Standard")
                        .set("size").to(item.getSize() != null ? item.getSize() : "Standard")
                        .set("quantity").to(item.getQuantity())
                        .set("unit_price").to(BigDecimal.valueOf(item.getPrice()))
                        .build());
            }
        }

        databaseClient.write(mutations);
        return recalculated;
    }

    public Cart recalculateCart(Cart cart) {
        if (cart.getItems() == null) cart.setItems(new ArrayList<>());
        double subtotal = cart.getItems().stream().mapToDouble(CartItem::getSubtotal).sum();
        double discount = 0;

        if (cart.getAppliedCoupon() != null) {
            discount = Math.min((subtotal * cart.getAppliedCoupon().getDiscountPercentage()) / 100.0, cart.getAppliedCoupon().getMaxDiscount());
        }

        double shipping = subtotal > 999 || subtotal == 0 ? 0 : 150;
        double tax = Math.round((subtotal - discount) * 0.05);
        double total = Math.max(0, subtotal - discount + shipping + tax);

        cart.setSubtotal(subtotal);
        cart.setDiscount(discount);
        cart.setShipping(shipping);
        cart.setTax(tax);
        cart.setTotal(total);
        return cart;
    }

    // =========================================================================
    // WISHLIST CLOUD SPANNER OPERATIONS
    // =========================================================================

    public List<WishlistItem> getUserWishlist(String userId) {
        Statement stmt = Statement.newBuilder("SELECT user_id, product_id, added_at FROM WishlistItems WHERE user_id = @userId")
                .bind("userId").to(userId).build();

        List<WishlistItem> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                String prodId = rs.getString("product_id");
                getProductById(prodId).ifPresent(p -> list.add(WishlistItem.builder()
                        .id("w-" + prodId)
                        .product(p)
                        .addedAt(rs.isNull("added_at") ? "" : rs.getTimestamp("added_at").toString())
                        .build()));
            }
        }
        return list;
    }

    public List<WishlistItem> addToWishlist(String userId, Product product) {
        Mutation m = Mutation.newInsertOrUpdateBuilder("WishlistItems")
                .set("user_id").to(userId)
                .set("product_id").to(product.getId())
                .set("added_at").to(Value.COMMIT_TIMESTAMP)
                .build();
        databaseClient.write(Collections.singletonList(m));
        return getUserWishlist(userId);
    }

    public List<WishlistItem> removeFromWishlist(String userId, String productId) {
        Mutation m = Mutation.delete("WishlistItems", Key.of(userId, productId));
        databaseClient.write(Collections.singletonList(m));
        return getUserWishlist(userId);
    }

    // =========================================================================
    // ORDERS & CHECKOUT TRANSACTIONAL CLOUD SPANNER OPERATIONS
    // =========================================================================

    public Order saveOrder(Order order) {
        if (order.getId() == null || order.getId().isEmpty()) {
            order.setId("ord-" + System.currentTimeMillis());
        }
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber("LOG-" + (100000 + new Random().nextInt(900000)));
        }
        if (order.getCreatedAt() == null || order.getCreatedAt().isEmpty()) {
            order.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
        }

        String addressJson = "";
        if (order.getShippingAddress() != null) {
            try {
                addressJson = objectMapper.writeValueAsString(order.getShippingAddress());
            } catch (JsonProcessingException ignored) {}
        }

        final String finalAddrJson = addressJson;
        final Order finalOrder = order;

        // Execute in Cloud Spanner Read-Write Transaction
        databaseClient.readWriteTransaction().run(transaction -> {
            // 1. Insert/Update Order Header
            transaction.buffer(Mutation.newInsertOrUpdateBuilder("Orders")
                    .set("order_id").to(finalOrder.getId())
                    .set("order_number").to(finalOrder.getOrderNumber())
                    .set("user_id").to(finalOrder.getUserId())
                    .set("shipping_address_json").to(finalAddrJson)
                    .set("payment_method").to(finalOrder.getPaymentMethod() != null ? finalOrder.getPaymentMethod().name() : "CARD")
                    .set("payment_status").to(finalOrder.getPaymentStatus() != null ? finalOrder.getPaymentStatus().name() : "COMPLETED")
                    .set("order_status").to(finalOrder.getOrderStatus() != null ? finalOrder.getOrderStatus().name() : "ORDER_PLACED")
                    .set("subtotal").to(BigDecimal.valueOf(finalOrder.getSubtotal()))
                    .set("discount").to(BigDecimal.valueOf(finalOrder.getDiscount()))
                    .set("shipping_fee").to(BigDecimal.valueOf(finalOrder.getShippingFee()))
                    .set("tax").to(BigDecimal.valueOf(finalOrder.getTax()))
                    .set("total_amount").to(BigDecimal.valueOf(finalOrder.getTotalAmount()))
                    .set("created_at").to(Value.COMMIT_TIMESTAMP)
                    .set("estimated_delivery").to(finalOrder.getEstimatedDelivery() != null ? finalOrder.getEstimatedDelivery() : "3-5 Business Days")
                    .set("tracking_number").to(finalOrder.getTrackingNumber() != null ? finalOrder.getTrackingNumber() : "LOG-TRACK-" + finalOrder.getOrderNumber())
                    .build());

            // 2. Insert Order Items
            if (finalOrder.getItems() != null) {
                long orderItemIdx = 1;
                for (OrderItem item : finalOrder.getItems()) {
                    if (item.getId() == null) item.setId("oi-" + (orderItemIdx++));
                    transaction.buffer(Mutation.newInsertOrUpdateBuilder("OrderItems")
                            .set("order_id").to(finalOrder.getId())
                            .set("item_id").to(item.getId())
                            .set("product_id").to(item.getProductId() != null ? item.getProductId() : "prod-unknown")
                            .set("product_name").to(item.getProductName() != null ? item.getProductName() : "")
                            .set("product_image").to(item.getProductImage() != null ? item.getProductImage() : "")
                            .set("brand").to(item.getBrand() != null ? item.getBrand() : "")
                            .set("color").to(item.getColor() != null ? item.getColor() : "")
                            .set("size").to(item.getSize() != null ? item.getSize() : "")
                            .set("quantity").to(item.getQuantity())
                            .set("unit_price").to(BigDecimal.valueOf(item.getUnitPrice()))
                            .set("total_price").to(BigDecimal.valueOf(item.getTotalPrice()))
                            .build());

                    // Deduct stock quantity in Inventory & Products
                    if (item.getProductId() != null) {
                        Statement stockStmt = Statement.newBuilder("SELECT stock_quantity FROM Products WHERE product_id = @pid")
                                .bind("pid").to(item.getProductId()).build();
                        try (ResultSet rs = transaction.executeQuery(stockStmt)) {
                            if (rs.next()) {
                                long currentStock = rs.getLong("stock_quantity");
                                long newStock = Math.max(0, currentStock - item.getQuantity());
                                transaction.buffer(Mutation.newUpdateBuilder("Products")
                                        .set("product_id").to(item.getProductId())
                                        .set("stock_quantity").to(newStock)
                                        .set("in_stock").to(newStock > 0)
                                        .build());
                                transaction.buffer(Mutation.newUpdateBuilder("Inventory")
                                        .set("product_id").to(item.getProductId())
                                        .set("available_stock").to(newStock)
                                        .set("status").to(newStock > 5 ? "IN_STOCK" : newStock > 0 ? "LOW_STOCK" : "OUT_OF_STOCK")
                                        .build());
                            }
                        }
                    }
                }
            }

            // 3. Insert Order Timeline
            if (finalOrder.getTimeline() != null) {
                for (OrderStatusTimeline tl : finalOrder.getTimeline()) {
                    transaction.buffer(Mutation.newInsertOrUpdateBuilder("OrderTimeline")
                            .set("order_id").to(finalOrder.getId())
                            .set("status").to(tl.getStatus().name())
                            .set("timestamp_str").to(tl.getTimestamp() != null ? tl.getTimestamp() : "")
                            .set("completed").to(tl.isCompleted())
                            .set("description").to(tl.getDescription() != null ? tl.getDescription() : "")
                            .build());
                }
            }

            // 4. Clear active Cart items for user
            transaction.buffer(Mutation.delete("CartItems", prefixKeySet(Key.of(finalOrder.getUserId()))));
            return null;
        });

        return order;
    }

    public Order cancelOrderTransactional(String orderId, String reason) {
        databaseClient.readWriteTransaction().run(transaction -> {
            Statement orderStmt = Statement.newBuilder("SELECT user_id, order_status FROM Orders WHERE order_id = @id")
                    .bind("id").to(orderId).build();
            String currentStatusStr = null;
            try (ResultSet rs = transaction.executeQuery(orderStmt)) {
                if (rs.next()) {
                    currentStatusStr = rs.getString("order_status");
                }
            }

            if (currentStatusStr == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Order not found: " + orderId);
            }

            OrderStatus currentStatus = OrderStatus.valueOf(currentStatusStr);
            if (currentStatus == OrderStatus.SHIPPED || currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Order cannot be cancelled in status: " + currentStatus);
            }

            // 1. Update Order Status
            transaction.buffer(Mutation.newUpdateBuilder("Orders")
                    .set("order_id").to(orderId)
                    .set("order_status").to(OrderStatus.CANCELLED.name())
                    .build());

            // 2. Insert Cancelled Timeline
            transaction.buffer(Mutation.newInsertOrUpdateBuilder("OrderTimeline")
                    .set("order_id").to(orderId)
                    .set("status").to(OrderStatus.CANCELLED.name())
                    .set("timestamp_str").to(new Date().toString())
                    .set("completed").to(true)
                    .set("description").to("Order cancelled by customer. Reason: " + (reason != null ? reason : "Customer request"))
                    .build());

            // 3. Restore Stock for Order Items
            Statement itemsStmt = Statement.newBuilder("SELECT product_id, quantity FROM OrderItems WHERE order_id = @id")
                    .bind("id").to(orderId).build();
            try (ResultSet rs = transaction.executeQuery(itemsStmt)) {
                while (rs.next()) {
                    String pid = rs.getString("product_id");
                    long qty = rs.getLong("quantity");
                    Statement pStmt = Statement.newBuilder("SELECT stock_quantity FROM Products WHERE product_id = @pid")
                            .bind("pid").to(pid).build();
                    try (ResultSet prs = transaction.executeQuery(pStmt)) {
                        if (prs.next()) {
                            long currStock = prs.getLong("stock_quantity");
                            long restoredStock = currStock + qty;
                            transaction.buffer(Mutation.newUpdateBuilder("Products")
                                    .set("product_id").to(pid)
                                    .set("stock_quantity").to(restoredStock)
                                    .set("in_stock").to(true)
                                    .build());
                            transaction.buffer(Mutation.newUpdateBuilder("Inventory")
                                    .set("product_id").to(pid)
                                    .set("available_stock").to(restoredStock)
                                    .set("status").to(restoredStock > 5 ? "IN_STOCK" : "LOW_STOCK")
                                    .build());
                        }
                    }
                }
            }
            return null;
        });

        return getOrderById(orderId).orElse(null);
    }

    public List<Order> getUserOrders(String userId) {
        Statement stmt = Statement.newBuilder("SELECT order_id FROM Orders WHERE user_id = @userId")
                .bind("userId").to(userId).build();

        List<String> orderIds = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                orderIds.add(rs.getString("order_id"));
            }
        }

        List<Order> orders = new ArrayList<>();
        for (String id : orderIds) {
            getOrderById(id).ifPresent(orders::add);
        }
        return orders;
    }

    public List<Order> getAllOrders() {
        Statement stmt = Statement.of("SELECT order_id FROM Orders");
        List<String> orderIds = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                orderIds.add(rs.getString("order_id"));
            }
        }

        List<Order> orders = new ArrayList<>();
        for (String id : orderIds) {
            getOrderById(id).ifPresent(orders::add);
        }
        return orders;
    }

    public Optional<Order> getOrderById(String id) {
        Statement stmt = Statement.newBuilder("SELECT order_id, order_number, user_id, shipping_address_json, payment_method, payment_status, order_status, subtotal, discount, shipping_fee, tax, total_amount, created_at, estimated_delivery, tracking_number FROM Orders WHERE order_id = @id")
                .bind("id").to(id).build();

        Order order = null;
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            if (rs.next()) {
                Address addr = null;
                if (!rs.isNull("shipping_address_json")) {
                    try {
                        addr = objectMapper.readValue(rs.getString("shipping_address_json"), Address.class);
                    } catch (Exception ignored) {}
                }

                order = Order.builder()
                        .id(rs.getString("order_id"))
                        .orderNumber(rs.getString("order_number"))
                        .userId(rs.getString("user_id"))
                        .shippingAddress(addr)
                        .paymentMethod(PaymentMethodType.valueOf(rs.getString("payment_method")))
                        .paymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")))
                        .orderStatus(OrderStatus.valueOf(rs.getString("order_status")))
                        .subtotal(rs.getBigDecimal("subtotal").doubleValue())
                        .discount(rs.getBigDecimal("discount").doubleValue())
                        .shippingFee(rs.getBigDecimal("shipping_fee").doubleValue())
                        .tax(rs.getBigDecimal("tax").doubleValue())
                        .totalAmount(rs.getBigDecimal("total_amount").doubleValue())
                        .createdAt(rs.isNull("created_at") ? "" : rs.getTimestamp("created_at").toString())
                        .estimatedDelivery(rs.isNull("estimated_delivery") ? "" : rs.getString("estimated_delivery"))
                        .trackingNumber(rs.isNull("tracking_number") ? "" : rs.getString("tracking_number"))
                        .canCancel(true)
                        .canReturn(false)
                        .build();
            }
        }

        if (order == null) return Optional.empty();

        // Load Order Items
        Statement itemsStmt = Statement.newBuilder("SELECT item_id, product_id, product_name, product_image, brand, color, size, quantity, unit_price, total_price FROM OrderItems WHERE order_id = @id")
                .bind("id").to(id).build();
        List<OrderItem> items = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(itemsStmt)) {
            while (rs.next()) {
                items.add(OrderItem.builder()
                        .id(rs.getString("item_id"))
                        .productId(rs.getString("product_id"))
                        .productName(rs.getString("product_name"))
                        .productImage(rs.getString("product_image"))
                        .brand(rs.getString("brand"))
                        .color(rs.getString("color"))
                        .size(rs.getString("size"))
                        .quantity((int) rs.getLong("quantity"))
                        .unitPrice(rs.getBigDecimal("unit_price").doubleValue())
                        .totalPrice(rs.getBigDecimal("total_price").doubleValue())
                        .build());
            }
        }
        order.setItems(items);

        // Load Order Timeline
        Statement tlStmt = Statement.newBuilder("SELECT status, timestamp_str, completed, description FROM OrderTimeline WHERE order_id = @id")
                .bind("id").to(id).build();
        List<OrderStatusTimeline> timeline = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(tlStmt)) {
            while (rs.next()) {
                timeline.add(OrderStatusTimeline.builder()
                        .status(OrderStatus.valueOf(rs.getString("status")))
                        .timestamp(rs.getString("timestamp_str"))
                        .completed(rs.getBoolean("completed"))
                        .description(rs.getString("description"))
                        .build());
            }
        }
        order.setTimeline(timeline);

        return Optional.of(order);
    }

    // =========================================================================
    // REVIEWS CLOUD SPANNER OPERATIONS
    // =========================================================================

    public List<Review> getProductReviews(String productId) {
        Statement stmt = Statement.newBuilder("SELECT review_id, product_id, user_id, user_name, user_avatar, rating, title, comment, created_at, verified_purchase, helpful_count FROM Reviews WHERE product_id = @productId")
                .bind("productId").to(productId).build();

        List<Review> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                list.add(Review.builder()
                        .id(rs.getString("review_id"))
                        .productId(rs.getString("product_id"))
                        .userId(rs.getString("user_id"))
                        .userName(rs.getString("user_name"))
                        .userAvatar(rs.isNull("user_avatar") ? null : rs.getString("user_avatar"))
                        .rating((int) rs.getLong("rating"))
                        .title(rs.getString("title"))
                        .comment(rs.getString("comment"))
                        .date(rs.isNull("created_at") ? "Recently" : rs.getTimestamp("created_at").toString())
                        .verifiedPurchase(rs.getBoolean("verified_purchase"))
                        .helpfulCount((int) rs.getLong("helpful_count"))
                        .build());
            }
        }
        return list;
    }

    public Review saveReview(Review review) {
        if (review.getId() == null || review.getId().isEmpty()) {
            review.setId("rev-" + System.currentTimeMillis());
        }

        Mutation m = Mutation.newInsertOrUpdateBuilder("Reviews")
                .set("product_id").to(review.getProductId())
                .set("review_id").to(review.getId())
                .set("user_id").to(review.getUserId())
                .set("user_name").to(review.getUserName())
                .set("user_avatar").to(review.getUserAvatar() != null ? review.getUserAvatar() : "")
                .set("rating").to(review.getRating())
                .set("title").to(review.getTitle())
                .set("comment").to(review.getComment())
                .set("created_at").to(Value.COMMIT_TIMESTAMP)
                .set("verified_purchase").to(review.isVerifiedPurchase())
                .set("helpful_count").to(review.getHelpfulCount())
                .build();

        databaseClient.write(Collections.singletonList(m));
        return review;
    }

    // =========================================================================
    // COUPONS CLOUD SPANNER OPERATIONS
    // =========================================================================

    public Optional<Coupon> findCouponByCode(String code) {
        Statement stmt = Statement.newBuilder("SELECT coupon_id, code, discount_percentage, max_discount, min_order_amount, expiry_date, usage_limit, usage_count, is_active FROM Coupons WHERE UPPER(code) = @code")
                .bind("code").to(code.toUpperCase().trim()).build();

        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            if (rs.next()) {
                return Optional.of(mapCoupon(rs.getCurrentRowAsStruct()));
            }
        }
        return Optional.empty();
    }

    public List<Coupon> getAllCoupons() {
        Statement stmt = Statement.of("SELECT coupon_id, code, discount_percentage, max_discount, min_order_amount, expiry_date, usage_limit, usage_count, is_active FROM Coupons");

        List<Coupon> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                list.add(mapCoupon(rs.getCurrentRowAsStruct()));
            }
        }
        return list;
    }

    public Coupon saveCoupon(Coupon coupon) {
        if (coupon.getId() == null || coupon.getId().isEmpty()) {
            coupon.setId("coup-" + System.currentTimeMillis());
        }

        Mutation m = Mutation.newInsertOrUpdateBuilder("Coupons")
                .set("coupon_id").to(coupon.getId())
                .set("code").to(coupon.getCode().toUpperCase().trim())
                .set("discount_percentage").to(coupon.getDiscountPercentage())
                .set("max_discount").to(BigDecimal.valueOf(coupon.getMaxDiscount()))
                .set("min_order_amount").to(BigDecimal.valueOf(coupon.getMinOrderAmount()))
                .set("expiry_date").to(coupon.getExpiryDate())
                .set("usage_limit").to(coupon.getUsageLimit())
                .set("usage_count").to(coupon.getUsageCount())
                .set("is_active").to(coupon.isActive())
                .build();

        databaseClient.write(Collections.singletonList(m));
        return coupon;
    }

    private Coupon mapCoupon(Struct s) {
        return Coupon.builder()
                .id(s.getString("coupon_id"))
                .code(s.getString("code"))
                .discountPercentage((int) s.getLong("discount_percentage"))
                .maxDiscount(s.getBigDecimal("max_discount").doubleValue())
                .minOrderAmount(s.getBigDecimal("min_order_amount").doubleValue())
                .expiryDate(s.getString("expiry_date"))
                .usageLimit((int) s.getLong("usage_limit"))
                .usageCount((int) s.getLong("usage_count"))
                .isActive(s.getBoolean("is_active"))
                .build();
    }

    // =========================================================================
    // INVENTORY CLOUD SPANNER OPERATIONS
    // =========================================================================

    public List<Inventory> getAllInventory() {
        Statement stmt = Statement.of("SELECT product_id, inventory_id, product_name, sku, category, available_stock, reserved_stock, total_sold, status FROM Inventory");

        List<Inventory> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                list.add(Inventory.builder()
                        .id(rs.getString("inventory_id"))
                        .productId(rs.getString("product_id"))
                        .productName(rs.getString("product_name"))
                        .sku(rs.getString("sku"))
                        .category(rs.getString("category"))
                        .availableStock((int) rs.getLong("available_stock"))
                        .reservedStock((int) rs.getLong("reserved_stock"))
                        .totalSold((int) rs.getLong("total_sold"))
                        .status(rs.getString("status"))
                        .build());
            }
        }
        return list;
    }

    public Inventory updateStock(String inventoryId, int availableStock) {
        List<Mutation> mutations = new ArrayList<>();
        String status = availableStock > 5 ? "IN_STOCK" : availableStock > 0 ? "LOW_STOCK" : "OUT_OF_STOCK";

        Statement stmt = Statement.newBuilder("SELECT product_id, inventory_id, product_name, sku, category, available_stock, reserved_stock, total_sold FROM Inventory WHERE inventory_id = @id OR product_id = @id")
                .bind("id").to(inventoryId).build();

        Inventory item = null;
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            if (rs.next()) {
                item = Inventory.builder()
                        .id(rs.getString("inventory_id"))
                        .productId(rs.getString("product_id"))
                        .productName(rs.getString("product_name"))
                        .sku(rs.getString("sku"))
                        .category(rs.getString("category"))
                        .availableStock(availableStock)
                        .reservedStock((int) rs.getLong("reserved_stock"))
                        .totalSold((int) rs.getLong("total_sold"))
                        .status(status)
                        .build();

                mutations.add(Mutation.newUpdateBuilder("Inventory")
                        .set("product_id").to(item.getProductId())
                        .set("available_stock").to(availableStock)
                        .set("status").to(status)
                        .build());

                mutations.add(Mutation.newUpdateBuilder("Products")
                        .set("product_id").to(item.getProductId())
                        .set("stock_quantity").to(availableStock)
                        .set("in_stock").to(availableStock > 0)
                        .build());
            }
        }

        if (!mutations.isEmpty()) {
            databaseClient.write(mutations);
        }
        return item;
    }

    // =========================================================================
    // NOTIFICATIONS CLOUD SPANNER OPERATIONS
    // =========================================================================

    public List<NotificationItem> getUserNotifications(String userId) {
        Statement stmt = Statement.newBuilder("SELECT notification_id, user_id, title, message, type, created_at, read FROM Notifications WHERE user_id = @userId")
                .bind("userId").to(userId).build();

        List<NotificationItem> list = new ArrayList<>();
        try (ResultSet rs = databaseClient.singleUse().executeQuery(stmt)) {
            while (rs.next()) {
                list.add(NotificationItem.builder()
                        .id(rs.getString("notification_id"))
                        .title(rs.getString("title"))
                        .message(rs.getString("message"))
                        .type(rs.getString("type"))
                        .createdAt(rs.isNull("created_at") ? "" : rs.getTimestamp("created_at").toString())
                        .read(rs.getBoolean("read"))
                        .link("/orders")
                        .build());
            }
        }
        return list;
    }

    public NotificationItem saveNotification(String userId, NotificationItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId("notif-" + System.currentTimeMillis());
        }

        Mutation m = Mutation.newInsertOrUpdateBuilder("Notifications")
                .set("user_id").to(userId)
                .set("notification_id").to(item.getId())
                .set("title").to(item.getTitle())
                .set("message").to(item.getMessage())
                .set("type").to(item.getType() != null ? item.getType() : "SYSTEM")
                .set("created_at").to(Value.COMMIT_TIMESTAMP)
                .set("read").to(item.isRead())
                .build();

        databaseClient.write(Collections.singletonList(m));
        return item;
    }
}
