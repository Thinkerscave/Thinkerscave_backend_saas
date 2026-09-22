-- Staff attendance regularization requests + immutable decision audit.
CREATE TABLE IF NOT EXISTS staff_attendance_regularization (
    request_id              BIGSERIAL PRIMARY KEY,
    organization_id         BIGINT NOT NULL,
    staff_id                BIGINT NOT NULL,
    staff_name              VARCHAR(200) NOT NULL,
    department              VARCHAR(100),
    attendance_id           BIGINT,
    attendance_date         DATE NOT NULL,
    requested_status        VARCHAR(20) NOT NULL,
    requested_sign_in_time  TIMESTAMP,
    requested_sign_out_time TIMESTAMP,
    requested_working_minutes INTEGER,
    reason                  VARCHAR(500) NOT NULL,
    remarks                 VARCHAR(1000),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_by            VARCHAR(150) NOT NULL,
    requested_at            TIMESTAMP NOT NULL,
    decision_by             VARCHAR(150),
    decision_at             TIMESTAMP,
    decision_comment        VARCHAR(1000),
    previous_status         VARCHAR(20),
    previous_sign_in_time   TIMESTAMP,
    previous_sign_out_time  TIMESTAMP,
    previous_working_minutes INTEGER,
    created_by              VARCHAR(100),
    created_on              TIMESTAMP,
    updated_by              VARCHAR(100),
    updated_on              TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sar_pending_per_day
    ON staff_attendance_regularization (organization_id, staff_id, attendance_date)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_sar_org_staff ON staff_attendance_regularization (organization_id, staff_id);
CREATE INDEX IF NOT EXISTS idx_sar_org_status ON staff_attendance_regularization (organization_id, status);
CREATE INDEX IF NOT EXISTS idx_sar_org_date ON staff_attendance_regularization (organization_id, attendance_date);

CREATE TABLE IF NOT EXISTS staff_attendance_regularization_audit (
    audit_id                BIGSERIAL PRIMARY KEY,
    organization_id         BIGINT NOT NULL,
    request_id              BIGINT NOT NULL,
    staff_id                BIGINT NOT NULL,
    staff_name              VARCHAR(200),
    attendance_date         DATE NOT NULL,
    previous_status         VARCHAR(20),
    previous_sign_in_time   TIMESTAMP,
    previous_sign_out_time  TIMESTAMP,
    requested_status        VARCHAR(20),
    requested_sign_in_time  TIMESTAMP,
    requested_sign_out_time TIMESTAMP,
    action                  VARCHAR(20) NOT NULL,
    comment                 VARCHAR(1000) NOT NULL,
    action_by               VARCHAR(150) NOT NULL,
    action_at               TIMESTAMP NOT NULL,
    created_by              VARCHAR(100),
    created_on              TIMESTAMP,
    updated_by              VARCHAR(100),
    updated_on              TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sara_request ON staff_attendance_regularization_audit (request_id);
CREATE INDEX IF NOT EXISTS idx_sara_org ON staff_attendance_regularization_audit (organization_id);
