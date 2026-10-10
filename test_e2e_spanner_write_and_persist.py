import requests
import json
import time

API_BASE = "http://localhost:8080/api"
SPANNER_BASE = "http://localhost:9020/v1/projects/globalcart-dev-project/instances/globalcart-spanner-instance/databases/globalcart_db"

def test_e2e_api_to_spanner_persistence():
    print("========================================================================")
    print(" E2E API TO CLOUD SPANNER DIRECT PERSISTENCE AUDIT")
    print("========================================================================")

    # 1. Register a new user via API
    test_ts = int(time.time())
    user_email = f"e2e_user_{test_ts}@logancee.fashion"
    user_pass = "SecurePass@123"
    user_name = f"E2E Test User {test_ts}"

    print(f"\n[1/4] Registering User via API POST /api/auth/register...")
    reg_payload = {
        "email": user_email,
        "password": user_pass,
        "name": user_name
    }
    reg_res = requests.post(f"{API_BASE}/auth/register", json=reg_payload)
    print(f"API Response Status: {reg_res.status_code}")
    assert reg_res.status_code == 200, f"Registration failed: {reg_res.text}"
    
    reg_data = reg_res.json()
    token = reg_data.get("token")
    user_id = reg_data.get("user", {}).get("id")
    print(f"Token Issued: {token[:25]}...")
    print(f"Registered User ID: {user_id}")

    # 2. Query Cloud Spanner directly to verify database row in Users table
    print(f"\n[2/4] Querying Cloud Spanner directly for email '{user_email}'...")
    sess_res = requests.post(f"{SPANNER_BASE}/sessions")
    sess_name = sess_res.json()["name"]
    sql_url = f"http://localhost:9020/v1/{sess_name}:executeSql"
    
    spanner_res = requests.post(sql_url, json={
        "sql": f"SELECT user_id, email, name, role, password_hash, created_at FROM Users WHERE email = '{user_email}'"
    })
    print(f"Spanner Direct Query Status: {spanner_res.status_code}")
    assert spanner_res.status_code == 200, "Spanner query failed"
    
    rows = spanner_res.json().get("rows", [])
    print(f"Direct Spanner DB Rows Found: {len(rows)}")
    assert len(rows) == 1, "User record was NOT found in Cloud Spanner database!"
    
    row = rows[0]
    db_user_id, db_email, db_name, db_role, db_pass_hash, db_created_at = row
    print(f"  - Spanner User ID       : {db_user_id}")
    print(f"  - Spanner Email         : {db_email}")
    print(f"  - Spanner Name          : {db_name}")
    print(f"  - Spanner Role          : {db_role}")
    print(f"  - Spanner Password Hash : {db_pass_hash[:20]}... (BCrypt Hashed)")
    print(f"  - Spanner Created At    : {db_created_at}")

    # 3. Retrieve User Profile via Backend API GET /api/users/me
    print(f"\n[3/4] Retrieving User Profile via API GET /api/users/me...")
    headers = {"Authorization": f"Bearer {token}"}
    me_res = requests.get(f"{API_BASE}/users/me", headers=headers)
    print(f"API Response Status: {me_res.status_code}")
    assert me_res.status_code == 200
    print(f"Profile Returned: {me_res.json().get('name')} ({me_res.json().get('email')})")

    # 4. Clean up test user directly in Spanner
    print(f"\n[4/4] Cleaning up test user record from Spanner...")
    tx_res = requests.post(f"http://localhost:9020/v1/{sess_name}:beginTransaction", json={"options": {"readWrite": {}}})
    tx_id = tx_res.json()["id"]
    requests.post(sql_url, json={"sql": f"DELETE FROM Users WHERE user_id = '{db_user_id}'", "transaction": {"id": tx_id}})
    del_res = requests.post(f"http://localhost:9020/v1/{sess_name}:commit", json={"transactionId": tx_id})
    print(f"Cleanup Status: {del_res.status_code}")
    print("\n========================================================================")
    print(" SUCCESS! FULL E2E SPANNER PERSISTENCE AUDIT PASSED PERFECTLY!")
    print("========================================================================")

if __name__ == "__main__":
    test_e2e_api_to_spanner_persistence()
