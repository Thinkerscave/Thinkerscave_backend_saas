-- Remove leftover Fee Management seed tables. The module is being rebuilt.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = current_schema()
          AND (
              tablename LIKE 'fee_%'
              OR tablename IN ('late_fee_config', 'concession_master')
          )
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I CASCADE', r.tablename);
    END LOOP;
END $$;
