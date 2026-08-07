-- Adds subscription entitlement columns expected by Menu entity.
-- Safe to re-run: IF NOT EXISTS / DO block over tenant schemas.
-- Flyway is disabled in test/prod profiles; this file is also applied
-- manually via scripts when ddl-auto=update has not caught up.

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
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = s AND table_name = 'menus'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I.menus ADD COLUMN IF NOT EXISTS menu_scope varchar(20) DEFAULT %L',
                s, 'SUBSCRIPTION'
            );
            EXECUTE format(
                'ALTER TABLE %I.menus ADD COLUMN IF NOT EXISTS feature_id bigint',
                s
            );
        END IF;
    END LOOP;
END $$;
