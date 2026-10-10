import requests
import json

BASE_URL = "http://localhost:9020/v1/projects/globalcart-dev-project/instances/globalcart-spanner-instance/databases/globalcart_db"

def test_spanner():
    # 1. Create Session
    session_res = requests.post(f"{BASE_URL}/sessions")
    print("Session Response Status:", session_res.status_code)
    if session_res.status_code != 200:
        print("Failed to create session:", session_res.text)
        return
    
    session_data = session_res.json()
    session_name = session_data["name"]
    print("Spanner Session Created:", session_name)
    
    # 2. Execute SQL - List Tables
    sql_url = f"http://localhost:9020/v1/{session_name}:executeSql"
    tables_res = requests.post(sql_url, json={"sql": "SELECT table_name FROM information_schema.tables WHERE table_schema = ''"})
    print("\nExecute SQL (List Tables) Status:", tables_res.status_code)
    if tables_res.status_code == 200:
        res_json = tables_res.json()
        print("Tables JSON:", json.dumps(res_json, indent=2))

if __name__ == "__main__":
    test_spanner()
