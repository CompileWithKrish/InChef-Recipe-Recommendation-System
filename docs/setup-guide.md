# InChef — Local Development & Setup Guide

This guide covers getting the InChef application running on your local machine.

---

## 1. System Requirements

- **Java Development Kit (JDK):** Version 17 LTS (Eclipse Temurin, Oracle JDK, or OpenJDK).
- **Maven:** Version 3.9+ (pre-bundled via `./mvnw` on Linux/macOS and `.\mvnw.cmd` on Windows).
- **Database:** MySQL 8.0+ (or Docker for containerized MySQL).
- **Python (Optional for dataset tools):** Version 3.9+ with `pandas`, `openpyxl`, and `pymysql`.

---

## 2. Step-by-Step Setup

### Step 1: Clone Repository & Configure Environment

```bash
git clone https://github.com/your-username/inchef.git
cd inchef
cp .env.example .env
```

Edit `.env` to match your local MySQL credentials:

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=inchef_db
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=inchef_super_secret_jwt_key_at_least_256_bits_long_for_security_compliance
```

### Step 2: Start MySQL Database

#### Option A: Docker Compose (Recommended)
```bash
docker compose up -d
```
This launches a standardized MySQL 8 container with an initialized `inchef_db` database on port 3306.

#### Option B: Local Native MySQL
Ensure your local MySQL service is running, then create the database if not present:
```sql
CREATE DATABASE IF NOT EXISTS inchef_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 3: Run Database Migrations (Optional / Automated)
Spring Boot will automatically execute Flyway or Hibernate schema generation. If applying migrations manually, run:
```bash
mysql -u root -p inchef_db < data/migrations/V2__add_recipe_image_metadata.sql
mysql -u root -p inchef_db < data/migrations/V3__add_login_history.sql
```

### Step 4: Import Sample or Full Dataset

To seed your database with verified sample recipes:
```bash
python scripts/import_dataset.py --dataset data/samples/sample_recipes.xlsx
```

To import the complete 6,000+ recipe dataset (if available):
```bash
python scripts/import_dataset.py --dataset "/path/to/IndianFoodDatasetXLS.xlsx"
```

### Step 5: Build and Run Backend

Using the pre-configured Maven wrapper:

**On Windows (PowerShell / Command Prompt):**
```powershell
.\mvnw.cmd spring-boot:run
```

**On Linux / macOS:**
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### Step 6: Access the Application

Open your browser and navigate to:
- **Web Application:** `http://localhost:8080/index.html`
- **Discover Recipes:** `http://localhost:8080/discover.html`
- **Recommendations:** `http://localhost:8080/recommendations.html`
- **Login / Signup:** `http://localhost:8080/login.html`
- **SOAP Reports WSDL:** `http://localhost:8080/ws/reports.wsdl`

---

## 3. Running Automated Tests

Run the full automated test suite using the isolated H2 profile:

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```
All 145 tests run against an in-memory database without affecting your local MySQL data.

