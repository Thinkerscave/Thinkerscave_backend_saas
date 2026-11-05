ALTER TABLE student_enrollment
    DROP CONSTRAINT IF EXISTS uk_student_academic_year;

CREATE INDEX IF NOT EXISTS idx_student_enrollment_student_year_active
    ON student_enrollment (student_id, academic_year_id, active);
