import urllib.request
import json

base_url = "http://localhost:8080/api"

def make_request(url, method="GET", headers=None, data=None):
    if headers is None:
        headers = {}
    req = urllib.request.Request(url, method=method, headers=headers)
    if data is not None:
        req.add_header("Content-Type", "application/json")
        body_bytes = json.dumps(data).encode("utf-8")
        req.data = body_bytes
    try:
        with urllib.request.urlopen(req) as resp:
            code = resp.status
            content = resp.read().decode("utf-8")
            return code, json.loads(content) if content else None
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8")
        try:
            parsed = json.loads(content)
        except:
            parsed = content
        return e.code, parsed

print("--- 1. Testing GET /api/products ---")
code, res = make_request(f"{base_url}/products")
print(f"Status: {code}")
print(f"Total Products: {res.get('totalElements') if res else 0}")
print(f"Page Content Size: {len(res.get('content', [])) if res else 0}")

print("\n--- 2. Testing POST /api/auth/login (Customer) ---")
login_data = {
    "email": "elena.rostova@logancee.fashion",
    "password": "Customer@123"
}
code, res = make_request(f"{base_url}/auth/login", method="POST", data=login_data)
print(f"Status: {code}")
token = res.get("token") if res else None
user = res.get("user") if res else None
print(f"Token Received: {token[:20]}..." if token else "No Token")
print(f"User Name: {user.get('name') if user else None}, Role: {user.get('role') if user else None}")

if token:
    headers = {"Authorization": f"Bearer {token}"}

    print("\n--- 3. Testing GET /api/users/me (Authenticated) ---")
    code, me_res = make_request(f"{base_url}/users/me", headers=headers)
    print(f"Status: {code}")
    print(f"User Profile Email: {me_res.get('email') if me_res else None}")

    print("\n--- 4. Testing GET /api/cart (Authenticated) ---")
    code, cart_res = make_request(f"{base_url}/cart", headers=headers)
    print(f"Status: {code}")
    print(f"Cart Items Count: {len(cart_res.get('items', [])) if cart_res else 0}")

print("\n--- 5. Testing POST /api/auth/login (Admin) ---")
admin_login = {
    "email": "admin@logancee.com",
    "password": "Admin@123"
}
code, admin_res = make_request(f"{base_url}/auth/login", method="POST", data=admin_login)
print(f"Status: {code}")
admin_token = admin_res.get("token") if admin_res else None
print(f"Admin Token Received: {admin_token[:20]}..." if admin_token else "No Token")

if admin_token:
    admin_headers = {"Authorization": f"Bearer {admin_token}"}
    print("\n--- 6. Testing GET /api/admin/dashboard (Admin Protected) ---")
    code, dash_res = make_request(f"{base_url}/admin/dashboard", headers=admin_headers)
    print(f"Status: {code}")
    print(f"Dashboard Revenue: {dash_res.get('totalRevenue') if dash_res else None}")
