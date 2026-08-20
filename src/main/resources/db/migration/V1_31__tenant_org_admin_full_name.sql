-- Keep tenant copies of platform catalog tables in sync with public.
-- Hibernate ddl-auto=update only alters the current schema, so tenant
-- organizations were missing admin_full_name and login failed with:
--   ERROR: column "admin_full_name" does not exist

DO $$
DECLARE
    s text;
BEGIN
    FOR s IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname = 'public'
           OR nspname LIKE 'tenant_%'
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = s AND table_name = 'organizations'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I.organizations ADD COLUMN IF NOT EXISTS admin_full_name VARCHAR(200) NULL',
                s
            );
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = s AND table_name = 'tenant_registry'
        ) THEN
            EXECUTE format('ALTER TABLE %I.tenant_registry ADD COLUMN IF NOT EXISTS student_count INTEGER', s);
            EXECUTE format('ALTER TABLE %I.tenant_registry ADD COLUMN IF NOT EXISTS staff_count INTEGER', s);
            EXECUTE format('ALTER TABLE %I.tenant_registry ADD COLUMN IF NOT EXISTS branch_count INTEGER', s);
            EXECUTE format('ALTER TABLE %I.tenant_registry ADD COLUMN IF NOT EXISTS class_count INTEGER', s);
            EXECUTE format('ALTER TABLE %I.tenant_registry ADD COLUMN IF NOT EXISTS section_count INTEGER', s);
            EXECUTE format('ALTER TABLE %I.tenant_registry ADD COLUMN IF NOT EXISTS usage_refreshed_at TIMESTAMP', s);
        END IF;
    END LOOP;
END $$;
