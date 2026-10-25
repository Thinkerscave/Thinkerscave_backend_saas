ALTER TABLE subject
    ALTER COLUMN academic_year_id DROP NOT NULL,
    ALTER COLUMN default_weekly_periods DROP NOT NULL;

ALTER TABLE subject
    DROP CONSTRAINT IF EXISTS chk_subject_weekly_periods;

CREATE UNIQUE INDEX IF NOT EXISTS uk_subject_catalog_code
    ON subject (LOWER(code))
    WHERE academic_year_id IS NULL AND is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_subject_catalog_active_name
    ON subject (LOWER(name), subject_id)
    WHERE is_active = TRUE;