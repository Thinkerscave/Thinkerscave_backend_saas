INSERT INTO catalog_versions (change_type, entity_type, entity_key, payload, created_by)
SELECT 'BASELINE', 'CATALOG', 'INITIAL', '{"description":"Initial synchronized catalog"}', 'system'
WHERE NOT EXISTS (SELECT 1 FROM catalog_versions);
