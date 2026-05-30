-- ============================================================================
-- V1_14: Add Missing Foreign Key Constraints
-- Migration: V1_14__add_foreign_key_constraints.sql
--
-- Purpose: Add organizationId FK constraints and other missing FKs.
--          Enforces referential integrity at database level.
-- NOTE: This migration runs in TENANT schemas
-- ============================================================================

-- ============================================================================
-- STEP 1: Add organization_id FK to Student
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_student_organization'
        AND table_name = 'student'
    ) THEN
        -- Ensure no orphan records before adding constraint
        UPDATE student SET organization_id = 1
            WHERE organization_id IS NULL;

        ALTER TABLE student
            ADD CONSTRAINT fk_student_organization
            FOREIGN KEY (organization_id)
            REFERENCES organisation(org_id)
            ON DELETE RESTRICT;

        RAISE NOTICE 'Added fk_student_organization constraint';
    END IF;
END $$;

-- ============================================================================
-- STEP 2: Add organization_id FK to Staff
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_staff_organization'
        AND table_name = 'staff'
    ) THEN
        UPDATE staff SET organization_id = 1
            WHERE organization_id IS NULL;

        ALTER TABLE staff
            ADD CONSTRAINT fk_staff_organization
            FOREIGN KEY (organization_id)
            REFERENCES organisation(org_id)
            ON DELETE RESTRICT;

        RAISE NOTICE 'Added fk_staff_organization constraint';
    END IF;
END $$;

-- ============================================================================
-- STEP 3: Add organization_id FK to Attendance
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_attendance_organization'
        AND table_name = 'attendance'
    ) THEN
        UPDATE attendance SET organization_id = 1
            WHERE organization_id IS NULL;

        ALTER TABLE attendance
            ADD CONSTRAINT fk_attendance_organization
            FOREIGN KEY (organization_id)
            REFERENCES organisation(org_id)
            ON DELETE RESTRICT;

        RAISE NOTICE 'Added fk_attendance_organization constraint';
    END IF;
END $$;

-- ============================================================================
-- STEP 4: Add organization_id FK to ClassEntity
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_class_organization'
        AND table_name = 'class'
    ) THEN
        UPDATE class SET organization_id = 1
            WHERE organization_id IS NULL;

        ALTER TABLE class
            ADD CONSTRAINT fk_class_organization
            FOREIGN KEY (organization_id)
            REFERENCES organisation(org_id)
            ON DELETE RESTRICT;

        RAISE NOTICE 'Added fk_class_organization constraint';
    END IF;
END $$;

-- ============================================================================
-- STEP 5: Add organization_id FK to Inquiry
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_inquiry_organization'
        AND table_name = 'inquiry'
    ) THEN
        UPDATE inquiry SET organization_id = 1
            WHERE organization_id IS NULL;

        ALTER TABLE inquiry
            ADD CONSTRAINT fk_inquiry_organization
            FOREIGN KEY (organization_id)
            REFERENCES organisation(org_id)
            ON DELETE RESTRICT;

        RAISE NOTICE 'Added fk_inquiry_organization constraint';
    END IF;
END $$;

-- ============================================================================
-- STEP 6: Add organization_id to Section (column + FK)
-- ============================================================================

DO $$
BEGIN
    -- Add column if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'section'
        AND column_name = 'organization_id'
    ) THEN
        ALTER TABLE section ADD COLUMN organization_id BIGINT;
        RAISE NOTICE 'Added organization_id column to section';
    END IF;

    -- Set default for existing records
    UPDATE section SET organization_id = 1
        WHERE organization_id IS NULL;

    -- Add FK constraint
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_section_organization'
        AND table_name = 'section'
    ) THEN
        ALTER TABLE section
            ADD CONSTRAINT fk_section_organization
            FOREIGN KEY (organization_id)
            REFERENCES organisation(org_id)
            ON DELETE RESTRICT;

        RAISE NOTICE 'Added fk_section_organization constraint';
    END IF;
END $$;

-- ============================================================================
-- STEP 7: Add organization_id FK to Role
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_role_organization'
        AND table_name = 'role_master'
    ) THEN
        -- Role may have NULL organization_id (system roles)
        -- Only add FK where organization_id IS NOT NULL
        -- Use a partial constraint or handle NULLs
        ALTER TABLE role_master
            ADD CONSTRAINT fk_role_organization
            FOREIGN KEY (organization_id)
            REFERENCES organisation(org_id)
            ON DELETE RESTRICT;

        RAISE NOTICE 'Added fk_role_organization constraint';
    END IF;
END $$;

-- ============================================================================
-- STEP 8: Add class_id FK to Attendance
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'class') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_attendance_class'
            AND table_name = 'attendance'
        ) THEN
            ALTER TABLE attendance
                ADD CONSTRAINT fk_attendance_class
                FOREIGN KEY (class_id)
                REFERENCES class(class_id)
                ON DELETE SET NULL;

            RAISE NOTICE 'Added fk_attendance_class constraint';
        END IF;
    END IF;
END $$;

-- ============================================================================
-- STEP 9: Add section_id to Attendance (for future normalization)
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'attendance'
        AND column_name = 'section_id'
    ) THEN
        ALTER TABLE attendance ADD COLUMN section_id BIGINT;

        RAISE NOTICE 'Added section_id column to attendance';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'section') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_attendance_section'
            AND table_name = 'attendance'
        ) THEN
            ALTER TABLE attendance
                ADD CONSTRAINT fk_attendance_section
                FOREIGN KEY (section_id)
                REFERENCES section(section_id)
                ON DELETE SET NULL;

            RAISE NOTICE 'Added fk_attendance_section constraint';
        END IF;
    END IF;
END $$;

-- ============================================================================
-- STEP 10: Add unique constraints
-- ============================================================================

-- Unique class name per organization
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'uk_class_name_org'
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_class_name_org
            ON class(class_name, organization_id)
            WHERE organization_id IS NOT NULL;
        RAISE NOTICE 'Added unique index on class(class_name, organization_id)';
    END IF;
END $$;

-- Unique section name per class
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'uk_section_class'
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_section_class
            ON section(section_name, class_entity_class_id)
            WHERE class_entity_class_id IS NOT NULL;
        RAISE NOTICE 'Added unique index on section(section_name, class_entity_class_id)';
    END IF;
END $$;

-- Unique staff code per organization
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'uk_staff_code_org'
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_staff_code_org
            ON staff(staff_code, organization_id)
            WHERE staff_code IS NOT NULL AND organization_id IS NOT NULL;
        RAISE NOTICE 'Added unique index on staff(staff_code, organization_id)';
    END IF;
END $$;

-- Prevent duplicate attendance per student per day
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'uk_attendance_ref_date'
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_ref_date
            ON attendance(reference_id, attendance_type, attendance_date, organization_id)
            WHERE reference_id IS NOT NULL;
        RAISE NOTICE 'Added unique index on attendance(reference_id, type, date, org)';
    END IF;
END $$;
