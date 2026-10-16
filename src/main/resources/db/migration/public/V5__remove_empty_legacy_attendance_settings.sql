DO $$
BEGIN
    IF to_regclass(format('%I.attendance_setting', current_schema())) IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM attendance_setting) THEN
            RAISE EXCEPTION 'Public attendance settings contain data; migrate to the owning tenant before cleanup';
        END IF;
        DROP TABLE attendance_setting;
    END IF;
END $$;