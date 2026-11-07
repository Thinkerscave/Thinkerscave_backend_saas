-- PENDING OWNER APPROVAL — not packaged, not executed by Flyway.
-- Move to db/migration/public as V7 after approval (see ../pending/README.md).
--
-- Retire the standalone Tenant Health sidebar entry.
-- Tenant Health is consolidated into Migration Center -> Tenants; the /tenant-health route
-- still redirects, so deep links and bookmarks keep working.
-- Menus live in the platform schema (see V4/V6). Preserve the menu row, its permissions,
-- role links and audit history; only remove it from navigation.
-- Safe to re-run: rows already retired are skipped, and the block is a no-op when the
-- platform schema has no menus table (for example isolated local profiles).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'menus'
    ) THEN
        RETURN;
    END IF;

    UPDATE menus
    SET active = FALSE,
        show_in_sidebar = FALSE,
        version = COALESCE(version, 0) + 1
    WHERE (menu_code = 'TENANT_HEALTH' OR route = '/app/tenant-management/tenant-health')
      AND (active IS DISTINCT FROM FALSE OR show_in_sidebar IS DISTINCT FROM FALSE);
END $$;
