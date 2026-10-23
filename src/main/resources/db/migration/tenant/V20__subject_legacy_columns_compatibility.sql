DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'subject'
          AND column_name IN ('active', 'subject_code', 'subject_name')
    ) THEN
        EXECUTE $sql$
            CREATE OR REPLACE FUNCTION set_subject_legacy_columns()
            RETURNS trigger
            LANGUAGE plpgsql
            AS $func$
            DECLARE
                row_data JSONB;
            BEGIN
                row_data := to_jsonb(NEW);
                IF row_data ? 'active' AND row_data->>'active' IS NULL THEN
                    row_data := jsonb_set(row_data, '{active}', row_data->'is_active', TRUE);
                END IF;
                IF row_data ? 'subject_code' AND row_data->>'subject_code' IS NULL THEN
                    row_data := jsonb_set(row_data, '{subject_code}', row_data->'code', TRUE);
                END IF;
                IF row_data ? 'subject_name' AND row_data->>'subject_name' IS NULL THEN
                    row_data := jsonb_set(row_data, '{subject_name}', row_data->'name', TRUE);
                END IF;
                NEW := jsonb_populate_record(NEW, row_data);
                RETURN NEW;
            END;
            $func$
        $sql$;
        DROP TRIGGER IF EXISTS trg_subject_legacy_columns ON subject;
        CREATE TRIGGER trg_subject_legacy_columns
            BEFORE INSERT ON subject
            FOR EACH ROW
            EXECUTE FUNCTION set_subject_legacy_columns();
    END IF;
END $$;