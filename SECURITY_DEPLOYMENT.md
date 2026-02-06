# 🔒 Security & Deployment Guide

## Security Best Practices

### ✅ Implemented Security Measures

This application has been hardened with the following security measures:

#### 1. **Environment Variable Configuration**
- ✅ All sensitive credentials moved to environment variables
- ✅ No hardcoded passwords, API keys, or secrets in source code
- ✅ `.env` file added to `.gitignore`
- ✅ `.env.example` template provided for reference

#### 2. **CORS Configuration**
- ✅ Removed wildcard `@CrossOrigin("*")` from all controllers
- ✅ Centralized CORS configuration in `SecurityConfig.java`
- ✅ Environment-based allowed origins
- ✅ Credentials support enabled for authenticated requests

#### 3. **Logging**
- ✅ Log4j2 configured with separate log files for:
  - Application logs (`application.log`)
  - Error logs (`error.log`)
  - Security logs (`security.log`)
- ✅ Log rotation enabled (10MB per file, max 10 files)
- ✅ Sensitive data not logged

#### 4. **JWT Authentication**
- ✅ Secure JWT secret from environment variable
- ✅ Configurable token expiration
- ✅ Refresh token mechanism implemented
- ✅ Login attempt tracking and account locking

---

## 🚀 Deployment Checklist

### Pre-Deployment

- [ ] **Java Version**: Ensure Java 21 is installed on production server
  ```bash
  java -version  # Should show Java 21
  ```

- [ ] **Environment Variables**: Set all required environment variables
  ```bash
  # Verify all variables are set
  echo $DB_URL
  echo $JWT_SECRET
  echo $MAIL_USERNAME
  echo $ALLOWED_ORIGINS
  ```

- [ ] **Database**: PostgreSQL database created and accessible
  ```sql
  -- Test connection
  psql -h your-db-host -U your-username -d your-database
  ```

- [ ] **CORS Origins**: Update `ALLOWED_ORIGINS` with production domain
  ```bash
  export ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
  ```

- [ ] **JWT Secret**: Generate a strong, unique JWT secret
  ```bash
  openssl rand -base64 64
  ```

### Production Configuration

#### 1. **Application Properties**

For production, consider these additional settings in `application.properties`:

```properties
# Production Database Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# Hibernate Settings
spring.jpa.hibernate.ddl-auto=validate  # NEVER use 'update' in production!
spring.jpa.show-sql=false  # Disable SQL logging in production

# Logging Level
logging.level.root=WARN
logging.level.com.thinkerscave=INFO

# Server Configuration
server.port=${SERVER_PORT:8181}
server.compression.enabled=true
server.http2.enabled=true
```

#### 2. **Database Migration**

For production, use Flyway or Liquibase instead of `ddl-auto=update`:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

#### 3. **SSL/TLS Configuration**

Enable HTTPS in production:

```properties
# application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

Generate keystore:
```bash
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

---

## 🐳 Docker Deployment

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port
EXPOSE 8181

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8181/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    ports:
      - "${SERVER_PORT}:8181"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION}
      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      ALLOWED_ORIGINS: ${ALLOWED_ORIGINS}
      SERVER_PORT: 8181
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres_data:
```

Run with:
```bash
docker-compose up -d
```

---

## 🔐 Environment Variable Management

### Development
Use `.env` file (already configured):
```bash
cp .env.example .env
# Edit .env with your values
```

### Production

#### Option 1: System Environment Variables
```bash
# /etc/environment or ~/.bashrc
export DB_URL="jdbc:postgresql://prod-db:5432/thinkerscave"
export JWT_SECRET="your-production-secret"
# ... other variables
```

#### Option 2: Docker Secrets (Recommended for Docker Swarm)
```yaml
services:
  app:
    secrets:
      - db_password
      - jwt_secret
    environment:
      DB_PASSWORD_FILE: /run/secrets/db_password
      JWT_SECRET_FILE: /run/secrets/jwt_secret

secrets:
  db_password:
    external: true
  jwt_secret:
    external: true
```

#### Option 3: Kubernetes Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
stringData:
  DB_PASSWORD: your-password
  JWT_SECRET: your-jwt-secret
```

---

## 📊 Monitoring & Logging

### Log Files Location
```
./Logs/
├── application.log       # All application logs
├── error.log            # Error logs only
└── security.log         # Authentication/security logs
```

### Log Rotation
- Logs rotate daily or when they reach 10MB
- Maximum 10 backup files kept
- Old logs automatically deleted

### Monitoring Endpoints

Add Spring Boot Actuator for monitoring:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

Access:
- Health: `http://localhost:8181/actuator/health`
- Metrics: `http://localhost:8181/actuator/metrics`

---

## 🛡️ Security Hardening Checklist

### Application Level
- [x] No hardcoded credentials
- [x] Environment variables for all secrets
- [x] CORS properly configured
- [x] JWT with secure secret
- [x] Login attempt tracking
- [x] Proper logging (no sensitive data)
- [ ] Rate limiting (consider adding Spring Cloud Gateway)
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (using JPA/Hibernate)

### Infrastructure Level
- [ ] HTTPS/TLS enabled
- [ ] Database connection encrypted
- [ ] Firewall rules configured
- [ ] Regular security updates
- [ ] Backup strategy implemented
- [ ] Monitoring and alerting set up

### Database Level
- [ ] Strong database password
- [ ] Database user with minimal privileges
- [ ] Regular backups
- [ ] Encryption at rest
- [ ] Connection pooling configured

---

## 🚨 Incident Response

### If Credentials Are Compromised

1. **Immediately rotate all secrets:**
   ```bash
   # Generate new JWT secret
   openssl rand -base64 64
   
   # Update environment variables
   export JWT_SECRET="new-secret"
   
   # Restart application
   ```

2. **Invalidate all existing tokens:**
   - Clear refresh_token table
   - Force all users to re-login

3. **Review logs for suspicious activity:**
   ```bash
   grep "Failed login" ./Logs/security.log
   grep "ERROR" ./Logs/error.log
   ```

4. **Update credentials in all environments**

---

## 📞 Support

For security issues, please contact: security@thinkerscave.com

**Never commit sensitive information to version control!**
