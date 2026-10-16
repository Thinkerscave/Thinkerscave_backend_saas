DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'academic_year'
          AND column_name IN ('year_code', 'year_name')
    ) THEN
        EXECUTE $sql$
            CREATE OR REPLACE FUNCTION set_academic_year_legacy_code()
            RETURNS trigger
            LANGUAGE plpgsql
            AS $func$
            DECLARE
                row_data JSONB;
            BEGIN
                row_data := to_jsonb(NEW);
                IF row_data ? 'year_code' AND row_data->>'year_code' IS NULL THEN
                    row_data := jsonb_set(row_data, '{year_code}', row_data->'name', TRUE);
                END IF;
                IF row_data ? 'year_name' AND row_data->>'year_name' IS NULL THEN
                    row_data := jsonb_set(row_data, '{year_name}', row_data->'name', TRUE);
                END IF;
                NEW := jsonb_populate_record(NEW, row_data);
                RETURN NEW;
            END;
            $func$
        $sql$;
        DROP TRIGGER IF EXISTS trg_academic_year_legacy_code ON academic_year;
        CREATE TRIGGER trg_academic_year_legacy_code
            BEFORE INSERT ON academic_year
            FOR EACH ROW
            EXECUTE FUNCTION set_academic_year_legacy_code();
    END IF;
END $$;