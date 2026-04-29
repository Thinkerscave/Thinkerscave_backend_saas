-- ============================================================================
-- Organization Audit Log Table
-- Migration: V1_5__create_organization_audit_log.sql
-- 
-- Purpose: Create audit logging table for tracking organization operations
-- ============================================================================

-- Create the audit log table in each tenant schema
CREATE TABLE IF NOT EXISTS organization_audit_log (
    id BIGSERIAL PRIMARY KEY,
    
    -- Context
    tenant_id VARCHAR(100),
    organization_id BIGINT,
    
    -- Action details
    action VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    
    -- Who and when
    performed_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Request details
    details JSONB DEFAULT '{}',
    
    -- Result
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    duration_ms BIGINT
);

-- Add comments
COMMENT ON TABLE organization_audit_log IS 'Audit trail for organization operations';
COMMENT ON COLUMN organization_audit_log.tenant_id IS 'Tenant schema where operation occurred';
COMMENT ON COLUMN organization_audit_log.organization_id IS 'Organization within tenant (if applicable)';
COMMENT ON COLUMN organization_audit_log.action IS 'Action type (e.g., CREATE_STUDENT, UPDATE_ORG)';
COMMENT ON COLUMN organization_audit_log.performed_by IS 'Username who performed the action';
COMMENT ON COLUMN organization_audit_log.details IS 'JSON payload with operation details';
COMMENT ON COLUMN organization_audit_log.status IS 'SUCCESS or FAILED';
COMMENT ON COLUMN organization_audit_log.duration_ms IS 'Operation duration in milliseconds';

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant 
    ON organization_audit_log(tenant_id);

CREATE INDEX IF NOT EXISTS idx_audit_log_org 
    ON organization_audit_log(organization_id);

CREATE INDEX IF NOT EXISTS idx_audit_log_action 
    ON organization_audit_log(action);

CREATE INDEX IF NOT EXISTS idx_audit_log_performed_by 
    ON organization_audit_log(performed_by);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at 
    ON organization_audit_log(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_status 
    ON organization_audit_log(status);

-- Composite index for common query pattern
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_org_date 
    ON organization_audit_log(tenant_id, organization_id, created_at DESC);

-- ============================================================================
-- Partition by month for better performance (optional - for production)
-- Uncomment if you expect high audit log volume
-- ============================================================================

-- CREATE TABLE organization_audit_log (
--     id BIGSERIAL,
--     tenant_id VARCHAR(100),
--     ...
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- ) PARTITION BY RANGE (created_at);

-- ============================================================================
