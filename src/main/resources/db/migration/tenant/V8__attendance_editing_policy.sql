ALTER TABLE IF EXISTS attendance_setting
    ADD COLUMN IF NOT EXISTS attendance_editing_policy VARCHAR(40);

DO $$
BEGIN
    IF to_regclass(format('%I.attendance_setting', current_schema())) IS NOT NULL THEN
        UPDATE attendance_setting
        SET attendance_editing_policy = 'EDITABLE_UNTIL_SUBMISSION_DAY'
        WHERE attendance_editing_policy IS NULL;
    END IF;
END $$;

ALTER TABLE IF EXISTS attendance_setting
    ALTER COLUMN attendance_editing_policy SET DEFAULT 'EDITABLE_UNTIL_SUBMISSION_DAY';

ALTER TABLE IF EXISTS attendance_setting
    ALTER COLUMN attendance_editing_policy SET NOT NULL;
