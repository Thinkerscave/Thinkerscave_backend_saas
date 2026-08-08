-- Add ApplicationAdmission.archived (table name is singular: application_admission).
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
            WHERE table_schema = s AND table_name = 'application_admission'
        ) THEN
            EXECUTE format(
                'ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false',
                s
            );
        END IF;
    END LOOP;
END $$;
