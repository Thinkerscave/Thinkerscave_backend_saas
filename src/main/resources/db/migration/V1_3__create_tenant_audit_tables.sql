-- Tenant Configuration Table
-- Stores tenant-level settings, subdomain mappings, and feature flags

CREATE TABLE IF NOT EXISTS public.tenant_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) UNIQUE NOT NULL,
    tenant_name VARCHAR(500),
    subdomain VARCHAR(255) UNIQUE,
    custom_domain VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    max_users INTEGER DEFAULT 100,
    storage_limit_mb INTEGER DEFAULT 10240,  -- 10GB default
    features JSONB DEFAULT '{}'::jsonb,  -- {"sms": true, "email": true, "reports": true}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT tenant_id_format CHECK (tenant_id ~ '^[a-z0-9_]{3,50}$')
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_tenant_config_subdomain ON public.tenant_config(subdomain) WHERE subdomain IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tenant_config_active ON public.tenant_config(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_tenant_config_tenant_id ON public.tenant_config(tenant_id);

-- Comments
COMMENT ON TABLE public.tenant_config IS 'Tenant configuration and feature flags';
COMMENT ON COLUMN public.tenant_config.features IS 'JSON object with feature flags, e.g., {"sms": true, "email": false}';
COMMENT ON COLUMN public.tenant_config.storage_limit_mb IS 'Storage limit in megabytes';

-- Tenant Audit Log Table
-- Comprehensive audit trail for all tenant-related operations

CREATE TABLE IF NOT EXISTS public.tenant_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,  -- CREATED, UPDATED, DELETED, USER_ADDED, SCHEMA_CREATED, ACTIVATED, DEACTIVATED
    performed_by VARCHAR(255),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details JSONB,  -- Additional context about the action
    ip_address VARCHAR(45),
    status VARCHAR(20) DEFAULT 'SUCCESS',  -- SUCCESS, FAILED, PENDING
    error_message TEXT,
    duration_ms INTEGER,  -- Operation duration in milliseconds
    CONSTRAINT valid_status CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING'))
);

-- Indexes for audit queries
CREATE INDEX IF NOT EXISTS idx_audit_tenant ON public.tenant_audit_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON public.tenant_audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON public.tenant_audit_log(performed_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_status ON public.tenant_audit_log(status);
CREATE INDEX IF NOT EXISTS idx_audit_performed_by ON public.tenant_audit_log(performed_by);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_audit_tenant_timestamp ON public.tenant_audit_log(tenant_id, performed_at DESC);

-- Comments
COMMENT ON TABLE public.tenant_audit_log IS 'Audit trail for all tenant operations';
COMMENT ON COLUMN public.tenant_audit_log.action IS 'Type of action performed (CREATED, DELETED, etc.)';
COMMENT ON COLUMN public.tenant_audit_log.details IS 'JSON object with additional context';
COMMENT ON COLUMN public.tenant_audit_log.duration_ms IS 'How long the operation took';

-- Function to update tenant_config.updated_at on modification
CREATE OR REPLACE FUNCTION update_tenant_config_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for auto-updating updated_at
DROP TRIGGER IF EXISTS trigger_update_tenant_config_timestamp ON public.tenant_config;
CREATE TRIGGER trigger_update_tenant_config_timestamp
    BEFORE UPDATE ON public.tenant_config
    FOR EACH ROW
    EXECUTE FUNCTION update_tenant_config_timestamp();

-- Insert default configuration for existing tenants
INSERT INTO public.tenant_config (tenant_id, tenant_name, is_active, created_by)
SELECT 
    schema_name,
    INITCAP(REPLACE(schema_name, '_', ' ')),
    true,
    'MIGRATION'
FROM information_schema.schemata
WHERE schema_name NOT IN ('public', 'pg_catalog', 'information_schema', 'pg_toast')
ON CONFLICT (tenant_id) DO NOTHING;
