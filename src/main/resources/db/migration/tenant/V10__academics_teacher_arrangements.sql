CREATE TABLE IF NOT EXISTS academics_teacher_arrangement (
    teacher_arrangement_id BIGSERIAL PRIMARY KEY,
    academic_year_id BIGINT NOT NULL REFERENCES academic_year (academic_year_id),
    timetable_version_id BIGINT NOT NULL REFERENCES timetable_version (timetable_version_id),
    absent_teacher_staff_id BIGINT NOT NULL REFERENCES staff (staff_id),
    absence_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id BIGINT,
    reviewed_at TIMESTAMP,
    review_note VARCHAR(500),
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_academics_teacher_arrangement_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_academics_teacher_arrangement_reason CHECK (length(trim(reason)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_academics_teacher_arrangement_year_date
    ON academics_teacher_arrangement (academic_year_id, absence_date);
CREATE INDEX IF NOT EXISTS idx_academics_teacher_arrangement_status
    ON academics_teacher_arrangement (status);

CREATE TABLE IF NOT EXISTS academics_teacher_arrangement_slot (
    teacher_arrangement_slot_id BIGSERIAL PRIMARY KEY,
    teacher_arrangement_id BIGINT NOT NULL REFERENCES academics_teacher_arrangement (teacher_arrangement_id) ON DELETE CASCADE,
    timetable_entry_id BIGINT NOT NULL REFERENCES timetable_entry (timetable_entry_id),
    substitute_staff_id BIGINT NOT NULL REFERENCES staff (staff_id),
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_academics_teacher_arrangement_slot_entry UNIQUE (teacher_arrangement_id, timetable_entry_id)
);

CREATE INDEX IF NOT EXISTS idx_academics_teacher_arrangement_slot_entry
    ON academics_teacher_arrangement_slot (timetable_entry_id);
CREATE INDEX IF NOT EXISTS idx_academics_teacher_arrangement_slot_substitute
    ON academics_teacher_arrangement_slot (substitute_staff_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
                WHERE conname = 'ck_academics_teacher_arrangement_status'
                    AND conrelid = 'academics_teacher_arrangement'::regclass
    ) THEN
                ALTER TABLE academics_teacher_arrangement
                        ADD CONSTRAINT ck_academics_teacher_arrangement_status
            CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
                WHERE conname = 'ck_academics_teacher_arrangement_reason'
                    AND conrelid = 'academics_teacher_arrangement'::regclass
    ) THEN
                ALTER TABLE academics_teacher_arrangement
                        ADD CONSTRAINT ck_academics_teacher_arrangement_reason
            CHECK (length(trim(reason)) > 0);
    END IF;
END $$;