CREATE TABLE IF NOT EXISTS tenant_release_metadata (
    id BIGSERIAL PRIMARY KEY,
    release_version VARCHAR(50),
    application_version VARCHAR(50),
    database_version VARCHAR(50) NOT NULL DEFAULT '3',
    applied_at TIMESTAMP NOT NULL DEFAULT now(),
    created_on TIMESTAMP NOT NULL DEFAULT now(),
    updated_on TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tenant_release_metadata_applied_at
    ON tenant_release_metadata (applied_at DESC);
