CREATE TABLE subject_choice_group (
    subject_choice_group_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subject_type VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_subject_choice_group_type
        CHECK (subject_type IN ('ACADEMIC_SUBJECT', 'PRACTICAL_LAB', 'ACTIVITY'))
);

CREATE UNIQUE INDEX uk_subject_choice_group_name
    ON subject_choice_group (LOWER(name));

CREATE TABLE subject_choice_group_member (
    subject_choice_group_member_id BIGSERIAL PRIMARY KEY,
    subject_choice_group_id BIGINT NOT NULL
        REFERENCES subject_choice_group(subject_choice_group_id) ON DELETE RESTRICT,
    subject_id BIGINT NOT NULL REFERENCES subject(subject_id) ON DELETE RESTRICT,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_subject_choice_group_member UNIQUE (subject_choice_group_id, subject_id)
);

CREATE INDEX idx_subject_choice_group_member_subject
    ON subject_choice_group_member (subject_id);

CREATE TABLE academic_group_choice_mapping (
    academic_group_choice_mapping_id BIGSERIAL PRIMARY KEY,
    academic_group_id BIGINT NOT NULL
        REFERENCES academic_group(academic_group_id) ON DELETE RESTRICT,
    subject_choice_group_id BIGINT NOT NULL
        REFERENCES subject_choice_group(subject_choice_group_id) ON DELETE RESTRICT,
    weekly_periods SMALLINT NOT NULL,
    timetable_preference VARCHAR(30) NOT NULL DEFAULT 'ANY',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_academic_group_choice_mapping
        UNIQUE (academic_group_id, subject_choice_group_id),
    CONSTRAINT chk_academic_group_choice_mapping_periods
        CHECK (weekly_periods >= 1 AND weekly_periods <= 40),
    CONSTRAINT chk_academic_group_choice_mapping_preference
        CHECK (timetable_preference IN ('ANY', 'FIRST_HALF', 'SECOND_HALF'))
);

CREATE INDEX idx_academic_group_choice_mapping_active
    ON academic_group_choice_mapping (academic_group_id, is_active);

ALTER TABLE academic_group_subject_mapping
    ADD COLUMN academic_group_choice_mapping_id BIGINT
        REFERENCES academic_group_choice_mapping(academic_group_choice_mapping_id) ON DELETE RESTRICT;

CREATE INDEX idx_group_subject_mapping_choice_mapping
    ON academic_group_subject_mapping (academic_group_choice_mapping_id);

ALTER TABLE timetable_entry
    ADD COLUMN academic_group_choice_mapping_id BIGINT
        REFERENCES academic_group_choice_mapping(academic_group_choice_mapping_id) ON DELETE RESTRICT;

ALTER TABLE timetable_entry
    DROP CONSTRAINT IF EXISTS uk_timetable_entry_slot;

DROP INDEX IF EXISTS uk_timetable_entry_group_slot;

CREATE UNIQUE INDEX uk_timetable_entry_section_single_slot
    ON timetable_entry (timetable_version_id, section_id, day_of_week, timetable_period_id)
    WHERE section_id IS NOT NULL AND academic_group_choice_mapping_id IS NULL;

CREATE UNIQUE INDEX uk_timetable_entry_section_choice_slot_subject
    ON timetable_entry (
        timetable_version_id, section_id, day_of_week, timetable_period_id,
        academic_group_choice_mapping_id, subject_id
    )
    WHERE section_id IS NOT NULL AND academic_group_choice_mapping_id IS NOT NULL;

CREATE UNIQUE INDEX uk_timetable_entry_group_single_slot
    ON timetable_entry (timetable_version_id, academic_group_id, day_of_week, timetable_period_id)
    WHERE section_id IS NULL AND academic_group_id IS NOT NULL
      AND academic_group_choice_mapping_id IS NULL;

CREATE UNIQUE INDEX uk_timetable_entry_group_choice_slot_subject
    ON timetable_entry (
        timetable_version_id, academic_group_id, day_of_week, timetable_period_id,
        academic_group_choice_mapping_id, subject_id
    )
    WHERE section_id IS NULL AND academic_group_id IS NOT NULL
      AND academic_group_choice_mapping_id IS NOT NULL;

