DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'academic_section'
          AND column_name IN ('active', 'section_code', 'section_name')
    ) THEN
        EXECUTE $sql$
            CREATE OR REPLACE FUNCTION set_academic_section_legacy_columns()
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
                IF row_data ? 'section_code' AND row_data->>'section_code' IS NULL THEN
                    row_data := jsonb_set(row_data, '{section_code}', row_data->'code', TRUE);
                END IF;
                IF row_data ? 'section_name' AND row_data->>'section_name' IS NULL THEN
                    row_data := jsonb_set(row_data, '{section_name}', row_data->'name', TRUE);
                END IF;
                NEW := jsonb_populate_record(NEW, row_data);
                RETURN NEW;
            END;
            $func$
        $sql$;
        DROP TRIGGER IF EXISTS trg_academic_section_legacy_columns ON academic_section;
        CREATE TRIGGER trg_academic_section_legacy_columns
            BEFORE INSERT ON academic_section
            FOR EACH ROW
            EXECUTE FUNCTION set_academic_section_legacy_columns();
    END IF;
END $$;