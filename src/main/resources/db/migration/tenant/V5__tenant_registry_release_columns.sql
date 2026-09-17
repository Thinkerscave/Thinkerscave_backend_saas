ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS catalog_version BIGINT;
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS application_version VARCHAR(50);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS observed_database_version VARCHAR(50);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS maintenance_reason VARCHAR(500);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS maintenance_operation VARCHAR(80);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS maintenance_started_at TIMESTAMP;
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS health_status VARCHAR(20);
ALTER TABLE tenant_registry ADD COLUMN IF NOT EXISTS health_message VARCHAR(1000);
