DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'academic_year'
          AND column_name = 'year_code'
    ) THEN
        EXECUTE 'CREATE OR REPLACE FUNCTION set_academic_year_legacy_code() RETURNS trigger LANGUAGE plpgsql AS $func$ BEGIN IF NEW.year_code IS NULL THEN NEW.year_code := NEW.name; END IF; RETURN NEW; END; $func$';
        DROP TRIGGER IF EXISTS trg_academic_year_legacy_code ON academic_year;
        CREATE TRIGGER trg_academic_year_legacy_code
            BEFORE INSERT ON academic_year
            FOR EACH ROW
            EXECUTE FUNCTION set_academic_year_legacy_code();
    END IF;
END $$;