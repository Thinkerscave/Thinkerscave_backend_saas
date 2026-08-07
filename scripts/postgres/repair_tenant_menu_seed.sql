-- One-shot repair for tenants whose menus/role_permissions were never seeded
-- (sidebar empty). Copies non-PLATFORM menus from public and grants Owner/Admin
-- PAGE permissions. Safe to re-run (ON CONFLICT DO NOTHING).
--
-- Usage: set schema_name + organization_id, then execute.

DO $$
DECLARE
    schema_name text := 'tenant_cp20260724113915';
    org_id bigint := 7;
BEGIN
    EXECUTE format(
        'INSERT INTO %I.menus SELECT * FROM public.menus WHERE COALESCE(menu_scope, %L) <> %L ON CONFLICT DO NOTHING',
        schema_name, 'SUBSCRIPTION', 'PLATFORM'
    );

    EXECUTE format(
        'INSERT INTO %I.role_permissions
           (organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_on, version)
         SELECT %s, r.id, m.id, true, true, true, now(), 0
         FROM %I.roles r
         CROSS JOIN %I.menus m
         WHERE r.role_code IN (%L, %L)
           AND m.menu_type = %L
           AND m.active = true
         ON CONFLICT DO NOTHING',
        schema_name, org_id, schema_name, schema_name, 'ROLE_OWNER', 'ROLE_ADMIN', 'PAGE'
    );

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = schema_name AND table_name = 'organization_modules'
    ) THEN
        EXECUTE format(
            'INSERT INTO %I.organization_modules
               (organization_id, menu_id, enabled, created_on, version)
             SELECT %s, id, true, now(), 0 FROM %I.menus
             ON CONFLICT DO NOTHING',
            schema_name, org_id, schema_name
        );
    END IF;
END $$;
