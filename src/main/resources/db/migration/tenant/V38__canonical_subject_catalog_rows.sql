ALTER TABLE subject
    ADD COLUMN is_catalog BOOLEAN NOT NULL DEFAULT TRUE;

WITH ranked_subjects AS (
    SELECT subject_id,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(code)
               ORDER BY CASE WHEN is_active THEN 0 ELSE 1 END,
                        academic_year_id NULLS FIRST,
                        subject_id
           ) AS row_number
    FROM subject
)
UPDATE subject s
SET is_catalog = (r.row_number = 1)
FROM ranked_subjects r
WHERE r.subject_id = s.subject_id;

DROP INDEX IF EXISTS uk_subject_catalog_code;

CREATE UNIQUE INDEX uk_subject_catalog_code
    ON subject (LOWER(code))
    WHERE is_catalog = TRUE AND is_active = TRUE;

DROP INDEX IF EXISTS idx_subject_catalog_active_name;

CREATE INDEX idx_subject_catalog_active_name
    ON subject (LOWER(name), subject_id)
    WHERE is_catalog = TRUE AND is_active = TRUE;