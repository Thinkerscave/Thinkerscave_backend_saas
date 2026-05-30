-- ============================================================================
-- V1_16: Create Activity Log Table
-- Migration: V1_16__create_activity_log.sql
--
-- Purpose: Lightweight activity feed for dashboard widgets.
--          Records business events like "Student Created", "Fee Paid", etc.
-- NOTE: This migration runs in TENANT schemas
-- ============================================================================

CREATE TABLE IF NOT EXISTS activity_log (
    id              BIGSERIAL       PRIMARY KEY,
    organization_id BIGINT          NOT NULL,
    entity_type     VARCHAR(64)     NOT NULL
        COMMENT 'STUDENT, INQUIRY, FEE, ATTENDANCE, STAFF, ADMISSION, EXAM, etc.',
    entity_id       BIGINT,
    action          VARCHAR(128)    NOT NULL
        COMMENT 'Human-readable action: Student Created, Fee Paid, etc.',
    description     VARCHAR(500),
    performed_by    VARCHAR(100)    NOT NULL,
    performed_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata        JSONB           COMMENT 'Additional context as JSON',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT          NOT NULL DEFAULT 0
);

-- Comments on table and columns
COMMENT ON TABLE activity_log IS 'Lightweight business activity feed for dashboard widgets';
COMMENT ON COLUMN activity_log.entity_type IS 'Domain entity type: STUDENT, INQUIRY, FEE, ATTENDANCE, STAFF, ADMISSION, EXAM';
COMMENT ON COLUMN activity_log.action IS 'Human-readable action description';
COMMENT ON COLUMN activity_log.metadata IS 'Additional context as JSON (e.g., old/new values, amounts)';

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_activity_log_org
    ON activity_log(organization_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_org_time
    ON activity_log(organization_id, performed_at DESC);
CREATE INDEX IF NOT EXISTS idx_activity_log_entity
    ON activity_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_activity_log_performer
    ON activity_log(performed_by);
CREATE INDEX IF NOT EXISTS idx_activity_log_type_time
    ON activity_log(entity_type, performed_at DESC);
