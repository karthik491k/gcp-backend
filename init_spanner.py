import os
import sys
import json
import time
import urllib.request
import urllib.error

PROJECT_ID = os.environ.get("GCP_PROJECT_ID", "globalcart-dev-project")
INSTANCE_ID = os.environ.get("SPANNER_INSTANCE_ID", "globalcart-spanner-instance")
DATABASE_ID = os.environ.get("SPANNER_DATABASE_ID", "globalcart_db")
BASE_URL = os.environ.get("SPANNER_REST_URL", "http://127.0.0.1:9020/v1")

DDL_STATEMENTS = [
    "CREATE TABLE Users (user_id STRING(64) NOT NULL, email STRING(255) NOT NULL, password_hash STRING(255) NOT NULL, name STRING(255) NOT NULL, phone STRING(32), avatar_url STRING(512), role STRING(32) NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (user_id)",
    "CREATE UNIQUE INDEX idx_users_email ON Users(email)",
    "CREATE TABLE UserAddresses (user_id STRING(64) NOT NULL, address_id STRING(64) NOT NULL, full_name STRING(255) NOT NULL, street STRING(512) NOT NULL, city STRING(128) NOT NULL, state STRING(128) NOT NULL, zip_code STRING(32) NOT NULL, country STRING(128) NOT NULL, phone STRING(32) NOT NULL, is_default BOOL NOT NULL, address_type STRING(32) NOT NULL) PRIMARY KEY (user_id, address_id), INTERLEAVE IN PARENT Users ON DELETE CASCADE",
    "CREATE TABLE Categories (category_id STRING(64) NOT NULL, name STRING(128) NOT NULL, slug STRING(128) NOT NULL, image_url STRING(512) NOT NULL, description STRING(1024), item_count INT64 NOT NULL) PRIMARY KEY (category_id)",
    "CREATE TABLE Brands (brand_id STRING(64) NOT NULL, name STRING(128) NOT NULL, logo_url STRING(512) NOT NULL, category STRING(128) NOT NULL) PRIMARY KEY (brand_id)",
    "CREATE TABLE Products (product_id STRING(64) NOT NULL, sku STRING(64) NOT NULL, name STRING(255) NOT NULL, brand STRING(128) NOT NULL, category STRING(128) NOT NULL, subcategory STRING(128) NOT NULL, description STRING(4096) NOT NULL, price NUMERIC NOT NULL, original_price NUMERIC NOT NULL, discount_percentage INT64 NOT NULL, rating FLOAT64 NOT NULL, review_count INT64 NOT NULL, stock_quantity INT64 NOT NULL, in_stock BOOL NOT NULL, hover_image_url STRING(512), is_new BOOL NOT NULL, is_trending BOOL NOT NULL, is_bestseller BOOL NOT NULL, is_flash_sale BOOL NOT NULL, flash_sale_end_time TIMESTAMP, specifications_json STRING(MAX), size_and_fit STRING(2048), shipping_info STRING(2048), return_policy STRING(2048), created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (product_id)",
    "CREATE INDEX idx_products_category ON Products(category)",
    "CREATE INDEX idx_products_brand ON Products(brand)",
    "CREATE UNIQUE INDEX idx_products_sku ON Products(sku)",
    "CREATE TABLE ProductColorOptions (product_id STRING(64) NOT NULL, color_name STRING(64) NOT NULL, color_hex STRING(32) NOT NULL, image_url STRING(512)) PRIMARY KEY (product_id, color_name), INTERLEAVE IN PARENT Products ON DELETE CASCADE",
    "CREATE TABLE ProductImages (product_id STRING(64) NOT NULL, image_order INT64 NOT NULL, image_url STRING(512) NOT NULL) PRIMARY KEY (product_id, image_order), INTERLEAVE IN PARENT Products ON DELETE CASCADE",
    "CREATE TABLE ProductSizes (product_id STRING(64) NOT NULL, size STRING(32) NOT NULL) PRIMARY KEY (product_id, size), INTERLEAVE IN PARENT Products ON DELETE CASCADE",
    "CREATE TABLE Carts (user_id STRING(64) NOT NULL, coupon_code STRING(64), updated_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (user_id)",
    "CREATE TABLE CartItems (user_id STRING(64) NOT NULL, item_id STRING(64) NOT NULL, product_id STRING(64) NOT NULL, color STRING(64) NOT NULL, size STRING(32) NOT NULL, quantity INT64 NOT NULL, unit_price NUMERIC NOT NULL) PRIMARY KEY (user_id, item_id), INTERLEAVE IN PARENT Carts ON DELETE CASCADE",
    "CREATE TABLE WishlistItems (user_id STRING(64) NOT NULL, product_id STRING(64) NOT NULL, added_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (user_id, product_id), INTERLEAVE IN PARENT Users ON DELETE CASCADE",
    "CREATE TABLE Orders (order_id STRING(64) NOT NULL, order_number STRING(64) NOT NULL, user_id STRING(64) NOT NULL, shipping_address_json STRING(MAX) NOT NULL, payment_method STRING(32) NOT NULL, payment_status STRING(32) NOT NULL, order_status STRING(64) NOT NULL, subtotal NUMERIC NOT NULL, discount NUMERIC NOT NULL, shipping_fee NUMERIC NOT NULL, tax NUMERIC NOT NULL, total_amount NUMERIC NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true), estimated_delivery STRING(128) NOT NULL, tracking_number STRING(128)) PRIMARY KEY (order_id)",
    "CREATE INDEX idx_orders_user_id ON Orders(user_id)",
    "CREATE UNIQUE INDEX idx_orders_order_number ON Orders(order_number)",
    "CREATE TABLE OrderItems (order_id STRING(64) NOT NULL, item_id STRING(64) NOT NULL, product_id STRING(64) NOT NULL, product_name STRING(255) NOT NULL, product_image STRING(512) NOT NULL, brand STRING(128) NOT NULL, color STRING(64) NOT NULL, size STRING(32) NOT NULL, quantity INT64 NOT NULL, unit_price NUMERIC NOT NULL, total_price NUMERIC NOT NULL) PRIMARY KEY (order_id, item_id), INTERLEAVE IN PARENT Orders ON DELETE CASCADE",
    "CREATE TABLE OrderTimeline (order_id STRING(64) NOT NULL, status STRING(64) NOT NULL, timestamp_str STRING(128), completed BOOL NOT NULL, description STRING(512) NOT NULL) PRIMARY KEY (order_id, status), INTERLEAVE IN PARENT Orders ON DELETE CASCADE",
    "CREATE TABLE Reviews (product_id STRING(64) NOT NULL, review_id STRING(64) NOT NULL, user_id STRING(64) NOT NULL, user_name STRING(255) NOT NULL, user_avatar STRING(512), rating INT64 NOT NULL, title STRING(255) NOT NULL, comment STRING(2048) NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true), verified_purchase BOOL NOT NULL, helpful_count INT64 NOT NULL) PRIMARY KEY (product_id, review_id), INTERLEAVE IN PARENT Products ON DELETE CASCADE",
    "CREATE TABLE Coupons (coupon_id STRING(64) NOT NULL, code STRING(64) NOT NULL, discount_percentage INT64 NOT NULL, max_discount NUMERIC NOT NULL, min_order_amount NUMERIC NOT NULL, expiry_date STRING(32) NOT NULL, usage_limit INT64 NOT NULL, usage_count INT64 NOT NULL, is_active BOOL NOT NULL) PRIMARY KEY (coupon_id)",
    "CREATE UNIQUE INDEX idx_coupons_code ON Coupons(code)",
    "CREATE TABLE Inventory (product_id STRING(64) NOT NULL, inventory_id STRING(64) NOT NULL, product_name STRING(255) NOT NULL, sku STRING(64) NOT NULL, category STRING(128) NOT NULL, available_stock INT64 NOT NULL, reserved_stock INT64 NOT NULL, total_sold INT64 NOT NULL, status STRING(32) NOT NULL) PRIMARY KEY (product_id)",
    "CREATE TABLE Notifications (user_id STRING(64) NOT NULL, notification_id STRING(64) NOT NULL, title STRING(255) NOT NULL, message STRING(1024) NOT NULL, type STRING(32) NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true), read BOOL NOT NULL) PRIMARY KEY (user_id, notification_id), INTERLEAVE IN PARENT Users ON DELETE CASCADE"
]

