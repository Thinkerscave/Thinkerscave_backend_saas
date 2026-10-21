DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'academic_class'
          AND column_name = 'academic_stage'
    ) THEN
        EXECUTE $sql$
            CREATE OR REPLACE FUNCTION set_academic_class_legacy_stage()
            RETURNS trigger
            LANGUAGE plpgsql
            AS $func$
            DECLARE
                row_data JSONB;
            BEGIN
                row_data := to_jsonb(NEW);
                IF row_data ? 'academic_stage' AND row_data->>'academic_stage' IS NULL THEN
                    row_data := jsonb_set(row_data, '{academic_stage}', row_data->'stage', TRUE);
                END IF;
                NEW := jsonb_populate_record(NEW, row_data);
                RETURN NEW;
            END;
            $func$
        $sql$;
        DROP TRIGGER IF EXISTS trg_academic_class_legacy_stage ON academic_class;
        CREATE TRIGGER trg_academic_class_legacy_stage
            BEFORE INSERT ON academic_class
            FOR EACH ROW
            EXECUTE FUNCTION set_academic_class_legacy_stage();
    END IF;
END $$;