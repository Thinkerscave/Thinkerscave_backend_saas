-- ============================================================================
-- Organization-Scoped RBAC Implementation
-- Migration: V1_4__add_organization_scoped_rbac.sql
-- 
-- Purpose: Add organization-level isolation WITHIN each tenant schema
-- Use Case: Gandhi Group (tenant) has multiple colleges/schools/institutes (organizations)
--           Each organization's data must be isolated from others
-- ============================================================================

-- IMPORTANT: This migration runs in EACH tenant schema, not in public schema
-- The SchemaInitializer will execute this for each tenant

-- ============================================================================
-- STEP 1: Add organization_id to existing core tables
-- ============================================================================

-- Add organization_id to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS organization_id BIGINT;

-- Add comment
COMMENT ON COLUMN users.organization_id IS 'Primary organization this user belongs to. Users can belong to multiple orgs via organization_users table.';

-- ============================================================================

-- Add organization_id to students table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'students') THEN
        ALTER TABLE students ADD COLUMN IF NOT EXISTS organization_id BIGINT NOT NULL DEFAULT 1;
        COMMENT ON COLUMN students.organization_id IS 'Organization (college/school/institute) this student belongs to';
    END IF;
END $$;

-- ============================================================================

-- Add organization_id to staff table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'staff') THEN
        ALTER TABLE staff ADD COLUMN IF NOT EXISTS organization_id BIGINT NOT NULL DEFAULT 1;
        COMMENT ON COLUMN staff.organization_id IS 'Organization this staff member belongs to';
    END IF;
END $$;

-- ============================================================================

-- Add organization_id to courses table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courses') THEN
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS organization_id BIGINT NOT NULL DEFAULT 1;
        COMMENT ON COLUMN courses.organization_id IS 'Organization offering this course';
    END IF;
END $$;

-- ============================================================================

-- Add organization_id to subjects table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'subjects') THEN
        ALTER TABLE subjects ADD COLUMN IF NOT EXISTS organization_id BIGINT NOT NULL DEFAULT 1;
        COMMENT ON COLUMN subjects.organization_id IS 'Organization offering this subject';
    END IF;
END $$;

-- ============================================================================

-- Add organization_id to branches table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'branches') THEN
        ALTER TABLE branches ADD COLUMN IF NOT EXISTS organization_id BIGINT NOT NULL DEFAULT 1;
        COMMENT ON COLUMN branches.organization_id IS 'Organization this branch belongs to';
    END IF;
END $$;

-- ============================================================================
-- STEP 2: Create organization_users junction table
-- ============================================================================

CREATE TABLE IF NOT EXISTS organization_users (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_name VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint: one user can only have one role per organization
    CONSTRAINT uk_organization_user 
        UNIQUE(organization_id, user_id)
);

-- Add comments
COMMENT ON TABLE organization_users IS 'Maps users to organizations with specific roles (within same tenant schema)';
COMMENT ON COLUMN organization_users.organization_id IS 'Reference to organisation.id in public schema';
COMMENT ON COLUMN organization_users.user_id IS 'Reference to user in this tenant schema';
COMMENT ON COLUMN organization_users.role_name IS 'Role within this organization (PRINCIPAL, TEACHER, ADMIN, etc.)';
COMMENT ON COLUMN organization_users.is_active IS 'Whether user membership is active';

-- ============================================================================
-- STEP 3: Create performance indexes
-- ============================================================================

-- Index on users.organization_id for filtering
CREATE INDEX IF NOT EXISTS idx_users_organization_id 
    ON users(organization_id) WHERE organization_id IS NOT NULL;

-- Indexes on organization_users junction table
CREATE INDEX IF NOT EXISTS idx_org_users_org_id 
    ON organization_users(organization_id);

CREATE INDEX IF NOT EXISTS idx_org_users_user_id 
    ON organization_users(user_id);

CREATE INDEX IF NOT EXISTS idx_org_users_active 
    ON organization_users(is_active) WHERE is_active = true;

-- Composite index for common query pattern
CREATE INDEX IF NOT EXISTS idx_org_users_org_user_active 
    ON organization_users(organization_id, user_id, is_active);

-- Conditional indexes on other tables (if they exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'students') THEN
        CREATE INDEX IF NOT EXISTS idx_students_organization_id ON students(organization_id);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'staff') THEN
        CREATE INDEX IF NOT EXISTS idx_staff_organization_id ON staff(organization_id);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courses') THEN
        CREATE INDEX IF NOT EXISTS idx_courses_organization_id ON courses(organization_id);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'subjects') THEN
        CREATE INDEX IF NOT EXISTS idx_subjects_organization_id ON subjects(organization_id);
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'branches') THEN
        CREATE INDEX IF NOT EXISTS idx_branches_organization_id ON branches(organization_id);
    END IF;
END $$;

-- ============================================================================
-- STEP 4: Create triggers
-- ============================================================================

-- Reusable trigger function for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to organization_users
CREATE TRIGGER update_organization_users_updated_at 
    BEFORE UPDATE ON organization_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- STEP 5: Data migration (set default organization_id = 1 for existing data)
-- ============================================================================

-- Update existing users to default organization (first organization in the tenant)
UPDATE users SET organization_id = 1 WHERE organization_id IS NULL;

-- Note: Students, staff, courses etc. already have DEFAULT 1 from ALTER TABLE

-- ============================================================================
-- Migration complete
-- ============================================================================

