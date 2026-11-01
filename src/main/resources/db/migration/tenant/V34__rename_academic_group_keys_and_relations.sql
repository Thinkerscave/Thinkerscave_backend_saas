-- Rename legacy Class identifiers and relationship tables to Academic Group terminology.
-- Rows are retained. PostgreSQL updates existing FK definitions when a referenced column is renamed.

DO $$
DECLARE
    relation_name TEXT;
    old_column TEXT;
    new_column TEXT;
BEGIN
    IF to_regclass(current_schema() || '.academic_group') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                   AND table_name = 'academic_group' AND column_name = 'class_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                       AND table_name = 'academic_group' AND column_name = 'academic_group_id') THEN
        ALTER TABLE academic_group RENAME COLUMN class_id TO academic_group_id;
    END IF;

    IF to_regclass(current_schema() || '.academic_group_subject_mapping') IS NULL
       AND to_regclass(current_schema() || '.class_subject_mapping') IS NOT NULL THEN
        ALTER TABLE class_subject_mapping RENAME TO academic_group_subject_mapping;
    END IF;
    IF to_regclass(current_schema() || '.academic_group_subject_mapping') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                   AND table_name = 'academic_group_subject_mapping' AND column_name = 'class_subject_mapping_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                       AND table_name = 'academic_group_subject_mapping' AND column_name = 'academic_group_subject_mapping_id') THEN
        ALTER TABLE academic_group_subject_mapping
            RENAME COLUMN class_subject_mapping_id TO academic_group_subject_mapping_id;
    END IF;

    IF to_regclass(current_schema() || '.academic_calendar_event_group') IS NULL
       AND to_regclass(current_schema() || '.academic_calendar_event_class') IS NOT NULL THEN
        ALTER TABLE academic_calendar_event_class RENAME TO academic_calendar_event_group;
    END IF;

    IF to_regclass(current_schema() || '.section_teacher_assignment') IS NULL
       AND to_regclass(current_schema() || '.class_teacher_assignment') IS NOT NULL THEN
        ALTER TABLE class_teacher_assignment RENAME TO section_teacher_assignment;
    END IF;

    IF to_regclass(current_schema() || '.section_teacher_assignment') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                   AND table_name = 'section_teacher_assignment' AND column_name = 'class_teacher_assignment_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                       AND table_name = 'section_teacher_assignment' AND column_name = 'section_teacher_assignment_id') THEN
        ALTER TABLE section_teacher_assignment
            RENAME COLUMN class_teacher_assignment_id TO section_teacher_assignment_id;
    END IF;

    FOR relation_name, old_column, new_column IN
        SELECT * FROM (VALUES
            ('academic_section', 'class_id', 'academic_group_id'),
            ('academic_calendar_event_group', 'class_id', 'academic_group_id'),
            ('teacher_allocation', 'class_subject_mapping_id', 'academic_group_subject_mapping_id'),
            ('academic_year_transition', 'copy_classes', 'copy_academic_groups')
        ) AS renames(table_name, old_name, new_name)
    LOOP
        IF to_regclass(current_schema() || '.' || relation_name) IS NOT NULL
           AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                       AND table_name = relation_name AND column_name = old_column)
           AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                           AND table_name = relation_name AND column_name = new_column) THEN
            EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', relation_name, old_column, new_column);
        END IF;
    END LOOP;

    IF to_regclass(current_schema() || '.academic_group_subject_mapping') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                   AND table_name = 'academic_group_subject_mapping' AND column_name = 'academic_class_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                       AND table_name = 'academic_group_subject_mapping' AND column_name = 'academic_group_id') THEN
        ALTER TABLE academic_group_subject_mapping RENAME COLUMN academic_class_id TO academic_group_id;
    END IF;
END $$;

-- Rename indexes whose names exposed the previous domain terminology.
ALTER INDEX IF EXISTS uk_academic_class_id_year RENAME TO uk_academic_group_id_year;
ALTER INDEX IF EXISTS uk_academic_section_class_code RENAME TO uk_academic_section_group_code;
ALTER INDEX IF EXISTS idx_academic_section_class_active RENAME TO idx_academic_section_group_active;
ALTER INDEX IF EXISTS uk_csm_class_subject RENAME TO uk_group_subject_mapping_subject;
ALTER INDEX IF EXISTS idx_csm_class_active RENAME TO idx_group_subject_mapping_active;
ALTER INDEX IF EXISTS uk_ace_class RENAME TO uk_ace_group;
ALTER INDEX IF EXISTS idx_ace_class_event RENAME TO idx_ace_group_event;
ALTER INDEX IF EXISTS idx_ace_class_class RENAME TO idx_ace_group_group;
ALTER INDEX IF EXISTS idx_cta_section_effective RENAME TO idx_sta_section_effective;
ALTER INDEX IF EXISTS idx_cta_staff_effective RENAME TO idx_sta_staff_effective;
ALTER INDEX IF EXISTS idx_cta_active RENAME TO idx_sta_active;
ALTER INDEX IF EXISTS idx_csm_subject RENAME TO idx_group_subject_mapping_subject;

DO $$
BEGIN
    IF to_regclass(current_schema() || '.academic_calendar_event') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                   AND table_name = 'academic_calendar_event' AND column_name = 'audience_type') THEN
        UPDATE academic_calendar_event SET audience_type = 'ACADEMIC_GROUP' WHERE audience_type = 'CLASS';
    END IF;
    IF to_regclass(current_schema() || '.timetable_conflict') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema()
                   AND table_name = 'timetable_conflict' AND column_name = 'conflict_type') THEN
        UPDATE timetable_conflict SET conflict_type = 'ACADEMIC_GROUP_CONFLICT' WHERE conflict_type = 'CLASS_CONFLICT';
    END IF;
END $$;

UPDATE menus
SET menu_name = 'Academic Groups',
    description = 'Academic Groups, sections, and student distribution',
    route = '/app/academics/academic-groups'
WHERE menu_code = 'ACADEMICS_CLASSES';

UPDATE menus
SET menu_name = 'My Academic Groups',
    route = '/app/academics/my-academic-groups'
WHERE menu_code = 'ACADEMICS_MY_CLASSES';
