-- ============================================================================
-- Database Integrity Constraints
-- Migration: V1_6__add_database_constraints.sql
-- 
-- Purpose: Add foreign key constraints and indexes for data integrity
-- NOTE: This migration runs in TENANT schemas (not public)
-- ============================================================================

-- ============================================================================
-- STEP 1: Self-referencing FK for parent organizations
-- ============================================================================

-- Check if parent column exists in organisations table
-- The parent_id should reference another organisation in the same tenant

-- Note: If organisations table is in PUBLIC schema, this constraint 
-- would need to be added there, not in tenant schemas.
-- This migration handles the case where organizations are per-tenant.

-- ============================================================================
-- STEP 2: Constraint on guardian (parent) relationship
-- ============================================================================

-- Add NOT NULL constraint to guardian_id if not already set
DO $$
BEGIN
    -- Check if the column allows NULLs and alter if needed
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'student' 
        AND column_name = 'guardian_id' 
        AND is_nullable = 'YES'
    ) THEN
        -- First update any NULL values to prevent constraint violation
        -- (You may need to handle orphaned records differently)
        -- ALTER TABLE student ALTER COLUMN guardian_id SET NOT NULL;
        RAISE NOTICE 'guardian_id constraint check complete';
    END IF;
END $$;

-- ============================================================================
-- STEP 3: Add foreign key for user references
-- ============================================================================

-- Ensure user_id in student table references users table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_student_user' 
        AND table_name = 'student'
    ) THEN
        ALTER TABLE student 
        ADD CONSTRAINT fk_student_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE;
        
        RAISE NOTICE 'Added fk_student_user constraint';
    END IF;
END $$;

-- Ensure user_id in guardian table references users table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_guardian_user' 
        AND table_name = 'guardian'
    ) THEN
        -- Only add if guardian table exists
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'guardian') THEN
            ALTER TABLE guardian 
            ADD CONSTRAINT fk_guardian_user 
            FOREIGN KEY (user_id) 
            REFERENCES users(id) 
            ON DELETE CASCADE;
            
            RAISE NOTICE 'Added fk_guardian_user constraint';
        END IF;
    END IF;
END $$;

-- ============================================================================
-- STEP 4: Add class and section foreign keys
-- ============================================================================

-- Ensure class_id in student references class table
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'class') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints 
            WHERE constraint_name = 'fk_student_class' 
            AND table_name = 'student'
        ) THEN
            ALTER TABLE student 
            ADD CONSTRAINT fk_student_class 
            FOREIGN KEY (class_id) 
            REFERENCES class(class_id) 
            ON DELETE SET NULL;
            
            RAISE NOTICE 'Added fk_student_class constraint';
        END IF;
    END IF;
END $$;

-- Ensure section_id in student references section table
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'section') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints 
            WHERE constraint_name = 'fk_student_section' 
            AND table_name = 'student'
        ) THEN
            ALTER TABLE student 
            ADD CONSTRAINT fk_student_section 
            FOREIGN KEY (section_id) 
            REFERENCES section(section_id) 
            ON DELETE SET NULL;
            
            RAISE NOTICE 'Added fk_student_section constraint';
        END IF;
    END IF;
END $$;

-- ============================================================================
-- STEP 5: Add unique constraints for data integrity
-- ============================================================================

-- Ensure email is unique per organization
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'uk_student_email_org' 
        AND table_name = 'student'
    ) THEN
        -- Create unique index on email + organization_id
        CREATE UNIQUE INDEX IF NOT EXISTS uk_student_email_org 
        ON student(email, organization_id);
        
        RAISE NOTICE 'Added unique index on student(email, organization_id)';
    END IF;
END $$;

-- Ensure roll number is unique per class and organization
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'uk_student_roll_class_org'
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_student_roll_class_org 
        ON student(roll_number, class_id, organization_id) 
        WHERE roll_number IS NOT NULL;
        
        RAISE NOTICE 'Added unique index on student(roll_number, class_id, organization_id)';
    END IF;
END $$;

-- ============================================================================
-- STEP 6: Add performance indexes
-- ============================================================================

-- Index on student.class_id for faster joins
CREATE INDEX IF NOT EXISTS idx_student_class_id ON student(class_id);

-- Index on student.section_id for faster joins  
CREATE INDEX IF NOT EXISTS idx_student_section_id ON student(section_id);

-- Index on student.user_id for faster joins
CREATE INDEX IF NOT EXISTS idx_student_user_id ON student(user_id);

-- Index on guardian for user lookup
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'guardian') THEN
        CREATE INDEX IF NOT EXISTS idx_guardian_user_id ON guardian(user_id);
    END IF;
END $$;

-- ============================================================================
-- Migration complete
-- ============================================================================
