# Pending platform migrations (not packaged)

Scripts in this folder are **not** executed by Flyway. Platform Flyway only scans
`classpath:db/migration/public`, and tenant Flyway only scans `classpath:db/migration/tenant`.

Use this folder to prepare and review a migration that needs owner approval before it can
change the shared database:

1. Review the script with the owner and confirm the target schemas/rows.
2. Take/confirm a backup of the affected schema.
3. Move the file into `db/migration/public` (platform schema) or `db/migration/tenant`
   (per-tenant schemas) with the next free version number.
4. Restart/redeploy only after approval; Flyway applies it once and records the checksum.
5. Delete the entry from this folder.

| Script | Purpose | Status |
| --- | --- | --- |
| `V7__retire_tenant_health_menu.sql` | Hides the retired `TENANT_HEALTH` sidebar row now that Tenant Health lives in Migration Center → Tenants. Keeps the menu row, permissions and audit history. | Prepared, awaiting owner approval |

> `V56__drop_unused_tenant_metadata_tables.sql` was approved by the owner on 2026-11-07 and
> moved into `db/migration/tenant` for execution through Migration Center.

> Do not add scripts here that duplicate a version already present in
> `db/migration/public` or `db/migration/tenant`.
