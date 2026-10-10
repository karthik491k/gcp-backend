# ============================================================================
# GlobalCart - Spanner Emulator & Local Environment Launch Script
# PowerShell Script for Docker Desktop, Cloud Spanner Emulator, Instance, Database & DDL Setup
# ============================================================================

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host " GlobalCart - Google Cloud Spanner Emulator Local Environment Setup" -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan

# 1. CHECK DOCKER DESKTOP STATUS
Write-Host ""
Write-Host "[1/5] Checking Docker Desktop status..." -ForegroundColor Yellow

$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Host "[ERROR] Docker CLI (docker.exe) is not found in system PATH." -ForegroundColor Red
    Write-Host "Please install Docker Desktop for Windows from: https://www.docker.com/products/docker-desktop/" -ForegroundColor Red
    exit 1
}

$dockerReady = $false
try {
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -eq 0) {
        $dockerReady = $true
        Write-Host "[OK] Docker Engine is running and responsive." -ForegroundColor Green
    }
} catch {
    $dockerReady = $false
}

if (-not $dockerReady) {
    Write-Host "[INFO] Docker Desktop is not running. Attempting to start Docker Desktop..." -ForegroundColor Yellow
    $dockerExe = "C:/Program Files/Docker/Docker/Docker Desktop.exe"
    if (Test-Path $dockerExe) {
        Start-Process $dockerExe
        Write-Host "[INFO] Waiting for Docker Desktop to start (up to 45 seconds)..." -ForegroundColor Yellow
        $timeoutSec = 45
        $elapsed = 0
        while ($elapsed -lt $timeoutSec) {
            Start-Sleep -Seconds 3
            $elapsed += 3
            try {
                docker info 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) {
                    $dockerReady = $true
                    Write-Host "[OK] Docker Desktop started successfully and is ready!" -ForegroundColor Green
                    break
                }
            } catch {}
            Write-Host "  ... waiting for Docker daemon: $elapsed of $timeoutSec sec" -ForegroundColor Gray
        }
    }

    if (-not $dockerReady) {
        Write-Host ""
        Write-Host "[MANUAL ACTION REQUIRED]" -ForegroundColor Red
        Write-Host "Docker Desktop could not be started automatically." -ForegroundColor Red
        Write-Host "Please open Docker Desktop manually from your Start Menu, wait until it finishes starting, and then re-run this script." -ForegroundColor Yellow
        exit 1
    }
}

# 2. START SPANNER EMULATOR CONTAINER
Write-Host ""
Write-Host "[2/5] Checking Spanner Emulator Docker container (ports 9010 and 9020)..." -ForegroundColor Yellow

$containerName = "spanner-emulator"
$containerFilter = docker ps -a --filter "name=^/spanner-emulator$" --format "{{.Names}} {{.Status}}" 2>&1

if ($containerFilter -match "spanner-emulator") {
    if ($containerFilter -match "Up") {
        Write-Host "[OK] Spanner Emulator container is already running." -ForegroundColor Green
    } else {
        Write-Host "[INFO] Starting existing Spanner Emulator container..." -ForegroundColor Yellow
        docker start $containerName | Out-Null
        Write-Host "[OK] Spanner Emulator container started." -ForegroundColor Green
    }
} else {
    Write-Host "[INFO] Pulling and running Cloud Spanner Emulator container..." -ForegroundColor Yellow
    docker run -d --name spanner-emulator -p 9010:9010 -p 9020:9020 gcr.io/cloud-spanner-emulator/emulator | Out-Null
    Write-Host "[OK] Spanner Emulator container created and started on ports 9010 (gRPC) and 9020 (REST)." -ForegroundColor Green
}

# 3. VERIFY EMULATOR REST API
Write-Host ""
Write-Host "[3/5] Verifying Spanner Emulator REST gateway availability on http://localhost:9020..." -ForegroundColor Yellow
$emulatorApiReady = $false
$retries = 15
for ($i = 1; $i -le $retries; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:9020/v1/projects/globalcart-dev-project/instances" -Method Get -ErrorAction Stop
        $emulatorApiReady = $true
        Write-Host "[OK] Cloud Spanner Emulator is ready and responding at http://localhost:9020!" -ForegroundColor Green
        break
    } catch {
        if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::NotFound -or $_.Exception.Response.StatusCode -eq 404) {
            $emulatorApiReady = $true
            Write-Host "[OK] Cloud Spanner Emulator REST endpoint is active." -ForegroundColor Green
            break
        }
        Start-Sleep -Seconds 1
        Write-Host "  ... waiting for Spanner emulator gateway: $i of $retries sec" -ForegroundColor Gray
    }
}

