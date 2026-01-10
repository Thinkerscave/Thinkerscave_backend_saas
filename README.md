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

### 2. Configure Database

Create a PostgreSQL database (e.g., `thinkerscave_saas`) and update your `application.yml` or `application.properties`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/thinkerscave_saas
    username: your_db_username
    password: your_db_password
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

---

## 📄 View All APIs

Once the application is running, go to:

```
http://localhost:8181/swagger-ui.html
```
