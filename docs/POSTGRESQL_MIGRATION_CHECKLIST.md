# PostgreSQL Migration Preparation Checklist
# Thinkerscave Backend — Schema-per-Tenant SaaS
#
# Status: MySQL remains the active development database.
# This document prepares the future Hostinger / PostgreSQL cutover.
# DO NOT treat this as completed migration work.

## Already prepared in code

1. `TenantSchemaSwitcher` abstraction
   - Active: `MysqlCatalogSchemaSwitcher` (`app.tenancy.schema-switcher=mysql`)
   - Ready: `PostgresSearchPathSchemaSwitcher` (`app.tenancy.schema-switcher=postgresql`)
2. Configurable `app.tenancy.platform-schema` (no hardcoded `thinkerscave_dev` in provider)
3. `application-prod.properties` targets PostgreSQL driver + SCHEMA multi-tenancy
4. Flyway wired (`flyway-core`, `flyway-mysql`, `flyway-database-postgresql`)
5. Prod `ddl-auto=validate` + Flyway enabled

## Remaining migration steps (future release)

### A. Schema & SQL dialect
- [ ] Audit all native SQL / `@Query` for MySQL-only functions (`IFNULL`, `GROUP_CONCAT`, backticks)
- [ ] Convert Flyway scripts under `db/migration` to PostgreSQL-compatible DDL (or dual dialect folders)
- [ ] Replace MySQL `AUTO_INCREMENT` assumptions with PostgreSQL sequences / `IDENTITY`
- [ ] Review `BOOLEAN`, `TEXT`, `JSON`, `DATETIME` type mappings
- [ ] Validate indexes/FK/unique constraints on PostgreSQL

### B. Multi-tenancy cutover
- [ ] Create platform schema + `tenant_*` schemas on PostgreSQL
- [ ] Set `TENANCY_SCHEMA_SWITCHER=postgresql`
- [ ] Set `TENANCY_PLATFORM_SCHEMA=public` (or dedicated platform schema)
- [ ] Integration-test login + authenticated request catalog/schema routing
- [ ] Confirm TenantFilter JWT binding still prevents spoofing after cutover

### C. Data migration
- [ ] Export MySQL `thinkerscave_dev` + each `tenant_*` catalog
- [ ] Import into PostgreSQL schemas with identity continuity
- [ ] Re-seed menus/roles if required
- [ ] Verify Flyway history table (`flyway_schema_history`) baseline

### D. Runtime
- [ ] Point `DB_URL` to PostgreSQL
- [ ] Remove MySQL-only connection params from prod env
- [ ] Smoke-test Actuator health, auth cookie refresh, student upload
- [ ] Keep MySQL profile available for local developers until team cutover

## Explicit non-goals of Release 2

- No forced PostgreSQL for local development
- No row-level (discriminator) multi-tenancy redesign
- No cloud object storage migration
