import requests
import json

BASE_URL = "http://localhost:9020/v1/projects/globalcart-dev-project/instances/globalcart-spanner-instance/databases/globalcart_db"

def run_spanner_audit():
    print("========================================================================")
    print(" CLOUD SPANNER EMULATOR SCHEMA & PERSISTENCE AUDIT")
    print("========================================================================")
    
    # 1. Create Session
    session_res = requests.post(f"{BASE_URL}/sessions")
    if session_res.status_code != 200:
        print("[ERROR] Failed to connect to Spanner Emulator session:", session_res.text)
        return
    
    session_name = session_res.json()["name"]
    sql_url = f"http://localhost:9020/v1/{session_name}:executeSql"
    print(f"[OK] Connected to Spanner Session: {session_name}\n")

    # 2. List Tables
    res = requests.post(sql_url, json={"sql": "SELECT table_name FROM information_schema.tables WHERE table_schema = ''"})
    tables = [row[0] for row in res.json().get("rows", [])]
    print(f"Total Spanner DDL Tables Found: {len(tables)}")
    for i, t in enumerate(tables, 1):
        print(f"  {i}. {t}")

    print("\n------------------------------------------------------------------------")
    print(" TABLE SCHEMAS, ROW COUNTS & SAMPLE RECORDS (EXCLUDING SECRETS)")
    print("------------------------------------------------------------------------")
    
    sensitive_fields = {"password_hash", "password", "jwt", "secret", "token"}

    for t in sorted(tables):
        # Query column info
        cols_res = requests.post(sql_url, json={"sql": f"SELECT column_name, spanner_type FROM information_schema.columns WHERE table_name = '{t}'"})
        cols_data = cols_res.json().get("rows", [])
        col_list = [c[0] for c in cols_data]

        # Query row count
        count_res = requests.post(sql_url, json={"sql": f"SELECT COUNT(*) FROM `{t}`"})
        row_count = 0
        if count_res.status_code == 200 and "rows" in count_res.json():
            row_count = int(count_res.json()["rows"][0][0])
            
        print(f"\n[TABLE] {t}")
        print(f"  Columns: {', '.join([f'{c[0]} ({c[1]})' for c in cols_data])}")
        print(f"  Direct Spanner Row Count: {row_count}")

        if row_count > 0:
            safe_cols = [c for c in col_list if c.lower() not in sensitive_fields]
            cols_sql = ", ".join([f"`{c}`" for c in safe_cols])
            sample_res = requests.post(sql_url, json={"sql": f"SELECT {cols_sql} FROM `{t}` LIMIT 2"})
            if sample_res.status_code == 200 and "rows" in sample_res.json():
                print("  Safe Sample Records:")
                for r in sample_res.json()["rows"]:
                    rec = dict(zip(safe_cols, r))
                    print(f"    - {rec}")

if __name__ == "__main__":
    run_spanner_audit()
