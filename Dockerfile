# ==============================================================================
# LOGANCEE (GlobalCart) - Unified Spring Boot + Cloud Spanner Emulator Container
# ==============================================================================

# STAGE 1: Build Spring Boot Application JAR
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy Maven POM and pre-fetch dependencies for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy application source code and compile
COPY src ./src
RUN mvn clean package -DskipTests -B

# STAGE 2: Lightweight Runtime Image with Java 21 + Cloud Spanner Emulator
FROM eclipse-temurin:21-jre

# Install Python3, curl, and certificates for emulator automation and health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copy Cloud Spanner Emulator binaries from official Google container image
COPY --from=gcr.io/cloud-spanner-emulator/emulator:latest /gateway_main /emulator_main /usr/local/bin/

WORKDIR /app

# Copy built application JAR and initialization scripts
COPY --from=builder /workspace/target/globalcart-backend-1.0.0.jar /app/app.jar
COPY init_spanner.py /app/init_spanner.py
COPY entrypoint.sh /app/entrypoint.sh

# Ensure executable permissions for binaries and entrypoint script
RUN chmod +x /app/entrypoint.sh /usr/local/bin/gateway_main /usr/local/bin/emulator_main

# Default runtime environment variables
ENV PORT=10000 \
    SERVER_ADDRESS=0.0.0.0 \
    GCP_PROJECT_ID=globalcart-dev-project \
    SPANNER_INSTANCE_ID=globalcart-spanner-instance \
    SPANNER_DATABASE_ID=globalcart_db \
    SPANNER_EMULATOR_HOST=localhost:9010 \
    SPANNER_REST_URL=http://127.0.0.1:9020/v1 \
    USE_ANONYMOUS_CREDENTIALS=true \
    JAVA_OPTS="-Xms256m -Xmx1024m"

# Expose ONLY the Spring Boot application HTTP port (never 9010 or 9020)
EXPOSE 10000

# Set entrypoint
ENTRYPOINT ["/app/entrypoint.sh"]
