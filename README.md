# 🧠 ThinkersCave SaaS Backend

This is the backend system for **ThinkersCave SaaS**, a multi-tenant application built using Spring Boot with schema-based multi-tenancy in PostgreSQL.

It provides modules for:
- Organization Management (register organizations and create isolated schemas)
- User and Role Management
- Tenant-specific data operations
- Upcoming modules like Staff and Roll Management

---

## ✅ Requirements

- Java 21
- Maven 3.8+
- PostgreSQL 15 or higher (recommended: PostgreSQL 17)
- IDE like IntelliJ / VS Code (optional)

---

## ⚙️ How to Set Up

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/thinkerscave_backend_saas.git
cd thinkerscave_backend_saas
````

### 2. Configure Environment Variables

**IMPORTANT**: This application uses environment variables for sensitive configuration. Never commit credentials to version control!

#### Step 1: Create `.env` file
```bash
cp .env.example .env
```

#### Step 2: Edit `.env` with your actual values

```bash
# Database Configuration
DB_URL=jdbc:postgresql://your-db-host:5432/your_database
DB_USERNAME=your_db_username
DB_PASSWORD=your_secure_password

# JWT Configuration (generate using: openssl rand -base64 64)
JWT_SECRET=your_generated_jwt_secret_key
JWT_EXPIRATION=900000

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password

# CORS Configuration
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200,https://yourdomain.com

# Server Configuration
SERVER_PORT=8181
```

#### Step 3: Generate JWT Secret
```bash
# On macOS/Linux
openssl rand -base64 64

# Or use the application endpoint
curl http://localhost:8181/api/v1/users/generateKey
```

#### Step 4: Gmail App Password Setup
For Gmail, you need to create an App Password:
1. Go to https://myaccount.google.com/security
2. Enable 2-Step Verification
3. Go to App Passwords
4. Generate a new app password for "Mail"
5. Use this password in `MAIL_PASSWORD`

---

### 3. Database Setup (manual — required)

The application does **not** create schema, run Flyway, or seed data on startup
(`ddl-auto=none`, Flyway off, no SQL init).

1. Create / migrate the PostgreSQL database yourself (tables must already exist).
2. Apply platform master data manually when needed:

```bash
psql -h <host> -U <user> -d <database> -v ON_ERROR_STOP=1 \
  -f scripts/postgres/01_platform_master_seed.sql
```

See `scripts/postgres/README.md`. Historical schema scripts live under
`scripts/postgres/archive/` (reference only).

`dev` / `test` / `prod` profiles currently share the same deploy-space defaults;
override with `SPRING_DATASOURCE_*` when environments split.

---

### 4. Run the Application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

---

## 📄 View All APIs

Once the application is running, go to:

```
http://localhost:8181/swagger-ui.html
```
