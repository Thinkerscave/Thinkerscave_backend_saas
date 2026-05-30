-- ============================================================================
-- V1_15: Add Audit Columns to ClassEntity and Section
-- Migration: V1_15__add_audit_columns.sql
--
-- Purpose: ClassEntity and Section lack audit columns (created_by, created_date,
--          etc.). This adds them for governance and traceability.
-- NOTE: This migration runs in TENANT schemas
-- ============================================================================

-- ============================================================================
-- STEP 1: Add audit columns to class table
-- ============================================================================

DO $$
BEGIN
    -- created_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'class' AND column_name = 'created_by'
    ) THEN
        ALTER TABLE class ADD COLUMN created_by VARCHAR(100);
        RAISE NOTICE 'Added created_by to class';
    END IF;

    -- created_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'class' AND column_name = 'created_date'
    ) THEN
        ALTER TABLE class ADD COLUMN created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added created_date to class';
    END IF;

    -- last_modified_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'class' AND column_name = 'last_modified_by'
    ) THEN
        ALTER TABLE class ADD COLUMN last_modified_by VARCHAR(100);
        RAISE NOTICE 'Added last_modified_by to class';
    END IF;

    -- last_modified_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'class' AND column_name = 'last_modified_date'
    ) THEN
        ALTER TABLE class ADD COLUMN last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added last_modified_date to class';
    END IF;

    -- is_active
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'class' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE class ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
        RAISE NOTICE 'Added is_active to class';
    END IF;
END $$;

-- ============================================================================
-- STEP 2: Add audit columns to section table
-- ============================================================================

DO $$
BEGIN
    -- created_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'section' AND column_name = 'created_by'
    ) THEN
        ALTER TABLE section ADD COLUMN created_by VARCHAR(100);
        RAISE NOTICE 'Added created_by to section';
    END IF;

    -- created_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'section' AND column_name = 'created_date'
    ) THEN
        ALTER TABLE section ADD COLUMN created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added created_date to section';
    END IF;

    -- last_modified_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'section' AND column_name = 'last_modified_by'
    ) THEN
        ALTER TABLE section ADD COLUMN last_modified_by VARCHAR(100);
        RAISE NOTICE 'Added last_modified_by to section';
    END IF;

    -- last_modified_date
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'section' AND column_name = 'last_modified_date'
    ) THEN
        ALTER TABLE section ADD COLUMN last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added last_modified_date to section';
    END IF;

    -- is_active
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'section' AND column_name = 'is_active'
    ) THEN
        ALTER TABLE section ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
        RAISE NOTICE 'Added is_active to section';
    END IF;
END $$;

-- ============================================================================
-- STEP 3: Set defaults for existing records
-- ============================================================================

UPDATE class SET created_by = 'system', created_date = CURRENT_TIMESTAMP
    WHERE created_by IS NULL;

UPDATE section SET created_by = 'system', created_date = CURRENT_TIMESTAMP
    WHERE created_by IS NULL;
