-- Take a per-tenant schema backup before release execution. PostgreSQL Flyway
-- runs this migration transactionally: any failed validation rolls back the
-- copied rows and leaves attendance_setting intact. Flyway has no automatic
-- down migration; post-success recovery is restore the schema backup, then
-- roll back the application release.
CREATE TABLE IF NOT EXISTS module_setting (
    setting_id BIGSERIAL PRIMARY KEY,
    module_code VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_module_setting_scope UNIQUE (module_code, setting_key),
    CONSTRAINT ck_module_setting_attendance_mode CHECK (
        module_code <> 'ATTENDANCE' OR setting_key <> 'ATTENDANCE_MODE'
        OR setting_value = 'DAILY'
    ),
    CONSTRAINT ck_module_setting_editing_policy CHECK (
        module_code <> 'ATTENDANCE' OR setting_key <> 'ATTENDANCE_EDITING_POLICY'
        OR setting_value IN ('SAME_DAY', 'SAME_WEEK', 'SAME_MONTH')
    ),
    CONSTRAINT ck_module_setting_threshold CHECK (
        module_code <> 'ATTENDANCE' OR setting_key <> 'LOW_ATTENDANCE_THRESHOLD'
        OR setting_value ~ '^(100|[0-9]{1,2})$'
    ),
    CONSTRAINT ck_module_setting_late_grace CHECK (
        module_code <> 'ATTENDANCE' OR setting_key <> 'STAFF_LATE_GRACE_MINUTES'
        OR setting_value ~ '^(120|1[01][0-9]|[1-9]?[0-9])$'
    ),
    CONSTRAINT ck_module_setting_working_hours CHECK (
        module_code <> 'ATTENDANCE' OR setting_key <> 'MINIMUM_WORKING_HOURS'
        OR setting_value ~ '^(24|([1-9]|1[0-9]|2[0-3])([.]5)?)$'
    ),
    CONSTRAINT ck_module_setting_copy_previous CHECK (
        module_code <> 'ATTENDANCE' OR setting_key <> 'ALLOW_COPY_PREVIOUS'
        OR setting_value IN ('true', 'false')
    ),
    CONSTRAINT ck_module_setting_freeze_days CHECK (
        module_code <> 'ATTENDANCE' OR setting_key <> 'FREEZE_AFTER_DAYS'
        OR setting_value ~ '^(0|[1-9][0-9]{0,9})$'
    ),
    CONSTRAINT ck_module_setting_time CHECK (
        module_code <> 'ATTENDANCE' OR setting_key NOT IN ('ATTENDANCE_WINDOW_START', 'ATTENDANCE_WINDOW_END')
        OR setting_value ~ '^([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9]([.][0-9]{1,9})?)?$'
    )
);

CREATE INDEX IF NOT EXISTS idx_module_setting_module
    ON module_setting (module_code);

DO $$
BEGIN
    IF to_regclass(format('%I.%I', current_schema(), 'attendance_setting')) IS NOT NULL THEN
        IF (SELECT COUNT(*) FROM attendance_setting) > 1 THEN
            RAISE EXCEPTION 'Multiple organization settings in one tenant schema; migration refused';
        END IF;
        IF EXISTS (
            SELECT 1 FROM attendance_setting
            WHERE min_student_attendance_percent IS NOT NULL
              AND min_student_attendance_percent NOT BETWEEN 0 AND 100
        ) OR EXISTS (
            SELECT 1 FROM attendance_setting
            WHERE staff_late_grace_minutes IS NOT NULL
              AND staff_late_grace_minutes NOT BETWEEN 0 AND 120
        ) OR EXISTS (
            SELECT 1 FROM attendance_setting
            WHERE min_staff_working_hours IS NOT NULL
              AND min_staff_working_hours NOT BETWEEN 1 AND 24
        ) THEN
            RAISE EXCEPTION 'Attendance settings contain values outside the new validated ranges; correct them before migration';
        END IF;

        INSERT INTO module_setting (
            module_code, setting_key, setting_value, version
        )
        SELECT 'ATTENDANCE', 'ATTENDANCE_MODE', 'DAILY', 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'LOW_ATTENDANCE_THRESHOLD',
               COALESCE(min_student_attendance_percent, 75)::text, 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'ATTENDANCE_EDITING_POLICY',
               CASE
                   WHEN attendance_editing_policy IN ('SAME_DAY', 'SAME_WEEK', 'SAME_MONTH') THEN attendance_editing_policy
                   ELSE 'SAME_DAY'
               END, 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'STAFF_LATE_GRACE_MINUTES',
             COALESCE(staff_late_grace_minutes, 15)::text, 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'MINIMUM_WORKING_HOURS',
               COALESCE(min_staff_working_hours, 8)::text, 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'ALLOW_COPY_PREVIOUS',
               COALESCE(allow_copy_previous, true)::text, 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'FREEZE_AFTER_DAYS',
               COALESCE(freeze_after_days, 0)::text, 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'ATTENDANCE_WINDOW_START',
               COALESCE(to_char(window_start_time, 'HH24:MI:SS'), '09:00:00'), 0
        FROM attendance_setting
        UNION ALL
        SELECT 'ATTENDANCE', 'ATTENDANCE_WINDOW_END',
               COALESCE(to_char(window_end_time, 'HH24:MI:SS'), '17:00:00'), 0
        FROM attendance_setting
        ON CONFLICT (module_code, setting_key) DO NOTHING;

        IF EXISTS (
            SELECT 1
            FROM attendance_setting legacy
            WHERE (
                SELECT COUNT(*)
                FROM module_setting setting
                                WHERE setting.module_code = 'ATTENDANCE'
                  AND setting.setting_key IN (
                      'ATTENDANCE_MODE', 'LOW_ATTENDANCE_THRESHOLD', 'ATTENDANCE_EDITING_POLICY',
                      'STAFF_LATE_GRACE_MINUTES', 'MINIMUM_WORKING_HOURS', 'ALLOW_COPY_PREVIOUS',
                      'FREEZE_AFTER_DAYS', 'ATTENDANCE_WINDOW_START', 'ATTENDANCE_WINDOW_END'
                  )
            ) <> 9
        ) THEN
            RAISE EXCEPTION 'Attendance settings migration verification failed';
        END IF;

        DROP TABLE attendance_setting;
    END IF;
END $$;