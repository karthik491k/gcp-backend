package com.logancee.globalcart;

import com.logancee.globalcart.model.*;
import com.logancee.globalcart.service.SpannerDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SpannerPersistenceIntegrationTest {

    @Autowired
    private SpannerDataService spannerDataService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Verify Cloud Spanner User Persistence and Password Hashing")
    public void testUserPersistence() {
        String testEmail = "test_persisted_user_" + System.currentTimeMillis() + "@logancee.fashion";
        User user = User.builder()
                .id("test-u-" + System.currentTimeMillis())
                .email(testEmail)
                .password("TestSecret@123")
                .name("Test Persistence User")
                .phone("+91 91234 56789")
                .role(Role.CUSTOMER)
                .build();

        // Save User to Spanner
        User savedUser = spannerDataService.saveUser(user);
        assertNotNull(savedUser.getId());

        // Fetch User from Spanner by email
        Optional<User> fetchedOpt = spannerDataService.findUserByEmail(testEmail);
        assertTrue(fetchedOpt.isPresent());
        User fetched = fetchedOpt.get();

        assertEquals("Test Persistence User", fetched.getName());
        assertEquals(Role.CUSTOMER, fetched.getRole());
        // Verify password is BCrypt hashed, never stored in plain-text
        assertTrue(passwordEncoder.matches("TestSecret@123", fetched.getPassword()));
    }

    @Test
    @DisplayName("Verify Cloud Spanner Product & Inventory Persistence")
    public void testProductAndInventoryPersistence() {
        String prodId = "test-prod-" + System.currentTimeMillis();
        Product product = Product.builder()
                .id(prodId)
                .sku("SKU-TEST-" + System.currentTimeMillis())
                .name("Persisted Test Blazer")
                .brand("LOGANCEE Atelier")
                .category("Men")
                .subcategory("Outerwear")
                .price(9999.0)
                .originalPrice(12999.0)
                .discountPercentage(23)
                .rating(5.0)
                .reviewCount(1)
                .stockQuantity(15)
                .inStock(true)
                .description("Test blazer product persisted in Cloud Spanner.")
                .colorOptions(Collections.singletonList(new ColorOption("Navy", "#000080", null)))
                .sizes(Arrays.asList("M", "L"))
                .images(Collections.singletonList("https://example.com/blazer.jpg"))
                .build();

        // Save product to Spanner
        spannerDataService.saveProduct(product);

        // Retrieve product from Spanner
        Optional<Product> fetchedOpt = spannerDataService.getProductById(prodId);
        assertTrue(fetchedOpt.isPresent());
        Product fetched = fetchedOpt.get();

        assertEquals("Persisted Test Blazer", fetched.getName());
        assertEquals(9999.0, fetched.getPrice());
        assertEquals(15, fetched.getStockQuantity());
        assertEquals(1, fetched.getColorOptions().size());
        assertEquals("Navy", fetched.getColorOptions().get(0).getName());

        // Cleanup test product
        spannerDataService.deleteProduct(prodId);
        assertFalse(spannerDataService.getProductById(prodId).isPresent());
    }

    @Test
    @DisplayName("Verify Cloud Spanner Cart & Transactional Order Persistence")
    public void testCartAndOrderPersistence() {
        String userId = "user-101"; // Elena Rostova
        
        // 1. Fetch Cart for User
        Cart cart = spannerDataService.getUserCart(userId);
        assertNotNull(cart);

        // 2. Create Order in Cloud Spanner
        OrderItem item = OrderItem.builder()
                .id("oi-test-" + System.currentTimeMillis())
                .productId("prod-1")
                .productName("Monogram Cashmere Wool Overcoat")
                .productImage("https://images.unsplash.com/photo-1544441893-675973e31985")
                .brand("LOGANCEE Atelier")
                .color("Onyx Black")
                .size("L")
                .quantity(1)
                .unitPrice(12999.0)
                .totalPrice(12999.0)
                .build();

        Order order = Order.builder()
                .id("ord-test-" + System.currentTimeMillis())
                .orderNumber("LOG-TEST-" + System.currentTimeMillis())
                .userId(userId)
                .items(Collections.singletonList(item))
                .paymentMethod(PaymentMethodType.CARD)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderStatus(OrderStatus.CONFIRMED)
                .subtotal(12999.0)
                .discount(0.0)
                .shippingFee(0.0)
                .tax(650.0)
                .totalAmount(13649.0)
                .timeline(Collections.singletonList(
                        OrderStatusTimeline.builder().status(OrderStatus.ORDER_PLACED).completed(true).description("Placed via test").build()
                ))
                .build();

        Order savedOrder = spannerDataService.saveOrder(order);
        assertNotNull(savedOrder.getId());

        // 3. Query Order back from Spanner
        Optional<Order> fetchedOrderOpt = spannerDataService.getOrderById(savedOrder.getId());
        assertTrue(fetchedOrderOpt.isPresent());
        Order fetchedOrder = fetchedOrderOpt.get();

        assertEquals(userId, fetchedOrder.getUserId());
        assertEquals(13649.0, fetchedOrder.getTotalAmount());
        assertEquals(1, fetchedOrder.getItems().size());
        assertEquals("Monogram Cashmere Wool Overcoat", fetchedOrder.getItems().get(0).getProductName());
    }

    @Test
    @DisplayName("Verify Cloud Spanner Coupon Persistence")
    public void testCouponPersistence() {
        String code = "TESTCOUPON" + System.currentTimeMillis();
        Coupon coupon = Coupon.builder()
                .id("coup-test-" + System.currentTimeMillis())
                .code(code)
                .discountPercentage(25)
                .maxDiscount(2000.0)
                .minOrderAmount(3000.0)
                .expiryDate("2026-12-31")
                .usageLimit(100)
                .usageCount(0)
                .isActive(true)
                .build();

        spannerDataService.saveCoupon(coupon);

        Optional<Coupon> fetchedOpt = spannerDataService.findCouponByCode(code);
        assertTrue(fetchedOpt.isPresent());
        assertEquals(25, fetchedOpt.get().getDiscountPercentage());
        assertEquals(2000.0, fetchedOpt.get().getMaxDiscount());
    }
}
