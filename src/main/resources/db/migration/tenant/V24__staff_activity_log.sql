-- V24: Staff activity log (audit trail shown on the staff profile Activity tab).
--  * one row per recorded change; section holds the profile area that changed (nullable)
--  * the schema itself is the tenant boundary; no organization_id column.

CREATE TABLE IF NOT EXISTS staff_activity_log (
    activity_id   BIGSERIAL      PRIMARY KEY,
    staff_id      BIGINT         NOT NULL REFERENCES staff (staff_id),
    action_type   VARCHAR(40)    NOT NULL,
    section       VARCHAR(60),
    description   VARCHAR(500)   NOT NULL,
    performed_by  VARCHAR(255),
    performed_on  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_staff_activity_log_staff
    ON staff_activity_log (staff_id, performed_on DESC);