def http_json_request(url, method="GET", payload=None):
    data_bytes = json.dumps(payload).encode("utf-8") if payload is not None else None
    headers = {"Content-Type": "application/json"} if payload is not None else {}
    req = urllib.request.Request(url, data=data_bytes, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, json.loads(body) if body else {}
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, {"error": body}
    except Exception as e:
        return 0, {"error": str(e)}

def wait_for_emulator(timeout=30):
    print(f"[INIT] Waiting for Spanner Emulator REST endpoint ({BASE_URL})...")
    start = time.time()
    url = f"{BASE_URL}/projects/{PROJECT_ID}/instances"
    while time.time() - start < timeout:
        status, res = http_json_request(url, method="GET")
        if status in (200, 404):
            print(f"[INIT] Spanner Emulator is responsive (HTTP {status}).")
            return True
        time.sleep(1)
    print(f"[ERROR] Spanner Emulator failed to respond within {timeout}s.")
    return False

def init_spanner():
    if not wait_for_emulator():
        sys.exit(1)

    # 1. Check / Create Instance
    inst_url = f"{BASE_URL}/projects/{PROJECT_ID}/instances"
    status, res = http_json_request(inst_url, method="GET")
    instances = res.get("instances", [])
    instance_found = any(i.get("name", "").endswith(f"/instances/{INSTANCE_ID}") for i in instances)

    if not instance_found:
        print(f"[INIT] Creating Spanner instance '{INSTANCE_ID}'...")
        inst_payload = {
            "instanceId": INSTANCE_ID,
            "instance": {
                "config": f"projects/{PROJECT_ID}/instanceConfigs/emulator-config",
                "displayName": "Local Dev Spanner",
                "nodeCount": 1
            }
        }
        status, res = http_json_request(inst_url, method="POST", payload=inst_payload)
        if status not in (200, 201):
            print(f"[ERROR] Failed to create instance: status {status}, response: {res}")
            sys.exit(1)
        print(f"[INIT] Instance '{INSTANCE_ID}' created successfully.")
    else:
        print(f"[INIT] Instance '{INSTANCE_ID}' already exists.")

    # 2. Check / Create Database with DDL
    db_list_url = f"{BASE_URL}/projects/{PROJECT_ID}/instances/{INSTANCE_ID}/databases"
    status, res = http_json_request(db_list_url, method="GET")
    databases = res.get("databases", [])
    database_found = any(d.get("name", "").endswith(f"/databases/{DATABASE_ID}") for d in databases)

    if not database_found:
        print(f"[INIT] Creating Spanner database '{DATABASE_ID}' with {len(DDL_STATEMENTS)} DDL statements...")
        db_payload = {
            "createStatement": f"CREATE DATABASE {DATABASE_ID}",
            "extraStatements": DDL_STATEMENTS
        }
        status, res = http_json_request(db_list_url, method="POST", payload=db_payload)
        if status not in (200, 201):
            print(f"[ERROR] Failed to create database: status {status}, response: {res}")
            sys.exit(1)
        print(f"[INIT] Database '{DATABASE_ID}' created with schema successfully.")
    else:
        print(f"[INIT] Database '{DATABASE_ID}' already exists.")

    # 3. Verify Tables
    verify_url = f"{BASE_URL}/projects/{PROJECT_ID}/instances/{INSTANCE_ID}/databases/{DATABASE_ID}/sessions"
    status, session = http_json_request(verify_url, method="POST")
    if status == 200 and "name" in session:
        session_name = session["name"]
        sql_url = f"{BASE_URL}/{session_name}:executeSql"
        sql_payload = {"sql": "SELECT table_name FROM information_schema.tables WHERE table_schema = ''"}
        status, tables_res = http_json_request(sql_url, method="POST", payload=sql_payload)
        rows = tables_res.get("rows", [])
        table_names = [r[0] for r in rows if r]
        print(f"[INIT] Verified {len(table_names)} tables in Cloud Spanner:")
        print(f"       {', '.join(sorted(table_names))}")
    else:
        print(f"[WARNING] Could not create session for table verification (status {status}).")

    print("[INIT] Cloud Spanner database initialization finished successfully!")

if __name__ == "__main__":
    init_spanner()
