#!/bin/sh
set -e

echo "========================================================================"
echo " Starting LOGANCEE Backend + Google Cloud Spanner Emulator Container"
echo "========================================================================"

# Default configuration values
export PORT="${PORT:-10000}"
export SERVER_ADDRESS="${SERVER_ADDRESS:-0.0.0.0}"
export GCP_PROJECT_ID="${GCP_PROJECT_ID:-globalcart-dev-project}"
export SPANNER_INSTANCE_ID="${SPANNER_INSTANCE_ID:-globalcart-spanner-instance}"
export SPANNER_DATABASE_ID="${SPANNER_DATABASE_ID:-globalcart_db}"
export SPANNER_EMULATOR_HOST="${SPANNER_EMULATOR_HOST:-localhost:9010}"
export SPANNER_REST_URL="${SPANNER_REST_URL:-http://127.0.0.1:9020/v1}"
export USE_ANONYMOUS_CREDENTIALS="${USE_ANONYMOUS_CREDENTIALS:-true}"
export JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx1024m}"

echo "[CONFIG] Port                 : $PORT"
echo "[CONFIG] Server Address       : $SERVER_ADDRESS"
echo "[CONFIG] GCP Project ID       : $GCP_PROJECT_ID"
echo "[CONFIG] Spanner Instance ID  : $SPANNER_INSTANCE_ID"
echo "[CONFIG] Spanner Database ID  : $SPANNER_DATABASE_ID"
echo "[CONFIG] Spanner gRPC Host    : $SPANNER_EMULATOR_HOST"
echo "[CONFIG] Spanner REST URL     : $SPANNER_REST_URL"

# Cleanup trap for graceful shutdown
cleanup() {
    echo "[ENTRYPOINT] Caught shutdown signal. Stopping services..."
    if [ -n "$APP_PID" ]; then
        kill -TERM "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
    fi
    if [ -n "$EMULATOR_PID" ]; then
        kill -TERM "$EMULATOR_PID" 2>/dev/null || true
        wait "$EMULATOR_PID" 2>/dev/null || true
    fi
    echo "[ENTRYPOINT] All processes terminated."
    exit 0
}
trap cleanup TERM INT

# 1. Start Google Cloud Spanner Emulator
echo ""
echo "[STEP 1/3] Launching Cloud Spanner Emulator daemon (gateway_main)..."
/usr/local/bin/gateway_main --hostname 127.0.0.1 &
EMULATOR_PID=$!

# 2. Initialize Spanner instance, database and schema
echo ""
echo "[STEP 2/3] Initializing Spanner Instance, Database & Schema DDL..."
python3 /app/init_spanner.py

# 3. Start Spring Boot Application
echo ""
echo "[STEP 3/3] Starting Spring Boot Backend application on port $PORT..."
java $JAVA_OPTS -Dserver.port="$PORT" -Dserver.address="$SERVER_ADDRESS" -jar /app/app.jar &
APP_PID=$!

# Wait for application process
wait "$APP_PID"
