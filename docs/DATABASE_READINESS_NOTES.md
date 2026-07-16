# Database Readiness Notes — Release 2
# Do NOT recreate tables. Apply only as additive DDL when needed.

## Already covered by Flyway (tenant schemas)

- `V1_6__add_database_constraints.sql` — constraints
- `V1_13__add_missing_indexes.sql` — performance indexes
- `V1_14__add_foreign_key_constraints.sql` — FKs

## Recommended additive checks (run manually against MySQL first)

```sql
-- Verify unique login identity within an organization
-- SHOW INDEX FROM users WHERE Key_name LIKE '%username%' OR Column_name IN ('username','email');

-- Suggested if missing (MySQL 8+):
-- CREATE UNIQUE INDEX uk_users_org_username ON users (organization_id, username);
-- CREATE UNIQUE INDEX uk_users_org_email ON users (organization_id, email);

-- Student uniqueness within org (if business rule requires):
-- CREATE UNIQUE INDEX uk_student_org_code ON student (organization_id, student_code);

-- Session refresh token lookup:
-- CREATE INDEX idx_user_session_refresh ON user_session (refresh_token(64));
```

## PostgreSQL notes (future)

- Replace MySQL `IF NOT EXISTS` index syntax review (already OK for PG)
- Convert backtick identifiers to quoted identifiers
- Re-validate FK cascade rules after import
- Ensure each `tenant_*` schema receives the same index set as platform/tenant migrations define

## Explicit non-goals of Release 2

- No table recreation
- No destructive ALTER that drops columns
- No forced schema rewrite for PostgreSQL today
