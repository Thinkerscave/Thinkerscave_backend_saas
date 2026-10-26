-- V41: Restructure the Timetable module around reusable schedule patterns, group/day
-- assignments and optional fixed activities.
--
-- Existing timetable_configuration / timetable_period rows are migrated in place:
--   * every configuration becomes a reusable schedule_pattern,
--   * its periods become that pattern's ordered slots,
--   * its working days become group/section day assignments for the pattern.
-- School-day "configuration" rows are retained as a hidden settings carrier because other
-- modules (staff attendance) and existing timetable versions reference them. The new
-- timetable UI uses schedule patterns instead.

CREATE TABLE IF NOT EXISTS schedule_pattern (
    schedule_pattern_id BIGSERIAL PRIMARY KEY,
    academic_year_id    BIGINT NOT NULL REFERENCES academic_year (academic_year_id),
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(100),
    created_on          TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_on          TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_schedule_pattern_year_name UNIQUE (academic_year_id, name)
);

CREATE INDEX IF NOT EXISTS idx_schedule_pattern_year_active
    ON schedule_pattern (academic_year_id, is_active);

ALTER TABLE timetable_period
    ADD COLUMN IF NOT EXISTS schedule_pattern_id BIGINT REFERENCES schedule_pattern (schedule_pattern_id);

ALTER TABLE timetable_period
    ALTER COLUMN timetable_configuration_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_timetable_period_pattern_number
    ON timetable_period (schedule_pattern_id, period_number)
    WHERE schedule_pattern_id IS NOT NULL;

-- One pattern per existing configuration.
INSERT INTO schedule_pattern (academic_year_id, name, description, is_active,
                              created_by, created_on, updated_by, updated_on, version)
SELECT c.academic_year_id,
       c.name,
       'Migrated from the previous school-day configuration (' || c.shift_type || ')',
       c.is_active,
       'system', now(), 'system', now(), 0
FROM timetable_configuration c
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_pattern p
    WHERE p.academic_year_id = c.academic_year_id AND p.name = c.name
);

UPDATE timetable_period p
SET schedule_pattern_id = sp.schedule_pattern_id
FROM timetable_configuration c
JOIN schedule_pattern sp
  ON sp.academic_year_id = c.academic_year_id AND sp.name = c.name
WHERE p.timetable_configuration_id = c.timetable_configuration_id
  AND p.schedule_pattern_id IS NULL;

CREATE TABLE IF NOT EXISTS group_schedule_assignment (
    group_schedule_assignment_id BIGSERIAL PRIMARY KEY,
    academic_year_id             BIGINT NOT NULL REFERENCES academic_year (academic_year_id),
    academic_group_id            BIGINT NOT NULL REFERENCES academic_group (academic_group_id),
    section_id                   BIGINT REFERENCES academic_section (section_id),
    day_of_week                  VARCHAR(15) NOT NULL,
    schedule_pattern_id          BIGINT NOT NULL REFERENCES schedule_pattern (schedule_pattern_id),
    effective_from               DATE,
    effective_to                 DATE,
    is_active                    BOOLEAN NOT NULL DEFAULT TRUE,
    created_by                   VARCHAR(100),
    created_on                   TIMESTAMP,
    updated_by                   VARCHAR(100),
    updated_on                   TIMESTAMP,
    version                      BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_group_schedule_effective_dates
        CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_group_schedule_assignment
    ON group_schedule_assignment (
        academic_year_id, academic_group_id, COALESCE(section_id, 0),
        day_of_week, schedule_pattern_id, COALESCE(effective_from, DATE '1900-01-01')
    );

CREATE INDEX IF NOT EXISTS idx_group_schedule_year_day
    ON group_schedule_assignment (academic_year_id, day_of_week, is_active);
CREATE INDEX IF NOT EXISTS idx_group_schedule_group_section
    ON group_schedule_assignment (academic_group_id, section_id);

-- Migrate working days to per-section assignments. Groups without sections are still
-- represented with a group-level assignment (section_id NULL).
INSERT INTO group_schedule_assignment (
    academic_year_id, academic_group_id, section_id, day_of_week, schedule_pattern_id,
    effective_from, effective_to, is_active, created_by, created_on, updated_by, updated_on, version)
SELECT c.academic_year_id, g.academic_group_id, s.section_id, wd.day_of_week,
       sp.schedule_pattern_id, y.start_date, y.end_date, TRUE,
       'system', now(), 'system', now(), 0
FROM timetable_working_day wd
JOIN timetable_configuration c ON c.timetable_configuration_id = wd.timetable_configuration_id
JOIN academic_year y ON y.academic_year_id = c.academic_year_id
JOIN schedule_pattern sp
  ON sp.academic_year_id = c.academic_year_id AND sp.name = c.name
JOIN academic_group g ON g.academic_year_id = c.academic_year_id AND g.is_active = TRUE
LEFT JOIN academic_section s ON s.academic_group_id = g.academic_group_id AND s.is_active = TRUE
WHERE wd.is_working = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM group_schedule_assignment a
      WHERE a.academic_year_id = c.academic_year_id
        AND a.academic_group_id = g.academic_group_id
        AND COALESCE(a.section_id, 0) = COALESCE(s.section_id, 0)
        AND a.day_of_week = wd.day_of_week
        AND a.schedule_pattern_id = sp.schedule_pattern_id
  );

-- A hidden settings configuration is required by attendance and by timetable versions.
INSERT INTO timetable_configuration (
    academic_year_id, name, shift_type, school_start_time, school_end_time,
    default_period_duration_min, max_teacher_weekly_periods, status, is_locked, is_active,
    created_by, created_on, updated_by, updated_on, version)
