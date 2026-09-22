# PostgreSQL scripts (manual / DBA only)

**The application never runs these files.** Schema and master data are prepared outside the app (psql, DBA tools, or CI DB jobs). Spring Boot starts with `ddl-auto=none`, Flyway off, and `spring.sql.init.mode=never`.

## Master data (use this)

| File | Purpose |
|------|---------|
| `01_platform_master_seed.sql` | Platform master data only: roles, privileges, Super Admin user, menus/features baseline, code sequences, finance categories / other catalog seeds as maintained here. |

Example:

```bash
psql -h <host> -U <user> -d <database> -v ON_ERROR_STOP=1 -f 01_platform_master_seed.sql
```

## Other files

| Path | Purpose |
|------|---------|
| `repair_tenant_menu_seed.sql` | One-off repair helper — run manually if needed |
| `archive/migration/` | Historical schema scripts (not Flyway-wired) |
| `archive/dev-demo/` | Old local MySQL demo dumps — do not use on live |

When you later split test vs prod databases, keep using the same master seed file and point each environment’s datasource via env vars only.
