-- Academic Groups: the academic_class table becomes academic_group; classification (stage) is optional;
-- groups gain a description. Existing rows and foreign keys are preserved (Postgres follows renames).

ALTER TABLE academic_class RENAME TO academic_group;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_academic_class_year_code'
               AND conrelid = format('%I.academic_group', current_schema())::regclass) THEN
        ALTER TABLE academic_group RENAME CONSTRAINT uk_academic_class_year_code TO uk_academic_group_year_code;
    END IF;
END $$;

ALTER INDEX IF EXISTS idx_academic_class_year_active RENAME TO idx_academic_group_year_active;
ALTER INDEX IF EXISTS idx_academic_class_stage RENAME TO idx_academic_group_stage;

ALTER TABLE class_subject_mapping RENAME COLUMN academic_class_id TO academic_group_id;
ALTER INDEX IF EXISTS idx_csm_class_active RENAME TO idx_csm_group_active;

ALTER TABLE academic_group ALTER COLUMN stage DROP NOT NULL;

ALTER TABLE academic_group ADD COLUMN IF NOT EXISTS description VARCHAR(500);

-- Rollback (run in reverse order):
--   ALTER TABLE academic_group DROP COLUMN IF EXISTS description;
--   ALTER TABLE academic_group ALTER COLUMN stage SET NOT NULL;   -- fails if NULL stages exist
--   ALTER INDEX IF EXISTS idx_csm_group_active RENAME TO idx_csm_class_active;
--   ALTER TABLE class_subject_mapping RENAME COLUMN academic_group_id TO academic_class_id;
--   ALTER INDEX IF EXISTS idx_academic_group_stage RENAME TO idx_academic_class_stage;
--   ALTER INDEX IF EXISTS idx_academic_group_year_active RENAME TO idx_academic_class_year_active;
--   ALTER TABLE academic_group RENAME CONSTRAINT uk_academic_group_year_code TO uk_academic_class_year_code;
--   ALTER TABLE academic_group RENAME TO academic_class;