CREATE FUNCTION prevent_unrelated_choice_slot_sharing()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    lock_key TEXT;
BEGIN
    lock_key := TG_TABLE_SCHEMA || ':' || NEW.timetable_version_id || ':'
        || COALESCE(NEW.section_id::TEXT, 'group-' || NEW.academic_group_id::TEXT) || ':'
        || NEW.day_of_week || ':' || NEW.timetable_period_id;
    PERFORM pg_advisory_xact_lock(hashtext(TG_TABLE_SCHEMA), hashtext(lock_key));

    IF EXISTS (
        SELECT 1
        FROM timetable_entry existing
        WHERE existing.timetable_entry_id IS DISTINCT FROM NEW.timetable_entry_id
          AND existing.timetable_version_id = NEW.timetable_version_id
          AND existing.section_id IS NOT DISTINCT FROM NEW.section_id
          AND existing.academic_group_id IS NOT DISTINCT FROM NEW.academic_group_id
          AND existing.day_of_week = NEW.day_of_week
          AND existing.timetable_period_id = NEW.timetable_period_id
          AND NOT (
              NEW.academic_group_choice_mapping_id IS NOT NULL
              AND existing.academic_group_choice_mapping_id = NEW.academic_group_choice_mapping_id
              AND existing.subject_id IS NOT NULL
              AND NEW.subject_id IS NOT NULL
              AND existing.subject_id <> NEW.subject_id
          )
    ) THEN
        RAISE EXCEPTION 'Timetable slot is already occupied by an unrelated lesson'
            USING ERRCODE = '23505';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_timetable_entry_choice_slot
    BEFORE INSERT OR UPDATE
    ON timetable_entry
    FOR EACH ROW EXECUTE FUNCTION prevent_unrelated_choice_slot_sharing();

-- Convert legacy text labels conservatively: one distinct reusable group per academic
-- group and label. Never infer reuse across academic groups or years.
DO $$
DECLARE
    legacy RECORD;
    new_group_id BIGINT;
    new_mapping_id BIGINT;
    generated_name VARCHAR(100);
BEGIN
    FOR legacy IN
        SELECT m.academic_group_id,
               LOWER(TRIM(m.choice_group)) AS normalized_label,
               MIN(TRIM(m.choice_group)) AS display_label,
               MIN(g.code) AS academic_group_code,
               MIN(y.name) AS academic_year_name,
               MIN(s.subject_type) AS subject_type,
               COUNT(DISTINCT s.subject_type) AS subject_type_count,
               COUNT(*) AS member_count,
               MAX(m.weekly_periods) AS weekly_periods,
               ROW_NUMBER() OVER (
                   PARTITION BY m.academic_group_id
                   ORDER BY LOWER(TRIM(m.choice_group))
               ) AS legacy_ordinal
        FROM academic_group_subject_mapping m
        JOIN academic_group g ON g.academic_group_id = m.academic_group_id
        JOIN academic_year y ON y.academic_year_id = g.academic_year_id
        JOIN subject s ON s.subject_id = m.subject_id
        WHERE m.is_active = TRUE
          AND m.requirement_type = 'OPTIONAL'
          AND m.choice_group IS NOT NULL
          AND TRIM(m.choice_group) <> ''
        GROUP BY m.academic_group_id, LOWER(TRIM(m.choice_group))
        HAVING COUNT(*) >= 2 AND COUNT(DISTINCT s.subject_type) = 1
    LOOP
        generated_name := LEFT(legacy.display_label, 24) || ' ('
            || LEFT(legacy.academic_group_code, 8) || ' '
            || LEFT(legacy.academic_year_name, 8) || ' #'
            || legacy.academic_group_id || '-' || legacy.legacy_ordinal || ')';

        INSERT INTO subject_choice_group (
            name, subject_type, is_active, created_by, created_on, updated_by, updated_on, version
        ) VALUES (
            generated_name, legacy.subject_type, TRUE,
            'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, 0
        ) RETURNING subject_choice_group_id INTO new_group_id;

        INSERT INTO academic_group_choice_mapping (
            academic_group_id, subject_choice_group_id, weekly_periods, timetable_preference,
            is_active, created_by, created_on, updated_by, updated_on, version
        ) VALUES (
            legacy.academic_group_id, new_group_id, legacy.weekly_periods, 'ANY',
            TRUE, 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, 0
        ) RETURNING academic_group_choice_mapping_id INTO new_mapping_id;

        INSERT INTO subject_choice_group_member (
            subject_choice_group_id, subject_id, created_by, created_on, updated_by, updated_on, version
        )
        SELECT new_group_id, m.subject_id, 'migration', CURRENT_TIMESTAMP, 'migration', CURRENT_TIMESTAMP, 0
        FROM academic_group_subject_mapping m
        WHERE m.academic_group_id = legacy.academic_group_id
          AND m.is_active = TRUE
          AND m.requirement_type = 'OPTIONAL'
          AND LOWER(TRIM(m.choice_group)) = legacy.normalized_label;

        UPDATE academic_group_subject_mapping m
        SET academic_group_choice_mapping_id = new_mapping_id
        WHERE m.academic_group_id = legacy.academic_group_id
          AND m.is_active = TRUE
          AND m.requirement_type = 'OPTIONAL'
          AND LOWER(TRIM(m.choice_group)) = legacy.normalized_label;
    END LOOP;
END $$;