if (-not $emulatorApiReady) {
    Write-Host "[ERROR] Cloud Spanner Emulator REST gateway did not respond on port 9020." -ForegroundColor Red
    exit 1
}

# 4. CREATE SPANNER INSTANCE
Write-Host ""
Write-Host "[4/5] Checking / Creating Spanner Instance globalcart-spanner-instance..." -ForegroundColor Yellow
$instanceId = "globalcart-spanner-instance"
$projectId = "globalcart-dev-project"
$databaseId = "globalcart_db"

$instanceUri = "http://localhost:9020/v1/projects/$projectId/instances"
$instanceExist = $false

try {
    $instList = Invoke-RestMethod -Uri $instanceUri -Method Get -ErrorAction Stop
    if ($instList.instances -and ($instList.instances | Where-Object { $_.name -match $instanceId })) {
        $instanceExist = $true
        Write-Host "[OK] Spanner instance $instanceId already exists." -ForegroundColor Green
    }
} catch {}

if (-not $instanceExist) {
    Write-Host "[INFO] Creating Spanner instance $instanceId..." -ForegroundColor Yellow
    $instPayload = @{
        instanceId = $instanceId
        instance = @{
            config = "projects/$projectId/instanceConfigs/emulator-config"
            displayName = "Local Dev Spanner"
            nodeCount = 1
        }
    }
    $createInstBody = $instPayload | ConvertTo-Json -Depth 5

    try {
        Invoke-RestMethod -Uri $instanceUri -Method Post -ContentType "application/json" -Body $createInstBody | Out-Null
        Write-Host "[OK] Spanner instance $instanceId created successfully!" -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] Instance creation failed." -ForegroundColor Yellow
    }
}

# 5. CREATE SPANNER DATABASE & APPLY SCHEMA DDL
Write-Host ""
Write-Host "[5/5] Checking / Creating Spanner Database globalcart_db and applying DDL schema..." -ForegroundColor Yellow
$dbUri = "http://localhost:9020/v1/projects/$projectId/instances/$instanceId/databases"
$dbExist = $false

try {
    $dbList = Invoke-RestMethod -Uri $dbUri -Method Get -ErrorAction Stop
    if ($dbList.databases -and ($dbList.databases | Where-Object { $_.name -match $databaseId })) {
        $dbExist = $true
        Write-Host "[OK] Spanner database $databaseId already exists." -ForegroundColor Green
    }
} catch {}

