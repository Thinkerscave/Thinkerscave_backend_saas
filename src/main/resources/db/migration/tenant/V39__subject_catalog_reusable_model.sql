-- V39: Subjects become a reusable organization-level catalog.
-- A subject no longer carries an academic year, default weekly periods, a timetable preference, or the legacy category.
-- Weekly periods, requirement, scope, and timetable preference are already stored on academic_group_subject_mapping.
-- Per-year duplicate subject rows (V36/V38) are merged into one catalog row per code and every reference is repointed.
-- The migration aborts (and rolls back) rather than discarding data when the merge would create duplicate mappings or names.

-- Snapshot of the retired subject columns, kept for audit and recovery. Drop once the migration is verified.
CREATE TABLE IF NOT EXISTS subject_legacy_snapshot_v39 AS
SELECT subject_id,
       academic_year_id,
       category,
       subject_type,
       default_weekly_periods,
       timetable_preference,
       is_catalog,
       CURRENT_TIMESTAMP AS captured_at
FROM subject;

CREATE TEMP TABLE subject_merge_map AS
SELECT d.subject_id AS duplicate_subject_id,
       c.subject_id AS canonical_subject_id
FROM subject d
JOIN subject c ON LOWER(c.code) = LOWER(d.code) AND c.is_catalog
WHERE NOT d.is_catalog;

DO $$
DECLARE
    conflicts INT;
    duplicate_names INT;
BEGIN
    SELECT COUNT(*) INTO conflicts
    FROM (
        SELECT m.academic_group_id, COALESCE(d.canonical_subject_id, m.subject_id) AS canonical_id
        FROM academic_group_subject_mapping m
        LEFT JOIN subject_merge_map d ON d.duplicate_subject_id = m.subject_id
        GROUP BY m.academic_group_id, COALESCE(d.canonical_subject_id, m.subject_id)
        HAVING COUNT(*) > 1
    ) x;
    IF conflicts > 0 THEN
        RAISE EXCEPTION 'V39 aborted: % academic group(s) would hold duplicate mappings after merging duplicate subjects. Resolve them and re-run.', conflicts;
    END IF;

    SELECT COUNT(*) INTO duplicate_names
    FROM (
        SELECT LOWER(name) FROM subject WHERE is_catalog GROUP BY LOWER(name) HAVING COUNT(*) > 1
    ) x;
    IF duplicate_names > 0 THEN
        RAISE EXCEPTION 'V39 aborted: % subject name(s) are duplicated across records. Rename one of each pair and re-run.', duplicate_names;
    END IF;
END $$;

UPDATE academic_group_subject_mapping m
SET subject_id = d.canonical_subject_id
FROM subject_merge_map d
WHERE m.subject_id = d.duplicate_subject_id;

DO $$
DECLARE
    r RECORD;
    subject_oid OID := to_regclass(current_schema() || '.subject');
BEGIN
    FOR r IN
        SELECT c.conrelid::regclass::text AS referencing_table, a.attname AS referencing_column
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
        WHERE c.contype = 'f'
          AND c.confrelid = subject_oid
          AND array_length(c.conkey, 1) = 1
    LOOP
        EXECUTE format(
            'UPDATE %s t SET %I = d.canonical_subject_id FROM subject_merge_map d WHERE t.%I = d.duplicate_subject_id',
            r.referencing_table, r.referencing_column, r.referencing_column);
    END LOOP;
END $$;

DELETE FROM subject s
USING subject_merge_map d
WHERE s.subject_id = d.duplicate_subject_id;

DROP TRIGGER IF EXISTS trg_subject_legacy_columns ON subject;
DROP FUNCTION IF EXISTS set_subject_legacy_columns();
DROP INDEX IF EXISTS uk_subject_catalog_code;
DROP INDEX IF EXISTS idx_subject_catalog_active_name;

ALTER TABLE subject
    DROP COLUMN IF EXISTS academic_year_id,
    DROP COLUMN IF EXISTS category,
    DROP COLUMN IF EXISTS default_weekly_periods,
    DROP COLUMN IF EXISTS timetable_preference,
    DROP COLUMN IF EXISTS is_catalog,
    DROP COLUMN IF EXISTS subject_code,
    DROP COLUMN IF EXISTS subject_name,
    DROP COLUMN IF EXISTS active,
    DROP COLUMN IF EXISTS remarks;

-- Organization-wide uniqueness, case-insensitive, including inactive subjects.
CREATE UNIQUE INDEX IF NOT EXISTS uk_subject_code ON subject (LOWER(code));
CREATE UNIQUE INDEX IF NOT EXISTS uk_subject_name ON subject (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_subject_active_name ON subject (is_active, LOWER(name));

-- Mapping integrity: weekly periods must be a supported positive value; requirement and scope use stable domain values.
ALTER TABLE academic_group_subject_mapping
    ADD CONSTRAINT chk_group_subject_mapping_weekly_periods CHECK (weekly_periods BETWEEN 1 AND 40),
    ADD CONSTRAINT chk_group_subject_mapping_requirement CHECK (requirement_type IN ('COMPULSORY', 'OPTIONAL')),
    ADD CONSTRAINT chk_group_subject_mapping_scope CHECK (scope_type IN ('ALL_SECTIONS', 'SELECTED_SECTIONS'));

DROP TABLE IF EXISTS subject_merge_map;

UPDATE menus
SET menu_name = 'Subjects',
    description = 'Reusable subjects and learning activities',
    route = '/app/academics/subjects'
WHERE menu_code = 'ACADEMICS_SUBJECTS';

-- Rollback (manual; run in reverse order, then restore columns from subject_legacy_snapshot_v39 if needed):
--   ALTER TABLE academic_group_subject_mapping DROP CONSTRAINT IF EXISTS chk_group_subject_mapping_scope,
--       DROP CONSTRAINT IF EXISTS chk_group_subject_mapping_requirement,
--       DROP CONSTRAINT IF EXISTS chk_group_subject_mapping_weekly_periods;
--   DROP INDEX IF EXISTS uk_subject_name; DROP INDEX IF EXISTS uk_subject_code; DROP INDEX IF EXISTS idx_subject_active_name;
--   Re-add academic_year_id, category, default_weekly_periods, timetable_preference, is_catalog from the snapshot table.
--   Merged duplicate subject rows are not restored automatically; their references point to the canonical row.
