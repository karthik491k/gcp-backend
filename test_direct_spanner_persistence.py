import requests
import json
import time

BASE_URL = "http://localhost:9020/v1/projects/globalcart-dev-project/instances/globalcart-spanner-instance/databases/globalcart_db"

def test_direct_spanner_write():
    print("========================================================================")
    print(" TESTING DIRECT SPANNER DATABASE PERSISTENCE WITH READ-WRITE TRANSACTION")
    print("========================================================================")
    
    # 1. Create Session
    session_res = requests.post(f"{BASE_URL}/sessions")
    session_name = session_res.json()["name"]
    
    test_user_id = f"test-user-{int(time.time())}"
    test_email = f"audit_test_{int(time.time())}@logancee.fashion"
    
    print(f"Creating test user record directly in Spanner DB table 'Users':")
    print(f"  ID: {test_user_id}")
    print(f"  Email: {test_email}")

    # 2. Begin Read-Write Transaction
    tx_res = requests.post(f"http://localhost:9020/v1/{session_name}:beginTransaction", json={"options": {"readWrite": {}}})
    print("\nBegin Transaction Status:", tx_res.status_code)
    if tx_res.status_code != 200:
        print("Failed to begin transaction:", tx_res.text)
        return
        
    tx_id = tx_res.json()["id"]
    print("Transaction ID:", tx_id)

    # 3. Execute DML in Transaction
    dml_sql = f"""INSERT INTO Users (user_id, email, password_hash, name, phone, avatar_url, role, created_at) VALUES ('{test_user_id}', '{test_email}', 'REDACTED_HASH', 'Audit Test User', '+1-555-0199', 'https://example.com/avatar.jpg', 'CUSTOMER', CURRENT_TIMESTAMP())"""
    sql_url = f"http://localhost:9020/v1/{session_name}:executeSql"
    
    dml_res = requests.post(sql_url, json={
        "sql": dml_sql,
        "transaction": {"id": tx_id}
    })
    print("\nExecute DML Status Code:", dml_res.status_code)
    
    # 4. Commit Transaction
    commit_res = requests.post(f"http://localhost:9020/v1/{session_name}:commit", json={"transactionId": tx_id})
    print("Commit Status Code:", commit_res.status_code)
    if commit_res.status_code == 200:
        print("Commit Result:", json.dumps(commit_res.json(), indent=2))
        
        # 5. Query back directly from Spanner
        query_sql = f"SELECT user_id, email, name, role, created_at FROM Users WHERE user_id = '{test_user_id}'"
        select_res = requests.post(sql_url, json={"sql": query_sql})
        print("\nQuerying Spanner directly for inserted record:")
        print("Select Status Code:", select_res.status_code)
        if select_res.status_code == 200:
            rows = select_res.json().get("rows", [])
            print("Direct Spanner DB Rows Found:", len(rows))
            for r in rows:
                print("  [CONFIRMED PERSISTED IN SPANNER DB]:", r)
                
        # 6. Clean up test user
        tx_res2 = requests.post(f"http://localhost:9020/v1/{session_name}:beginTransaction", json={"options": {"readWrite": {}}})
        tx_id2 = tx_res2.json()["id"]
        requests.post(sql_url, json={"sql": f"DELETE FROM Users WHERE user_id = '{test_user_id}'", "transaction": {"id": tx_id2}})
        del_commit = requests.post(f"http://localhost:9020/v1/{session_name}:commit", json={"transactionId": tx_id2})
        print(f"\nCleaned up test user from Spanner DB (Delete commit status: {del_commit.status_code})")

if __name__ == "__main__":
    test_direct_spanner_write()
