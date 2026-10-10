-- ============================================================================
-- GOOGLE CLOUD SPANNER PRODUCTION & EMULATOR DDL SCHEMA
-- Project: GlobalCart Luxury Fashion & Lifestyle Marketplace
-- Architecture: Distributed Global Spanner Schema with Interleaved Child Tables
-- ============================================================================

-- 1. USERS TABLE (Root Table)
CREATE TABLE Users (
    user_id STRING(64) NOT NULL,
    email STRING(255) NOT NULL,
    password_hash STRING(255) NOT NULL,
    name STRING(255) NOT NULL,
    phone STRING(32),
    avatar_url STRING(512),
    role STRING(32) NOT NULL, -- CUSTOMER, ADMIN, SELLER
    created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)
) PRIMARY KEY (user_id);

CREATE UNIQUE INDEX idx_users_email ON Users(email);

-- 2. USER ADDRESSES (Interleaved inside Users)
CREATE TABLE UserAddresses (
    user_id STRING(64) NOT NULL,
    address_id STRING(64) NOT NULL,
    full_name STRING(255) NOT NULL,
    street STRING(512) NOT NULL,
    city STRING(128) NOT NULL,
    state STRING(128) NOT NULL,
    zip_code STRING(32) NOT NULL,
    country STRING(128) NOT NULL,
    phone STRING(32) NOT NULL,
    is_default BOOL NOT NULL,
    address_type STRING(32) NOT NULL -- Home, Work, Other
) PRIMARY KEY (user_id, address_id),
  INTERLEAVE IN PARENT Users ON DELETE CASCADE;

-- 3. CATEGORIES & BRANDS
CREATE TABLE Categories (
    category_id STRING(64) NOT NULL,
    name STRING(128) NOT NULL,
    slug STRING(128) NOT NULL,
    image_url STRING(512) NOT NULL,
    description STRING(1024),
    item_count INT64 NOT NULL
) PRIMARY KEY (category_id);

CREATE TABLE Brands (
    brand_id STRING(64) NOT NULL,
    name STRING(128) NOT NULL,
    logo_url STRING(512) NOT NULL,
    category STRING(128) NOT NULL
) PRIMARY KEY (brand_id);

-- 4. PRODUCTS TABLE
CREATE TABLE Products (
    product_id STRING(64) NOT NULL,
    sku STRING(64) NOT NULL,
    name STRING(255) NOT NULL,
    brand STRING(128) NOT NULL,
    category STRING(128) NOT NULL,
    subcategory STRING(128) NOT NULL,
    description STRING(4096) NOT NULL,
    price NUMERIC NOT NULL,
    original_price NUMERIC NOT NULL,
    discount_percentage INT64 NOT NULL,
    rating FLOAT64 NOT NULL,
    review_count INT64 NOT NULL,
    stock_quantity INT64 NOT NULL,
    in_stock BOOL NOT NULL,
    hover_image_url STRING(512),
    is_new BOOL NOT NULL,
    is_trending BOOL NOT NULL,
    is_bestseller BOOL NOT NULL,
    is_flash_sale BOOL NOT NULL,
    flash_sale_end_time TIMESTAMP,
    specifications_json STRING(MAX), -- JSON key-value pairs
    size_and_fit STRING(2048),
    shipping_info STRING(2048),
    return_policy STRING(2048),
    created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)
) PRIMARY KEY (product_id);

CREATE INDEX idx_products_category ON Products(category);
CREATE INDEX idx_products_brand ON Products(brand);
CREATE UNIQUE INDEX idx_products_sku ON Products(sku);

-- 5. PRODUCT COLOR OPTIONS & IMAGES (Interleaved inside Products)
CREATE TABLE ProductColorOptions (
    product_id STRING(64) NOT NULL,
    color_name STRING(64) NOT NULL,
    color_hex STRING(32) NOT NULL,
    image_url STRING(512)
) PRIMARY KEY (product_id, color_name),
  INTERLEAVE IN PARENT Products ON DELETE CASCADE;

CREATE TABLE ProductImages (
    product_id STRING(64) NOT NULL,
    image_order INT64 NOT NULL,
    image_url STRING(512) NOT NULL
) PRIMARY KEY (product_id, image_order),
  INTERLEAVE IN PARENT Products ON DELETE CASCADE;

CREATE TABLE ProductSizes (
    product_id STRING(64) NOT NULL,
    size STRING(32) NOT NULL
) PRIMARY KEY (product_id, size),
  INTERLEAVE IN PARENT Products ON DELETE CASCADE;

-- 6. CARTS & CART ITEMS (Interleaved inside Carts)
CREATE TABLE Carts (
    user_id STRING(64) NOT NULL,
    coupon_code STRING(64),
    updated_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)
) PRIMARY KEY (user_id);