if (-not $dbExist) {
    Write-Host "[INFO] Creating Spanner database $databaseId with DDL tables..." -ForegroundColor Yellow

    $dbPayload = @{
        createStatement = "CREATE DATABASE $databaseId"
        extraStatements = @(
            'CREATE TABLE Users (user_id STRING(64) NOT NULL, email STRING(255) NOT NULL, password_hash STRING(255) NOT NULL, name STRING(255) NOT NULL, phone STRING(32), avatar_url STRING(512), role STRING(32) NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (user_id)',
            'CREATE UNIQUE INDEX idx_users_email ON Users(email)',
            'CREATE TABLE UserAddresses (user_id STRING(64) NOT NULL, address_id STRING(64) NOT NULL, full_name STRING(255) NOT NULL, street STRING(512) NOT NULL, city STRING(128) NOT NULL, state STRING(128) NOT NULL, zip_code STRING(32) NOT NULL, country STRING(128) NOT NULL, phone STRING(32) NOT NULL, is_default BOOL NOT NULL, address_type STRING(32) NOT NULL) PRIMARY KEY (user_id, address_id), INTERLEAVE IN PARENT Users ON DELETE CASCADE',
            'CREATE TABLE Categories (category_id STRING(64) NOT NULL, name STRING(128) NOT NULL, slug STRING(128) NOT NULL, image_url STRING(512) NOT NULL, description STRING(1024), item_count INT64 NOT NULL) PRIMARY KEY (category_id)',
            'CREATE TABLE Brands (brand_id STRING(64) NOT NULL, name STRING(128) NOT NULL, logo_url STRING(512) NOT NULL, category STRING(128) NOT NULL) PRIMARY KEY (brand_id)',
            'CREATE TABLE Products (product_id STRING(64) NOT NULL, sku STRING(64) NOT NULL, name STRING(255) NOT NULL, brand STRING(128) NOT NULL, category STRING(128) NOT NULL, subcategory STRING(128) NOT NULL, description STRING(4096) NOT NULL, price NUMERIC NOT NULL, original_price NUMERIC NOT NULL, discount_percentage INT64 NOT NULL, rating FLOAT64 NOT NULL, review_count INT64 NOT NULL, stock_quantity INT64 NOT NULL, in_stock BOOL NOT NULL, hover_image_url STRING(512), is_new BOOL NOT NULL, is_trending BOOL NOT NULL, is_bestseller BOOL NOT NULL, is_flash_sale BOOL NOT NULL, flash_sale_end_time TIMESTAMP, specifications_json STRING(MAX), size_and_fit STRING(2048), shipping_info STRING(2048), return_policy STRING(2048), created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (product_id)',
            'CREATE INDEX idx_products_category ON Products(category)',
            'CREATE INDEX idx_products_brand ON Products(brand)',
            'CREATE UNIQUE INDEX idx_products_sku ON Products(sku)',
            'CREATE TABLE ProductColorOptions (product_id STRING(64) NOT NULL, color_name STRING(64) NOT NULL, color_hex STRING(32) NOT NULL, image_url STRING(512)) PRIMARY KEY (product_id, color_name), INTERLEAVE IN PARENT Products ON DELETE CASCADE',
            'CREATE TABLE ProductImages (product_id STRING(64) NOT NULL, image_order INT64 NOT NULL, image_url STRING(512) NOT NULL) PRIMARY KEY (product_id, image_order), INTERLEAVE IN PARENT Products ON DELETE CASCADE',
            'CREATE TABLE ProductSizes (product_id STRING(64) NOT NULL, size STRING(32) NOT NULL) PRIMARY KEY (product_id, size), INTERLEAVE IN PARENT Products ON DELETE CASCADE',
            'CREATE TABLE Carts (user_id STRING(64) NOT NULL, coupon_code STRING(64), updated_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (user_id)',
            'CREATE TABLE CartItems (user_id STRING(64) NOT NULL, item_id STRING(64) NOT NULL, product_id STRING(64) NOT NULL, color STRING(64) NOT NULL, size STRING(32) NOT NULL, quantity INT64 NOT NULL, unit_price NUMERIC NOT NULL) PRIMARY KEY (user_id, item_id), INTERLEAVE IN PARENT Carts ON DELETE CASCADE',
            'CREATE TABLE WishlistItems (user_id STRING(64) NOT NULL, product_id STRING(64) NOT NULL, added_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true)) PRIMARY KEY (user_id, product_id), INTERLEAVE IN PARENT Users ON DELETE CASCADE',
            'CREATE TABLE Orders (order_id STRING(64) NOT NULL, order_number STRING(64) NOT NULL, user_id STRING(64) NOT NULL, shipping_address_json STRING(MAX) NOT NULL, payment_method STRING(32) NOT NULL, payment_status STRING(32) NOT NULL, order_status STRING(64) NOT NULL, subtotal NUMERIC NOT NULL, discount NUMERIC NOT NULL, shipping_fee NUMERIC NOT NULL, tax NUMERIC NOT NULL, total_amount NUMERIC NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true), estimated_delivery STRING(128) NOT NULL, tracking_number STRING(128)) PRIMARY KEY (order_id)',
            'CREATE INDEX idx_orders_user_id ON Orders(user_id)',
            'CREATE UNIQUE INDEX idx_orders_order_number ON Orders(order_number)',
            'CREATE TABLE OrderItems (order_id STRING(64) NOT NULL, item_id STRING(64) NOT NULL, product_id STRING(64) NOT NULL, product_name STRING(255) NOT NULL, product_image STRING(512) NOT NULL, brand STRING(128) NOT NULL, color STRING(64) NOT NULL, size STRING(32) NOT NULL, quantity INT64 NOT NULL, unit_price NUMERIC NOT NULL, total_price NUMERIC NOT NULL) PRIMARY KEY (order_id, item_id), INTERLEAVE IN PARENT Orders ON DELETE CASCADE',
            'CREATE TABLE OrderTimeline (order_id STRING(64) NOT NULL, status STRING(64) NOT NULL, timestamp_str STRING(128), completed BOOL NOT NULL, description STRING(512) NOT NULL) PRIMARY KEY (order_id, status), INTERLEAVE IN PARENT Orders ON DELETE CASCADE',
            'CREATE TABLE Reviews (product_id STRING(64) NOT NULL, review_id STRING(64) NOT NULL, user_id STRING(64) NOT NULL, user_name STRING(255) NOT NULL, user_avatar STRING(512), rating INT64 NOT NULL, title STRING(255) NOT NULL, comment STRING(2048) NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true), verified_purchase BOOL NOT NULL, helpful_count INT64 NOT NULL) PRIMARY KEY (product_id, review_id), INTERLEAVE IN PARENT Products ON DELETE CASCADE',
            'CREATE TABLE Coupons (coupon_id STRING(64) NOT NULL, code STRING(64) NOT NULL, discount_percentage INT64 NOT NULL, max_discount NUMERIC NOT NULL, min_order_amount NUMERIC NOT NULL, expiry_date STRING(32) NOT NULL, usage_limit INT64 NOT NULL, usage_count INT64 NOT NULL, is_active BOOL NOT NULL) PRIMARY KEY (coupon_id)',
            'CREATE UNIQUE INDEX idx_coupons_code ON Coupons(code)',
            'CREATE TABLE Inventory (product_id STRING(64) NOT NULL, inventory_id STRING(64) NOT NULL, product_name STRING(255) NOT NULL, sku STRING(64) NOT NULL, category STRING(128) NOT NULL, available_stock INT64 NOT NULL, reserved_stock INT64 NOT NULL, total_sold INT64 NOT NULL, status STRING(32) NOT NULL) PRIMARY KEY (product_id)',
            'CREATE TABLE Notifications (user_id STRING(64) NOT NULL, notification_id STRING(64) NOT NULL, title STRING(255) NOT NULL, message STRING(1024) NOT NULL, type STRING(32) NOT NULL, created_at TIMESTAMP NOT NULL OPTIONS (allow_commit_timestamp=true), read BOOL NOT NULL) PRIMARY KEY (user_id, notification_id), INTERLEAVE IN PARENT Users ON DELETE CASCADE'
        )
    }
    $createDbBody = $dbPayload | ConvertTo-Json -Depth 10

    try {
        Invoke-RestMethod -Uri $dbUri -Method Post -ContentType "application/json" -Body $createDbBody | Out-Null
        Write-Host "[OK] Spanner database $databaseId and schema DDL created successfully!" -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] Database creation failed." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host " SETUP COMPLETE! CLOUD SPANNER EMULATOR IS READY AND INITIALIZED" -ForegroundColor Green
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "Project ID  : globalcart-dev-project" -ForegroundColor White
Write-Host "Instance ID : globalcart-spanner-instance" -ForegroundColor White
Write-Host "Database ID : globalcart_db" -ForegroundColor White
Write-Host "gRPC Host   : localhost:9010" -ForegroundColor White
Write-Host "REST Host   : http://localhost:9020" -ForegroundColor White
Write-Host ""
Write-Host "Next Step: Run the Spring Boot Backend" -ForegroundColor Yellow
Write-Host 'Command     : cd C:/Users/karth/.gemini/antigravity/scratch/logancee-backend' -ForegroundColor White
Write-Host '              powershell -ExecutionPolicy Bypass -File ./start-backend.ps1' -ForegroundColor White
Write-Host '   or in STS: Right-click globalcart-backend -> Run As -> Spring Boot App' -ForegroundColor White
Write-Host "========================================================================" -ForegroundColor Cyan