SELECT y.academic_year_id, 'Timetable settings', 'REGULAR', TIME '08:00', TIME '14:00',
       40, 30, 'READY', FALSE, TRUE, 'system', now(), 'system', now(), 0
FROM academic_year y
WHERE NOT EXISTS (
    SELECT 1 FROM timetable_configuration c WHERE c.academic_year_id = y.academic_year_id
);

-- Keep the settings configuration's working days in sync with migrated assignments so
-- staff attendance keeps recognising working days.
INSERT INTO timetable_working_day (
    timetable_configuration_id, day_of_week, is_working,
    created_by, created_on, updated_by, updated_on, version)
SELECT c.timetable_configuration_id, a.day_of_week, TRUE,
       'system', now(), 'system', now(), 0
FROM (SELECT DISTINCT academic_year_id, day_of_week FROM group_schedule_assignment WHERE is_active = TRUE) a
JOIN timetable_configuration c ON c.academic_year_id = a.academic_year_id
WHERE NOT EXISTS (
    SELECT 1 FROM timetable_working_day wd
    WHERE wd.timetable_configuration_id = c.timetable_configuration_id
      AND wd.day_of_week = a.day_of_week
);

CREATE TABLE IF NOT EXISTS fixed_activity (
    fixed_activity_id   BIGSERIAL PRIMARY KEY,
    academic_year_id    BIGINT NOT NULL REFERENCES academic_year (academic_year_id),
    name                VARCHAR(120) NOT NULL,
    description         VARCHAR(500),
    activity_type       VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    applies_to_all_groups BOOLEAN NOT NULL DEFAULT TRUE,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    recurrence          VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    effective_from      DATE,
    effective_to        DATE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(100),
    created_on          TIMESTAMP,
    updated_by          VARCHAR(100),
    updated_on          TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fixed_activity_times CHECK (end_time > start_time),
    CONSTRAINT chk_fixed_activity_effective_dates
        CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);

CREATE INDEX IF NOT EXISTS idx_fixed_activity_year_active
    ON fixed_activity (academic_year_id, is_active);

CREATE TABLE IF NOT EXISTS fixed_activity_day (
    fixed_activity_id BIGINT NOT NULL REFERENCES fixed_activity (fixed_activity_id) ON DELETE CASCADE,
    day_of_week       VARCHAR(15) NOT NULL,
    PRIMARY KEY (fixed_activity_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS fixed_activity_group (
    fixed_activity_group_id BIGSERIAL PRIMARY KEY,
    fixed_activity_id       BIGINT NOT NULL REFERENCES fixed_activity (fixed_activity_id) ON DELETE CASCADE,
    academic_group_id       BIGINT NOT NULL REFERENCES academic_group (academic_group_id),
    section_id              BIGINT REFERENCES academic_section (section_id),
    created_by              VARCHAR(100),
    created_on              TIMESTAMP,
    updated_by              VARCHAR(100),
    updated_on              TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_fixed_activity_group
    ON fixed_activity_group (fixed_activity_id, academic_group_id, COALESCE(section_id, 0));

ALTER TABLE timetable_entry
    ADD COLUMN IF NOT EXISTS academic_group_id BIGINT REFERENCES academic_group (academic_group_id),
    ADD COLUMN IF NOT EXISTS subject_id BIGINT REFERENCES subject (subject_id),
    ADD COLUMN IF NOT EXISTS staff_id BIGINT REFERENCES staff (staff_id),
    ADD COLUMN IF NOT EXISTS fixed_activity_id BIGINT REFERENCES fixed_activity (fixed_activity_id),
    ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE timetable_entry e
SET academic_group_id = s.academic_group_id
FROM academic_section s
WHERE e.section_id = s.section_id AND e.academic_group_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_timetable_entry_group_day
    ON timetable_entry (timetable_version_id, academic_group_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_timetable_entry_staff_day
    ON timetable_entry (timetable_version_id, staff_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_timetable_entry_fixed_activity
    ON timetable_entry (fixed_activity_id);

CREATE TABLE IF NOT EXISTS timetable_generation_run (
    generation_run_id      BIGSERIAL PRIMARY KEY,
    timetable_version_id   BIGINT NOT NULL REFERENCES timetable_version (timetable_version_id),
    academic_year_id       BIGINT NOT NULL REFERENCES academic_year (academic_year_id),
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    seed                   BIGINT,
    requested_group_ids    VARCHAR(2000),
    report                 TEXT,
    started_at             TIMESTAMP,
    finished_at            TIMESTAMP,
    requested_by_user_id   BIGINT,
    created_by             VARCHAR(100),
    created_on             TIMESTAMP,
    updated_by             VARCHAR(100),
    updated_on             TIMESTAMP,
    version                BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_timetable_generation_run_version
    ON timetable_generation_run (timetable_version_id);

CREATE TABLE IF NOT EXISTS timetable_version_event (
    timetable_version_event_id BIGSERIAL PRIMARY KEY,
    timetable_version_id       BIGINT NOT NULL REFERENCES timetable_version (timetable_version_id),
    event_type                 VARCHAR(40) NOT NULL,
    from_status                VARCHAR(40),
    to_status                  VARCHAR(40),
    note                       VARCHAR(1000),
    actor_user_id              BIGINT,
    created_by                 VARCHAR(100),
    created_on                 TIMESTAMP,
    updated_by                 VARCHAR(100),
    updated_on                 TIMESTAMP,
    version                    BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_timetable_version_event_version
    ON timetable_version_event (timetable_version_id, created_on);

-- Rollback (manual): drop the new tables/columns and restore NOT NULL on
-- timetable_period.timetable_configuration_id after moving slots back to their configuration.
