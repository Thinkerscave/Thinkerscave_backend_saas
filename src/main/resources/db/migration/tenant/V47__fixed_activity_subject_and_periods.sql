-- V47: Fixed activities reference the reusable Activity subject catalog and the
-- configured timetable period for each applicable group/section/day combo.
--
-- start_time/end_time are retained for backwards compatibility and as a read-only
-- snapshot (earliest/latest resolved period) but the canonical scheduling data is
-- fixed_activity_period -> timetable_period.

ALTER TABLE fixed_activity
    ADD COLUMN IF NOT EXISTS subject_id BIGINT REFERENCES subject (subject_id);

ALTER TABLE fixed_activity
    ADD COLUMN IF NOT EXISTS staff_id BIGINT REFERENCES staff (staff_id);

ALTER TABLE fixed_activity
    ADD COLUMN IF NOT EXISTS period_name VARCHAR(50);

ALTER TABLE fixed_activity
    ALTER COLUMN name TYPE VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_fixed_activity_subject
    ON fixed_activity (subject_id);

CREATE TABLE IF NOT EXISTS fixed_activity_period (
    fixed_activity_period_id BIGSERIAL PRIMARY KEY,
    fixed_activity_id        BIGINT NOT NULL REFERENCES fixed_activity (fixed_activity_id) ON DELETE CASCADE,
    academic_group_id        BIGINT NOT NULL REFERENCES academic_group (academic_group_id),
    section_id               BIGINT REFERENCES academic_section (section_id),
    day_of_week              VARCHAR(15) NOT NULL,
    timetable_period_id      BIGINT NOT NULL REFERENCES timetable_period (timetable_period_id),
    created_by               VARCHAR(100),
    created_on               TIMESTAMP,
    updated_by               VARCHAR(100),
    updated_on               TIMESTAMP,
    version                  BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_fixed_activity_period_unit_day
    ON fixed_activity_period (fixed_activity_id, academic_group_id, COALESCE(section_id, 0), day_of_week);

CREATE INDEX IF NOT EXISTS idx_fixed_activity_period_period
    ON fixed_activity_period (timetable_period_id);

-- Rollback (manual): drop fixed_activity_period, drop subject_id/staff_id/period_name
-- and restore name to VARCHAR(120) after clearing long values.
