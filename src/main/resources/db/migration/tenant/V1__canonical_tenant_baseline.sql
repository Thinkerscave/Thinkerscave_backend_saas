-- Canonical tenant baseline marker.
-- New tenant structures are created from the application's canonical schema
-- before this migration is applied. Existing non-empty schemas may only be
-- baselined at V1 after TenantScopedFlyway validates their structural fingerprint.
CREATE TABLE IF NOT EXISTS tenant_schema_metadata (
    metadata_key VARCHAR(100) PRIMARY KEY,
    metadata_value VARCHAR(500) NOT NULL,
    updated_on TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO tenant_schema_metadata(metadata_key, metadata_value, updated_on)
VALUES ('canonical_baseline', '1', now())
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value, updated_on = EXCLUDED.updated_on;
