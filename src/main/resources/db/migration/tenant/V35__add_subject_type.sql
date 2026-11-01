ALTER TABLE subject
    ADD COLUMN IF NOT EXISTS subject_type VARCHAR(30);

UPDATE subject
SET subject_type = CASE
    WHEN category IN ('LAB', 'PRACTICAL') THEN 'PRACTICAL_LAB'
    WHEN category = 'ACTIVITY' THEN 'ACTIVITY'
    ELSE 'ACADEMIC_SUBJECT'
END
WHERE subject_type IS NULL;

ALTER TABLE subject
    ALTER COLUMN subject_type SET DEFAULT 'ACADEMIC_SUBJECT',
    ALTER COLUMN subject_type SET NOT NULL;

ALTER TABLE subject
    ADD CONSTRAINT chk_subject_type
        CHECK (subject_type IN ('ACADEMIC_SUBJECT', 'PRACTICAL_LAB', 'ACTIVITY'));