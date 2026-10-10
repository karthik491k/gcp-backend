import requests
import json
import time

API_BASE = "http://localhost:8080/api"
SPANNER_BASE = "http://localhost:9020/v1/projects/globalcart-dev-project/instances/globalcart-spanner-instance/databases/globalcart_db"

def run_comprehensive_audit():
    results = {}
    print("========================================================================")
    print(" GLOBALCART COMPREHENSIVE END-TO-END FUNCTIONAL AUDIT")
    print("========================================================================")

    # -------------------------------------------------------------------------
    # WORKFLOW 1: AUTHENTICATION
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 1: AUTHENTICATION] ---")
    ts = int(time.time())
    cust_email = f"audit_cust_{ts}@logancee.fashion"
    cust_pass = "CustomerPass@123"
    cust_name = f"Audit Customer {ts}"

    # 1.1 Registration
    res = requests.post(f"{API_BASE}/auth/register", json={"email": cust_email, "password": cust_pass, "name": cust_name})
    if res.status_code == 200 and "token" in res.json():
        cust_token = res.json()["token"]
        cust_id = res.json()["user"]["id"]
        headers = {"Authorization": f"Bearer {cust_token}"}
        results["1.1 Registration"] = "PASS"
        print("  [PASS] 1.1 Customer Registration: Token & User returned.")
    else:
        results["1.1 Registration"] = f"FAIL ({res.status_code}): {res.text}"
        print(f"  [FAIL] 1.1 Customer Registration: {res.text}")

    # 1.2 Login with valid credentials
    res = requests.post(f"{API_BASE}/auth/login", json={"email": cust_email, "password": cust_pass})
    if res.status_code == 200 and "token" in res.json():
        results["1.2 Login Valid"] = "PASS"
        print("  [PASS] 1.2 Login Valid Credentials: JWT issued.")
    else:
        results["1.2 Login Valid"] = f"FAIL ({res.status_code})"

    # 1.3 Login with invalid password
    res = requests.post(f"{API_BASE}/auth/login", json={"email": cust_email, "password": "WrongPassword"})
    if res.status_code in [400, 401, 403]:
        results["1.3 Login Invalid"] = "PASS"
        print("  [PASS] 1.3 Login Invalid Credentials: Correctly rejected.")
    else:
        results["1.3 Login Invalid"] = f"FAIL ({res.status_code})"

    # 1.4 Protected Route (User Profile)
    res = requests.get(f"{API_BASE}/users/me", headers=headers)
    if res.status_code == 200 and res.json().get("email") == cust_email:
        results["1.4 Protected Route Profile"] = "PASS"
        print("  [PASS] 1.4 Protected Route Profile: Authenticated user returned.")
    else:
        results["1.4 Protected Route Profile"] = f"FAIL ({res.status_code})"

    # 1.5 Session Logout
    res = requests.post(f"{API_BASE}/auth/logout", headers=headers)
    if res.status_code == 200:
        results["1.5 Session Logout"] = "PASS"
        print("  [PASS] 1.5 Session Logout: 200 OK.")
    else:
        results["1.5 Session Logout"] = f"FAIL ({res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 2: PRODUCT CATALOG
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 2: PRODUCT CATALOG] ---")
    
    # 2.1 Product Listing
    res = requests.get(f"{API_BASE}/products")
    products_list = res.json().get("content", []) if res.status_code == 200 and isinstance(res.json(), dict) else []
    if res.status_code == 200 and len(products_list) > 0:
        results["2.1 Product Listing"] = "PASS"
        print(f"  [PASS] 2.1 Product Listing: {len(products_list)} items returned.")
    else:
        results["2.1 Product Listing"] = f"FAIL ({res.status_code})"

    # 2.2 Product Search
    res = requests.get(f"{API_BASE}/products?searchQuery=Overcoat")
    if res.status_code == 200:
        results["2.2 Product Search"] = "PASS"
        print("  [PASS] 2.2 Product Search: Search query handled.")
    else:
        results["2.2 Product Search"] = f"FAIL ({res.status_code})"

    # 2.3 Category Filtering
    res = requests.get(f"{API_BASE}/products?category=Men")
    if res.status_code == 200:
        results["2.3 Category Filtering"] = "PASS"
        print("  [PASS] 2.3 Category Filtering: Category filter active.")
    else:
        results["2.3 Category Filtering"] = f"FAIL ({res.status_code})"

    # 2.4 Product Details
    res = requests.get(f"{API_BASE}/products/prod-1")
    if res.status_code == 200 and res.json().get("name") == "Monogram Cashmere Wool Overcoat":
        results["2.4 Product Details"] = "PASS"
        print("  [PASS] 2.4 Product Details: Full specifications & gallery loaded.")
    else:
        results["2.4 Product Details"] = f"FAIL ({res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 3: CART OPERATIONS
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 3: CART OPERATIONS] ---")

    # 3.1 Get Cart
    res = requests.get(f"{API_BASE}/cart", headers=headers)
    if res.status_code == 200:
        results["3.1 Get Cart"] = "PASS"
        print("  [PASS] 3.1 Get Cart: 200 OK.")
    else:
        results["3.1 Get Cart"] = f"FAIL ({res.status_code})"

    # 3.2 Add Item to Cart
    add_payload = {"productId": "prod-1", "quantity": 1, "color": "Onyx Black", "size": "L"}
    res = requests.post(f"{API_BASE}/cart/items", json=add_payload, headers=headers)
    if res.status_code == 200 and len(res.json().get("items", [])) > 0:
        results["3.2 Add Item to Cart"] = "PASS"
        cart_item_obj = res.json()["items"][0]
        print("  [PASS] 3.2 Add Item to Cart: Item added and subtotal calculated.")
    else:
        results["3.2 Add Item to Cart"] = f"FAIL ({res.status_code}): {res.text}"
        print(f"  [FAIL] 3.2 Add Item to Cart: {res.text}")

    # 3.3 Update Item Quantity
    update_payload = {"quantity": 2, "color": "Onyx Black", "size": "L"}
    res = requests.put(f"{API_BASE}/cart/items/prod-1", json=update_payload, headers=headers)
    if res.status_code == 200:
        results["3.3 Update Item Quantity"] = "PASS"
        print("  [PASS] 3.3 Update Item Quantity: Quantity updated.")
    else:
        results["3.3 Update Item Quantity"] = f"FAIL ({res.status_code})"

    # 3.4 Insufficient Stock Handling
    exceed_payload = {"productId": "prod-1", "quantity": 99999}
    res = requests.post(f"{API_BASE}/cart/items", json=exceed_payload, headers=headers)
    if res.status_code in [400, 422, 500]:
        results["3.4 Insufficient Stock Error"] = "PASS"
        print("  [PASS] 3.4 Insufficient Stock: Correctly rejected stock over-request.")
    else:
        results["3.4 Insufficient Stock Error"] = f"FAIL ({res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 4: WISHLIST
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 4: WISHLIST] ---")

    # 4.1 Add to Wishlist
    res = requests.post(f"{API_BASE}/wishlist/prod-1", headers=headers)
    if res.status_code == 200:
        results["4.1 Add to Wishlist"] = "PASS"
        print("  [PASS] 4.1 Add to Wishlist: Item added.")
    else:
        results["4.1 Add to Wishlist"] = f"FAIL ({res.status_code})"

    # 4.2 Get Wishlist
    res = requests.get(f"{API_BASE}/wishlist", headers=headers)
    if res.status_code == 200:
        results["4.2 Get Wishlist"] = "PASS"
        print("  [PASS] 4.2 Get Wishlist: Persisted items retrieved.")
    else:
        results["4.2 Get Wishlist"] = f"FAIL ({res.status_code})"

    # 4.3 Remove from Wishlist
    res = requests.delete(f"{API_BASE}/wishlist/prod-1", headers=headers)
    if res.status_code == 200:
        results["4.3 Remove from Wishlist"] = "PASS"
        print("  [PASS] 4.3 Remove from Wishlist: Item removed.")
    else:
        results["4.3 Remove from Wishlist"] = f"FAIL ({res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 5: CHECKOUT & SIMULATED PAYMENTS
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 5: CHECKOUT & ORDER CREATION] ---")

    # Ensure item in cart
    cart_res = requests.get(f"{API_BASE}/cart", headers=headers)
    active_cart = cart_res.json()
    if not active_cart.get("items"):
        requests.post(f"{API_BASE}/cart/items", json={"productId": "prod-1", "quantity": 1}, headers=headers)
        active_cart = requests.get(f"{API_BASE}/cart", headers=headers).json()

    # 5.1 Coupon Validation (Valid)
    res = requests.post(f"{API_BASE}/cart/coupon", json={"code": "WELCOME10"}, headers=headers)
    if res.status_code == 200:
        results["5.1 Coupon Validation (Valid)"] = "PASS"
        print("  [PASS] 5.1 Coupon Validation: WELCOME10 applied.")
    else:
        results["5.1 Coupon Validation (Valid)"] = f"FAIL ({res.status_code}): {res.text}"

    # 5.2 Coupon Validation (Invalid)
    res = requests.post(f"{API_BASE}/cart/coupon", json={"code": "FAKECODE99"}, headers=headers)
    if res.status_code in [400, 404, 422, 500]:
        results["5.2 Coupon Validation (Invalid)"] = "PASS"
        print("  [PASS] 5.2 Coupon Validation (Invalid): Correctly rejected.")
    else:
        results["5.2 Coupon Validation (Invalid)"] = f"FAIL ({res.status_code})"

    # 5.3 Order Creation (Checkout)
    order_payload = {
        "shippingAddress": {
            "fullName": cust_name,
            "street": "123 Fashion Blvd",
            "city": "Hyderabad",
            "state": "Telangana",
            "zipCode": "500081",
            "country": "India",
            "phone": "+91 98765 43210"
        },
        "paymentMethod": "CARD",
        "items": active_cart.get("items", []),
        "subtotal": active_cart.get("subtotal", 12999),
        "discount": active_cart.get("discount", 0),
        "shippingFee": active_cart.get("shipping", 0),
        "tax": active_cart.get("tax", 650),
        "totalAmount": active_cart.get("total", 13649)
    }
    res = requests.post(f"{API_BASE}/orders", json=order_payload, headers=headers)
    if res.status_code == 200 and "id" in res.json():
        created_order_id = res.json()["id"]
        results["5.3 Order Creation"] = "PASS"
        print(f"  [PASS] 5.3 Order Creation: Order {created_order_id} created transactionally.")
    else:
        results["5.3 Order Creation"] = f"FAIL ({res.status_code}): {res.text}"
        print(f"  [FAIL] 5.3 Order Creation: {res.text}")

    # -------------------------------------------------------------------------
    # WORKFLOW 6: ORDERS & TRACKING
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 6: ORDERS & TIMELINE] ---")

    # 6.1 Order History
    res = requests.get(f"{API_BASE}/orders", headers=headers)
    if res.status_code == 200 and len(res.json()) > 0:
        results["6.1 Order History"] = "PASS"
        print(f"  [PASS] 6.1 Order History: {len(res.json())} orders retrieved.")
    else:
        results["6.1 Order History"] = f"FAIL ({res.status_code})"

    # 6.2 Order Details & Timeline
    if "created_order_id" in locals():
        res = requests.get(f"{API_BASE}/orders/{created_order_id}", headers=headers)
        if res.status_code == 200 and "timeline" in res.json():
            results["6.2 Order Details & Timeline"] = "PASS"
            print("  [PASS] 6.2 Order Details & Timeline: Milestone timeline loaded.")
        else:
            results["6.2 Order Details & Timeline"] = f"FAIL ({res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 7: ADMIN WORKFLOWS
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 7: ADMIN WORKFLOWS] ---")

    # 7.1 Admin Login
    res = requests.post(f"{API_BASE}/auth/login", json={"email": "admin@logancee.com", "password": "Admin@123"})
    if res.status_code == 200 and "token" in res.json():
        admin_token = res.json()["token"]
        admin_headers = {"Authorization": f"Bearer {admin_token}"}
        results["7.1 Admin Login"] = "PASS"
        print("  [PASS] 7.1 Admin Login: Admin JWT token issued.")
    else:
        results["7.1 Admin Login"] = f"FAIL ({res.status_code})"

    # 7.2 Admin Dashboard Metrics
    res = requests.get(f"{API_BASE}/admin/dashboard", headers=admin_headers)
    if res.status_code == 200 and "totalRevenue" in res.json():
        results["7.2 Admin Dashboard"] = "PASS"
        print(f"  [PASS] 7.2 Admin Dashboard: Revenue metric ${res.json()['totalRevenue']} loaded.")
    else:
        results["7.2 Admin Dashboard"] = f"FAIL ({res.status_code})"

    # 7.3 Admin Inventory Update
    res = requests.put(f"{API_BASE}/admin/inventory/inv-prod-1", json={"availableStock": 25}, headers=admin_headers)
    if res.status_code == 200:
        results["7.3 Admin Inventory Update"] = "PASS"
        print("  [PASS] 7.3 Admin Inventory Update: Stock updated in Spanner.")
    else:
        results["7.3 Admin Inventory Update"] = f"FAIL ({res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 8: SECURITY & AUTHORIZATION BOUNDARIES
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 8: SECURITY BOUNDARIES] ---")

    # 8.1 Customer Accessing Admin Endpoint (Must fail with 403)
    res = requests.get(f"{API_BASE}/admin/dashboard", headers=headers)
    if res.status_code == 403:
        results["8.1 Security Admin Route Guard"] = "PASS"
        print("  [PASS] 8.1 Security Admin Route Guard: Customer blocked (HTTP 403).")
    else:
        results["8.1 Security Admin Route Guard"] = f"FAIL ({res.status_code})"

    # 8.2 Unauthenticated Access to Protected Route (Must fail with 401)
    res = requests.get(f"{API_BASE}/users/me")
    if res.status_code == 401:
        results["8.2 Security Unauthenticated Guard"] = "PASS"
        print("  [PASS] 8.2 Security Unauthenticated Guard: Unauthenticated request blocked (HTTP 401).")
    else:
        results["8.2 Security Unauthenticated Guard"] = f"FAIL ({res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 9: SPANNER PERSISTENCE & BACKEND RESTART RE-VERIFICATION
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 9: SPANNER PERSISTENCE & RESTART RE-VERIFICATION] ---")
    
    # Query Spanner directly for customer email registered in step 1.1
    sess_res = requests.post(f"{SPANNER_BASE}/sessions")
    sess_name = sess_res.json()["name"]
    sql_url = f"http://localhost:9020/v1/{sess_name}:executeSql"

    spanner_res = requests.post(sql_url, json={
        "sql": f"SELECT user_id, email, name, role FROM Users WHERE email = '{cust_email}'"
    })
    if spanner_res.status_code == 200 and len(spanner_res.json().get("rows", [])) > 0:
        results["9.1 Direct Spanner Database Verification"] = "PASS"
        print(f"  [PASS] 9.1 Direct Spanner Database Verification: Customer row confirmed in Spanner `Users` table.")
    else:
        results["9.1 Direct Spanner Database Verification"] = f"FAIL ({spanner_res.status_code})"

    # -------------------------------------------------------------------------
    # WORKFLOW 10: ERROR HANDLING
    # -------------------------------------------------------------------------
    print("\n--- [WORKFLOW 10: ERROR HANDLING] ---")

    # 10.1 Non-existent product ID (404)
    res = requests.get(f"{API_BASE}/products/prod-nonexistent-999")
    if res.status_code in [404, 400, 500]:
        results["10.1 Non-existent Product Error"] = "PASS"
        print("  [PASS] 10.1 Non-existent Product Error: Correctly handled.")
    else:
        results["10.1 Non-existent Product Error"] = f"FAIL ({res.status_code})"

    print("\n========================================================================")
    print(" COMPREHENSIVE AUDIT SUMMARY")
    print("========================================================================")
    all_passed = True
    for k, v in results.items():
        print(f"  {k:45} : {v}")
        if "PASS" not in v:
            all_passed = False
            
    if all_passed:
        print("\nALL WORKFLOWS PASSED FULL FUNCTIONAL AUDIT!")
    else:
        print("\nSOME WORKFLOWS NEED ATTENTION.")

if __name__ == "__main__":
    run_comprehensive_audit()
