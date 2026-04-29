-- ============================================================================
-- Multi-Tenant Authentication Enhancement - Phase 1
-- User-Tenant Mapping Table Creation
-- ============================================================================

-- Create the global user-tenant mapping table in public schema
CREATE TABLE IF NOT EXISTS public.user_tenant_mapping (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255) UNIQUE NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_username UNIQUE (username)
);

-- Create indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_user_tenant_email ON public.user_tenant_mapping(email);
CREATE INDEX IF NOT EXISTS idx_user_tenant_username ON public.user_tenant_mapping(username);
CREATE INDEX IF NOT EXISTS idx_user_tenant_tenant_id ON public.user_tenant_mapping(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_tenant_active ON public.user_tenant_mapping(is_active);

-- Add comment for documentation
COMMENT ON TABLE public.user_tenant_mapping IS 'Global mapping of users to their respective tenants for automatic tenant detection during login';
COMMENT ON COLUMN public.user_tenant_mapping.email IS 'User email address (must be unique across all tenants)';
COMMENT ON COLUMN public.user_tenant_mapping.username IS 'Username (must be unique across all tenants)';
COMMENT ON COLUMN public.user_tenant_mapping.tenant_id IS 'Database schema/tenant identifier';
COMMENT ON COLUMN public.user_tenant_mapping.is_active IS 'Flag to enable/disable user access without deleting the record';

-- Create trigger function to keep mapping in sync when users are created/updated
CREATE OR REPLACE FUNCTION public.sync_user_tenant_mapping()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert or update the mapping table
    INSERT INTO public.user_tenant_mapping (email, username, tenant_id)
    VALUES (NEW.email, NEW.user_name, current_schema())
    ON CONFLICT (email) DO UPDATE 
    SET username = EXCLUDED.username,
        tenant_id = EXCLUDED.tenant_id,
        updated_at = CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.sync_user_tenant_mapping() IS 'Trigger function to automatically sync user changes to the global mapping table';
