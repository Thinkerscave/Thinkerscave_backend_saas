-- ============================================
-- DEV Schema Initialization (H2)
-- Tables here supplement Hibernate's auto-DDL.
-- Only for global/shared tables not tied to entities.
-- ============================================

-- Global user-tenant mapping (normally lives in 'public' schema in PostgreSQL)
CREATE TABLE IF NOT EXISTS user_tenant_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255),
    tenant_id VARCHAR(100) NOT NULL DEFAULT 'public',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tenant configuration (normally lives in 'public' schema in PostgreSQL)
CREATE TABLE IF NOT EXISTS tenant_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    features TEXT,
    max_users INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
