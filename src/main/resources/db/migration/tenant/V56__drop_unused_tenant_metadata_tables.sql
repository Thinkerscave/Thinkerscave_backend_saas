-- Deep-clean: drop tenant metadata tables that no application code reads or writes.
--
-- Evidence (static repository analysis, 2026-11-06):
--   * tenant_schema_metadata          -> only INSERTed by V1__canonical_tenant_baseline.sql;
--                                        no Java/JDBC reader anywhere in the backend.
--   * tenant_release_metadata         -> created by V3__tenant_release_metadata.sql;
--                                        no Java/JDBC reader or writer.
--   * tenant_catalog_sync_checkpoint  -> created by V4__tenant_catalog_sync_checkpoint.sql;
--                                        no Java/JDBC reader or writer.
--
-- These tables are NOT part of TenantScopedFlyway.FINGERPRINT
-- (users, roles, user_roles, tenant_registry, organizations), so removing them
-- cannot break canonical-baseline validation.
--
-- Deliberately NOT dropped (live or intentionally retained):
--   * catalog_versions, catalog_sync_executions, provisioning_steps,
--     operation_audit_events, tenant_migration_results, tenant_operation_locks
--     (read/written by platform services)
--   * fixed_activity_day (JPA @CollectionTable on FixedActivity)
--   * student_document (kept by V53 for reconciliation/rollback)
--
-- Execution: run once per tenant schema (TenantScopedFlyway sets current_schema()).
-- The drops are idempotent and contain no data migration.

DROP TABLE IF EXISTS tenant_catalog_sync_checkpoint;
DROP TABLE IF EXISTS tenant_release_metadata;
DROP TABLE IF EXISTS tenant_schema_metadata;
