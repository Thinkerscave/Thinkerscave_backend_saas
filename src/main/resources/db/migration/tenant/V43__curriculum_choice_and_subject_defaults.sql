-- V43: Optional-subject choice groups, consecutive-period requirements and
-- subject-level scheduling defaults.

ALTER TABLE academic_group_subject_mapping
    ADD COLUMN IF NOT EXISTS choice_group VARCHAR(60),
    ADD COLUMN IF NOT EXISTS consecutive_periods SMALLINT NOT NULL DEFAULT 1;

ALTER TABLE subject
    ADD COLUMN IF NOT EXISTS default_lesson_duration_min SMALLINT,
    ADD COLUMN IF NOT EXISTS default_timetable_preference VARCHAR(30);

ALTER TABLE academic_group_subject_mapping
    DROP CONSTRAINT IF EXISTS chk_mapping_consecutive_periods;

ALTER TABLE academic_group_subject_mapping
    ADD CONSTRAINT chk_mapping_consecutive_periods
    CHECK (consecutive_periods >= 1 AND consecutive_periods <= 10);

CREATE INDEX IF NOT EXISTS idx_group_subject_mapping_choice_group
    ON academic_group_subject_mapping (academic_group_id, choice_group);

ALTER TABLE timetable_version
    ADD COLUMN IF NOT EXISTS source_version_id BIGINT;

ALTER TABLE timetable_version
    DROP CONSTRAINT IF EXISTS fk_timetable_version_source;

ALTER TABLE timetable_version
    ADD CONSTRAINT fk_timetable_version_source
    FOREIGN KEY (source_version_id) REFERENCES timetable_version(timetable_version_id) ON DELETE SET NULL;
