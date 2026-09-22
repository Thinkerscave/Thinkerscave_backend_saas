CREATE TABLE IF NOT EXISTS platform_releases (
    id BIGSERIAL PRIMARY KEY,
    release_id VARCHAR(80) NOT NULL UNIQUE,
    release_version VARCHAR(50) NOT NULL,
    application_version VARCHAR(50) NOT NULL,
    target_database_version VARCHAR(50) NOT NULL,
    target_catalog_version VARCHAR(50) NOT NULL,
    release_notes VARCHAR(4000),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    released_at TIMESTAMP,
    created_by VARCHAR(100),
    created_on TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tenant_migration_executions (
    id BIGSERIAL PRIMARY KEY,
    release_id BIGINT REFERENCES platform_releases(id),
    tenant_registry_id BIGINT NOT NULL REFERENCES tenant_registry(id),
    from_version VARCHAR(50),
    target_version VARCHAR(50) NOT NULL,
    observed_version VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt INTEGER NOT NULL DEFAULT 1,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message VARCHAR(4000),
    created_by VARCHAR(100),
    created_on TIMESTAMP NOT NULL DEFAULT now(),
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tenant_migration_tenant ON tenant_migration_executions(tenant_registry_id, created_on DESC);
CREATE INDEX IF NOT EXISTS idx_tenant_migration_release ON tenant_migration_executions(release_id);

CREATE TABLE IF NOT EXISTS tenant_migration_results (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL REFERENCES tenant_migration_executions(id) ON DELETE CASCADE,
    migration_version VARCHAR(50),
    description VARCHAR(500),
    script VARCHAR(500),
    execution_time_ms INTEGER,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(4000),
    installed_on TIMESTAMP,
    created_on TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS tenant_operation_locks (
    tenant_registry_id BIGINT PRIMARY KEY REFERENCES tenant_registry(id) ON DELETE CASCADE,
    operation_type VARCHAR(50) NOT NULL,
    execution_id BIGINT,
    owner_token VARCHAR(100) NOT NULL,
    acquired_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS catalog_version_number_seq;

CREATE TABLE IF NOT EXISTS catalog_versions (
    id BIGSERIAL PRIMARY KEY,
    version_number BIGINT NOT NULL UNIQUE DEFAULT nextval('catalog_version_number_seq'),
    change_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_key VARCHAR(150) NOT NULL,
    payload TEXT,
    created_by VARCHAR(100),
    created_on TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS catalog_sync_executions (
    id BIGSERIAL PRIMARY KEY,
    catalog_version_id BIGINT NOT NULL REFERENCES catalog_versions(id),
    tenant_registry_id BIGINT NOT NULL REFERENCES tenant_registry(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt INTEGER NOT NULL DEFAULT 1,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message VARCHAR(4000),
    created_on TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(catalog_version_id, tenant_registry_id, attempt)
);

CREATE TABLE IF NOT EXISTS provisioning_steps (
    id BIGSERIAL PRIMARY KEY,
    provisioning_job_id BIGINT NOT NULL REFERENCES provisioning_jobs(id) ON DELETE CASCADE,
    step_key VARCHAR(80) NOT NULL,
    display_order INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt INTEGER NOT NULL DEFAULT 1,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message VARCHAR(4000),
    created_on TIMESTAMP NOT NULL DEFAULT now(),
    updated_on TIMESTAMP,
    UNIQUE(provisioning_job_id, step_key)
);

CREATE TABLE IF NOT EXISTS operation_audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    actor VARCHAR(100),
    tenant_identifier VARCHAR(50),
    entity_type VARCHAR(80),
    entity_id VARCHAR(100),
    execution_id BIGINT,
    status VARCHAR(20) NOT NULL,
    target_version VARCHAR(50),
    details TEXT,
    error_message VARCHAR(2000),
    created_on TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_operation_audit_tenant ON operation_audit_events(tenant_identifier, created_on DESC);
CREATE INDEX IF NOT EXISTS idx_operation_audit_execution ON operation_audit_events(execution_id);

ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS catalog_version BIGINT;
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS application_version VARCHAR(50);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS observed_database_version VARCHAR(50);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS maintenance_reason VARCHAR(500);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS maintenance_operation VARCHAR(80);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS maintenance_started_at TIMESTAMP;
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS health_status VARCHAR(20);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS health_message VARCHAR(1000);
ALTER TABLE user_permissions ADD COLUMN IF NOT EXISTS organization_id BIGINT;
ALTER TABLE user_permissions DROP CONSTRAINT IF EXISTS uk_user_permission;
ALTER TABLE user_permissions ADD CONSTRAINT uk_user_permission
    UNIQUE (user_id, menu_id, organization_id);
CREATE INDEX IF NOT EXISTS idx_user_permission_organization ON user_permissions(organization_id);
