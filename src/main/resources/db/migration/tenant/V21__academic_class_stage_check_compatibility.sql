DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'academic_class'
          AND column_name = 'academic_stage'
    ) THEN
        ALTER TABLE academic_class
            DROP CONSTRAINT IF EXISTS academic_class_academic_stage_check;
    END IF;
END $$;