CREATE TABLE CartItems (
    user_id STRING(64) NOT NULL,
    item_id STRING(64) NOT NULL,
    product_id STRING(64) NOT NULL,
    color STRING(64) NOT NULL,
    size STRING(32) NOT NULL,
    quantity INT64 NOT NULL,
    unit_price NUMERIC NOT NULL
) PRIMARY KEY (user_id, item_id),
  INTERLEAVE IN PARENT Carts ON DELETE CASCADE;

-- 7. WISHLIST ITEMS (Interleaved inside Users)
CREATE TABLE WishlistItems (
    user_id STRING(64) NOT NULL,
    product_id STRING(64) NOT NULL,
    added_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)
) PRIMARY KEY (user_id, product_id),
  INTERLEAVE IN PARENT Users ON DELETE CASCADE;

-- 8. ORDERS & ORDER ITEMS
CREATE TABLE Orders (
    order_id STRING(64) NOT NULL,
    order_number STRING(64) NOT NULL,
    user_id STRING(64) NOT NULL,
    shipping_address_json STRING(MAX) NOT NULL,
    payment_method STRING(32) NOT NULL, -- UPI, CARD, NET_BANKING, COD
    payment_status STRING(32) NOT NULL, -- PENDING, COMPLETED, FAILED, REFUNDED
    order_status STRING(64) NOT NULL,   -- ORDER_PLACED, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    subtotal NUMERIC NOT NULL,
    discount NUMERIC NOT NULL,
    shipping_fee NUMERIC NOT NULL,
    tax NUMERIC NOT NULL,
    total_amount NUMERIC NOT NULL,
    created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true),
    estimated_delivery STRING(128) NOT NULL,
    tracking_number STRING(128)
) PRIMARY KEY (order_id);

CREATE INDEX idx_orders_user_id ON Orders(user_id);
CREATE UNIQUE INDEX idx_orders_order_number ON Orders(order_number);

CREATE TABLE OrderItems (
    order_id STRING(64) NOT NULL,
    item_id STRING(64) NOT NULL,
    product_id STRING(64) NOT NULL,
    product_name STRING(255) NOT NULL,
    product_image STRING(512) NOT NULL,
    brand STRING(128) NOT NULL,
    color STRING(64) NOT NULL,
    size STRING(32) NOT NULL,
    quantity INT64 NOT NULL,
    unit_price NUMERIC NOT NULL,
    total_price NUMERIC NOT NULL
) PRIMARY KEY (order_id, item_id),
  INTERLEAVE IN PARENT Orders ON DELETE CASCADE;

CREATE TABLE OrderTimeline (
    order_id STRING(64) NOT NULL,
    status STRING(64) NOT NULL,
    timestamp_str STRING(128),
    completed BOOL NOT NULL,
    description STRING(512) NOT NULL
) PRIMARY KEY (order_id, status),
  INTERLEAVE IN PARENT Orders ON DELETE CASCADE;

-- 9. REVIEWS & COUPONS
CREATE TABLE Reviews (
    product_id STRING(64) NOT NULL,
    review_id STRING(64) NOT NULL,
    user_id STRING(64) NOT NULL,
    user_name STRING(255) NOT NULL,
    user_avatar STRING(512),
    rating INT64 NOT NULL,
    title STRING(255) NOT NULL,
    comment STRING(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true),
    verified_purchase BOOL NOT NULL,
    helpful_count INT64 NOT NULL
) PRIMARY KEY (product_id, review_id),
  INTERLEAVE IN PARENT Products ON DELETE CASCADE;

CREATE TABLE Coupons (
    coupon_id STRING(64) NOT NULL,
    code STRING(64) NOT NULL,
    discount_percentage INT64 NOT NULL,
    max_discount NUMERIC NOT NULL,
    min_order_amount NUMERIC NOT NULL,
    expiry_date STRING(32) NOT NULL,
    usage_limit INT64 NOT NULL,
    usage_count INT64 NOT NULL,
    is_active BOOL NOT NULL
) PRIMARY KEY (coupon_id);

CREATE UNIQUE INDEX idx_coupons_code ON Coupons(code);

-- 10. INVENTORY & NOTIFICATIONS
CREATE TABLE Inventory (
    product_id STRING(64) NOT NULL,
    inventory_id STRING(64) NOT NULL,
    product_name STRING(255) NOT NULL,
    sku STRING(64) NOT NULL,
    category STRING(128) NOT NULL,
    available_stock INT64 NOT NULL,
    reserved_stock INT64 NOT NULL,
    total_sold INT64 NOT NULL,
    status STRING(32) NOT NULL -- IN_STOCK, LOW_STOCK, OUT_OF_STOCK
) PRIMARY KEY (product_id);

CREATE TABLE Notifications (
    user_id STRING(64) NOT NULL,
    notification_id STRING(64) NOT NULL,
    title STRING(255) NOT NULL,
    message STRING(1024) NOT NULL,
    type STRING(32) NOT NULL, -- ORDER, WISHLIST, PROMO, SYSTEM
    created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true),
    read BOOL NOT NULL
) PRIMARY KEY (user_id, notification_id),
  INTERLEAVE IN PARENT Users ON DELETE CASCADE;
