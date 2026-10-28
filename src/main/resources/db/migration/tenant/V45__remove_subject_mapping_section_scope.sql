-- Existing curriculum mappings become group-wide; historical teacher and timetable records remain unchanged.
DROP TABLE IF EXISTS academic_group_subject_mapping_section;

ALTER TABLE academic_group_subject_mapping
    DROP COLUMN IF EXISTS scope_type;
