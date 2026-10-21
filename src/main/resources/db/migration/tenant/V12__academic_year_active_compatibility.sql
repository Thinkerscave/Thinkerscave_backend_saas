DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'academic_year'
          AND column_name = 'active'
    ) THEN
        ALTER TABLE academic_year ALTER COLUMN active SET DEFAULT TRUE;
    END IF;
END $$;