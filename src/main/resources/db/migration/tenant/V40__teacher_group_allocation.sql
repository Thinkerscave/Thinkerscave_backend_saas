-- V40: Teacher allocation defaults at Academic Group level.
-- A group-level teacher is stored once in teacher_group_allocation. Each section receives a teacher_allocation row
-- flagged inherited = TRUE, so timetable lessons keep linking to a per-section allocation. A section-level
-- override is a teacher_allocation row with inherited = FALSE and is never overwritten by a group change.

ALTER TABLE teacher_allocation
    ADD COLUMN IF NOT EXISTS inherited BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS teacher_group_allocation (
    teacher_group_allocation_id       BIGSERIAL PRIMARY KEY,
    academic_group_subject_mapping_id BIGINT NOT NULL
        REFERENCES academic_group_subject_mapping (academic_group_subject_mapping_id),
    staff_id                          BIGINT NOT NULL REFERENCES staff (staff_id),
    is_active                         BOOLEAN NOT NULL DEFAULT TRUE,
    created_by                        VARCHAR(100),
    created_on                        TIMESTAMP,
    updated_by                        VARCHAR(100),
    updated_on                        TIMESTAMP,
    version                           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_teacher_group_allocation_mapping UNIQUE (academic_group_subject_mapping_id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_group_allocation_staff
    ON teacher_group_allocation (staff_id);

-- Rollback: DROP TABLE IF EXISTS teacher_group_allocation;
