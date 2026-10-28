# InChef — Production Deployment & Operations Guide

This guide details packaging, production runtime configuration, Docker containerization, reverse proxying, and operational monitoring for InChef.

---

## 1. Production Build & Packaging

Build a self-contained, executable JAR file excluding test executions:

```bash
# On Linux/macOS
./mvnw clean package -DskipTests

# On Windows
.\mvnw.cmd clean package -DskipTests
```

The compiled artifact will be created at: `target/inchef-backend-1.0.0.jar`.

---

## 2. Running as a Standalone Service

Run the JAR using production JVM arguments:

```bash
java -server \
     -Xms1024m -Xmx2048m \
     -XX:+UseG1GC \
     -Dspring.profiles.active=mysql \
     -Dserver.port=8080 \
     -jar target/inchef-backend-1.0.0.jar
```

Ensure environment variables are loaded securely:
- `DB_HOST`: Hostname of MySQL production cluster.
- `DB_USERNAME`: Dedicated application database user.
- `DB_PASSWORD`: Strong password from secrets vault.
- `JWT_SECRET`: Minimum 256-bit cryptographic key.
- `ADMIN_SECRET`: Secret header key for image audit endpoints.

---

## 3. Docker Deployment

### Multi-Stage Dockerfile

```dockerfile
# Stage 1: Build application
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN addgroup --system inchef && adduser --system --ingroup inchef inchef
USER inchef
COPY --from=builder /app/target/inchef-backend-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]
```

---

## 4. Nginx Reverse Proxy & SSL Termination

Sample Nginx configuration for HTTPS termination, static asset caching, and API proxying:

```nginx
server {
    listen 80;
    server_name inchef.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name inchef.example.com;

    ssl_certificate /etc/letsencrypt/live/inchef.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/inchef.example.com/privkey.pem;

    # Static assets caching
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|svg)$ {
        proxy_pass http://127.0.0.1:8080;
        expires 7d;
        add_header Cache-Control "public, no-transform";
    }

    # API and dynamic endpoints
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 5. Health Checks & Monitoring

The application exposes standard Spring Boot health endpoints:
- **Liveness & Readiness:** `GET /actuator/health`
- **Application Metrics:** `GET /actuator/metrics`
- **Application Info:** `GET /actuator/info`

