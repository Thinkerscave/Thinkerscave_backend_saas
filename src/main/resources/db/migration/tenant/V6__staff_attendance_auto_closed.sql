-- Persist 24-hour auto sign-out on staff attendance sessions.
ALTER TABLE IF EXISTS staff_attendance
    ADD COLUMN IF NOT EXISTS auto_closed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_stfa_active_signin
    ON staff_attendance (sign_in_time)
    WHERE sign_out_time IS NULL;
