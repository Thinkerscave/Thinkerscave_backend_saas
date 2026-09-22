CREATE TABLE IF NOT EXISTS tenant_catalog_sync_checkpoint (
    catalog_version BIGINT PRIMARY KEY,
    synchronization_status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    synchronized_at TIMESTAMP NOT NULL DEFAULT now(),
    execution_reference VARCHAR(100),
    created_on TIMESTAMP NOT NULL DEFAULT now(),
    updated_on TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tenant_catalog_sync_checkpoint_time
    ON tenant_catalog_sync_checkpoint (synchronized_at DESC);
