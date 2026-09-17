ALTER TABLE catalog_versions
    ADD COLUMN IF NOT EXISTS target_tenant_registry_id BIGINT REFERENCES tenant_registry(id);

CREATE INDEX IF NOT EXISTS idx_catalog_version_target_tenant
    ON catalog_versions(target_tenant_registry_id, version_number);

UPDATE catalog_versions c
SET target_tenant_registry_id = t.id
FROM tenant_registry t
WHERE c.entity_type = 'ROLE_PERMISSION'
  AND c.target_tenant_registry_id IS NULL
  AND split_part(c.entity_key, ':', 2) ~ '^[0-9]+$'
  AND t.organization_id = split_part(c.entity_key, ':', 2)::BIGINT;
