# LOGANCEE (GlobalCart) - Unified Spring Boot & Cloud Spanner Deployment

A self-contained, production-ready Docker deployment container for the **LOGANCEE (GlobalCart)** e-commerce backend. It packages **Spring Boot 3.3.4 (Java 21)** and the official **Google Cloud Spanner Emulator** into a single container image.

---

## 🏗️ Architecture Overview

```
+-------------------------------------------------------------------------+
| Docker Container (Linux / Java 21)                                      |
|                                                                         |
|  [Entrypoint: entrypoint.sh]                                            |
|       |                                                                 |
|       +--> 1. Starts Google Cloud Spanner Emulator (gateway_main)       |
|       |    - gRPC: 127.0.0.1:9010 (internal only)                      |
|       |    - REST: 127.0.0.1:9020 (internal only)                      |
|       |                                                                 |
|       +--> 2. Initializes Spanner Instance & Database (init_spanner.py) |
|       |    - Instance: globalcart-spanner-instance                     |
|       |    - Database: globalcart_db                                    |
|       |    - Creates 18 tables & indexes                                |
|       |                                                                 |
|       +--> 3. Starts Spring Boot Application (globalcart-backend)       |
|       |    - Connects to emulator at localhost:9010                     |
|       |    - Automatically seeds 114 products, categories, brands, users |
|       |    - Listens on 0.0.0.0:${PORT} (default: 10000)                |
+-------------------------------------------------------------------------+
                                    |
                          EXPOSED PORT: ${PORT:-10000}
                                    |
                            [Client / Frontend]
```

### Key Security & Isolation Features:
- **Zero Public Emulator Exposure**: Cloud Spanner Emulator ports `9010` (gRPC) and `9020` (REST) bind strictly to `127.0.0.1` within the container. Only the Spring Boot HTTP port (`PORT`, default `10000`) is exposed.
- **Preserved Security**: Full Spring Security filter chain, stateless JWT authentication, and BCrypt password encryption remain active.
- **Configurable Secrets & CORS**: Secrets and origins can be injected via standard environment variables without rebuilding the image.

---

## 🚀 Quick Start

### 1. Build the Docker Image

From `D:\LOGANCEE-Backend-Deploy`:
```bash
docker build -t logancee-backend:latest .
```

### 2. Run the Container

Run mapping the default container port `10000` to host port `10000`:
```bash
docker run -d \
  --name logancee-backend \
  -p 10000:10000 \
  logancee-backend:latest
```

Or map to host port `8080` (e.g. for existing frontend configurations):
```bash
docker run -d \
  --name logancee-backend \
  -p 8080:10000 \
  -e PORT=10000 \
  logancee-backend:latest
```

### 3. Verify Container Health & Data

Inspect logs:
```bash
docker logs -f logancee-backend
```

Test the Products API:
```bash
curl http://localhost:10000/api/products
```

Test Categories:
```bash
curl http://localhost:10000/api/categories
```

---

## ⚙️ Environment Variables Reference

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `10000` | HTTP port on which Spring Boot listens inside the container. |
| `SERVER_ADDRESS` | `0.0.0.0` | Network interface binding for the Spring Boot embedded Tomcat. |
| `GCP_PROJECT_ID` | `globalcart-dev-project` | Google Cloud project identifier for Spanner. |
| `SPANNER_INSTANCE_ID` | `globalcart-spanner-instance` | Cloud Spanner instance ID. |
| `SPANNER_DATABASE_ID` | `globalcart_db` | Cloud Spanner database ID. |
| `SPANNER_EMULATOR_HOST` | `localhost:9010` | Spanner emulator gRPC endpoint address. |
| `SPANNER_REST_URL` | `http://127.0.0.1:9020/v1` | Spanner emulator REST management endpoint. |
| `USE_ANONYMOUS_CREDENTIALS` | `true` | Allows unauthenticated client access to local Spanner emulator. |
| `JWT_SECRET` | *(64-byte hex default)* | Secret key for signing HS512 JWT authentication tokens. **Override in production!** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | JWT access token validity period in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,...` | Comma-delimited list of origins permitted to call the backend APIs. |
| `JAVA_OPTS` | `-Xms256m -Xmx1024m` | JVM memory parameters and garbage collection options. |

---

## 🛡️ Database Schema & Seed Data

On first boot:
1. `init_spanner.py` creates the `globalcart-spanner-instance` instance and `globalcart_db` database in the emulator.
2. The schema creates 18 tables:
   - `Users`, `UserAddresses`
   - `Categories`, `Brands`
   - `Products`, `ProductColorOptions`, `ProductImages`, `ProductSizes`
   - `Carts`, `CartItems`
   - `WishlistItems`
   - `Orders`, `OrderItems`, `OrderTimeline`
   - `Reviews`, `Coupons`, `Inventory`, `Notifications`
3. Spring Boot's `SpannerDataService` executes on startup:
   - Populates **114 products** across 6 categories (`Women`, `Men`, `Kids`, `Beauty & Self Care`, `Electronics`, `Home & Lifestyle`).
   - Populates 10 luxury brands, 3 default users (Admin, Seller, Customer), and active coupons (`WELCOME10`, `SAVE20`, `FLASH50`).

---

## 🧪 Testing and Validation

### Pre-configured Test Accounts
- **Customer**: `john.doe@example.com` / `password123`
- **Admin**: `admin@globalcart.com` / `admin123`
- **Seller**: `seller@luxurybrands.com` / `seller123`

### Sample API Requests

**1. Customer Login:**
```bash
curl -X POST http://localhost:10000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","password":"password123"}'
```

**2. Fetch Paginated Products:**
```bash
curl "http://localhost:10000/api/products?page=0&limit=10&sortBy=recommended"
```

**3. Filter by Category:**
```bash
curl "http://localhost:10000/api/products?category=Women&limit=8"
```

---

## ☁️ Cloud Deployment Options

### Google Cloud Run
Deploying to Cloud Run with 2GB RAM and 1 vCPU:
```bash
gcloud run deploy logancee-backend \
  --image gcr.io/[PROJECT_ID]/logancee-backend:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 10000 \
  --memory 2Gi \
  --set-env-vars "JWT_SECRET=[YOUR_PRODUCTION_SECRET]"
```

### Docker Compose
Create `docker-compose.yml`:
```yaml
services:
  backend:
    image: logancee-backend:latest
    build: .
    ports:
      - "10000:10000"
    environment:
      - PORT=10000
      - JWT_SECRET=YOUR_SECURE_JWT_SECRET_KEY_HERE_MIN_64_CHARS
      - CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com,http://localhost:5173
    restart: unless-stopped
```
Run with:
```bash
docker compose up -d
```
