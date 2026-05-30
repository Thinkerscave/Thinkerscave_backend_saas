-- ============================================================================
-- V1_13: Add Missing Indexes for Performance
-- Migration: V1_13__add_missing_indexes.sql
--
-- Purpose: Add indexes on frequently queried columns across all tables.
--          Focuses on organization_id, FK columns, and date columns.
-- NOTE: This migration runs in TENANT schemas
-- ============================================================================

-- ============================================================================
-- ATTENDANCE INDEXES (Critical — high-volume table)
-- ============================================================================

-- Composite index for daily attendance lookup
CREATE INDEX IF NOT EXISTS idx_attendance_org_date
    ON attendance(organization_id, attendance_date);

-- Index for student/staff attendance lookup by reference
CREATE INDEX IF NOT EXISTS idx_attendance_ref_type
    ON attendance(reference_id, attendance_type);

-- Index for class-level attendance reports
CREATE INDEX IF NOT EXISTS idx_attendance_class_date
    ON attendance(class_id, attendance_date);

-- Index for attendance type filtering
CREATE INDEX IF NOT EXISTS idx_attendance_type
    ON attendance(attendance_type);

-- Index for status filtering
CREATE INDEX IF NOT EXISTS idx_attendance_status
    ON attendance(status);

-- ============================================================================
-- STUDENT INDEXES
-- ============================================================================

-- Organization scoping
CREATE INDEX IF NOT EXISTS idx_student_org
    ON student(organization_id);

-- Email lookup
CREATE INDEX IF NOT EXISTS idx_student_email
    ON student(email);

-- Active status filtering
CREATE INDEX IF NOT EXISTS idx_student_active
    ON student(is_active);

-- ============================================================================
-- STAFF INDEXES
-- ============================================================================

-- Organization scoping
CREATE INDEX IF NOT EXISTS idx_staff_org
    ON staff(organization_id);

-- Staff code lookup
CREATE INDEX IF NOT EXISTS idx_staff_code
    ON staff(staff_code);

-- Email lookup
CREATE INDEX IF NOT EXISTS idx_staff_email
    ON staff(email);

-- Department filtering
CREATE INDEX IF NOT EXISTS idx_staff_department
    ON staff(department_id);

-- Branch filtering
CREATE INDEX IF NOT EXISTS idx_staff_branch
    ON staff(branch_id);

-- Active status filtering
CREATE INDEX IF NOT EXISTS idx_staff_active
    ON staff(is_active);

-- ============================================================================
-- USER INDEXES
-- ============================================================================

-- Username lookup (login)
CREATE INDEX IF NOT EXISTS idx_user_username
    ON users(user_name);

-- Email lookup
CREATE INDEX IF NOT EXISTS idx_user_email
    ON users(email);

-- User code lookup
CREATE INDEX IF NOT EXISTS idx_user_code
    ON users(user_code);

-- ============================================================================
-- CLASS ENTITY INDEXES
-- ============================================================================

-- Organization scoping
CREATE INDEX IF NOT EXISTS idx_class_org
    ON class(organization_id);

-- ============================================================================
-- SECTION INDEXES
-- ============================================================================

-- FK to class
CREATE INDEX IF NOT EXISTS idx_section_class
    ON section(class_entity_class_id);

-- ============================================================================
-- ROLE INDEXES
-- ============================================================================

-- Role code lookup
CREATE INDEX IF NOT EXISTS idx_role_code
    ON role_master(role_code);

-- Organization scoping
CREATE INDEX IF NOT EXISTS idx_role_org
    ON role_master(organization_id);

-- Active status
CREATE INDEX IF NOT EXISTS idx_role_active
    ON role_master(is_active);

-- ============================================================================
-- ACADEMIC YEAR INDEXES
-- ============================================================================

-- Year code lookup
CREATE INDEX IF NOT EXISTS idx_academic_year_code
    ON academic_years(year_code);

-- Current year flag
CREATE INDEX IF NOT EXISTS idx_academic_year_current
    ON academic_years(is_current);

-- ============================================================================
-- COURSE INDEXES
-- ============================================================================

-- Course code lookup
CREATE INDEX IF NOT EXISTS idx_course_code
    ON courses(course_code);

-- Active status
CREATE INDEX IF NOT EXISTS idx_course_active
    ON courses(is_active);

-- ============================================================================
-- SUBJECT INDEXES
-- ============================================================================

-- Subject code lookup
CREATE INDEX IF NOT EXISTS idx_subject_code
    ON subjects(subject_code);

-- Active status
CREATE INDEX IF NOT EXISTS idx_subject_active
    ON subjects(is_active);

-- ============================================================================
-- GUARDIAN INDEXES
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'guardian') THEN
        CREATE INDEX IF NOT EXISTS idx_guardian_org
            ON guardian(organization_id);
    END IF;
END $$;

-- ============================================================================
-- BRANCH INDEXES
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'branch') THEN
        CREATE INDEX IF NOT EXISTS idx_branch_org
            ON branch(organization_id);
        CREATE INDEX IF NOT EXISTS idx_branch_active
            ON branch(is_active);
    END IF;
END $$;

-- ============================================================================
-- DEPARTMENT INDEXES
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'department') THEN
        CREATE INDEX IF NOT EXISTS idx_department_org
            ON department(organization_id);
        CREATE INDEX IF NOT EXISTS idx_department_active
            ON department(is_active);
    END IF;
END $$;

-- ============================================================================
-- LEAVE REQUEST INDEXES
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'leave_request') THEN
        CREATE INDEX IF NOT EXISTS idx_leave_org
            ON leave_request(organization_id);
        CREATE INDEX IF NOT EXISTS idx_leave_status
            ON leave_request(status);
        CREATE INDEX IF NOT EXISTS idx_leave_user
            ON leave_request(user_id);
    END IF;
END $$;

-- ============================================================================
-- FOLLOWUP INDEXES
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'followup') THEN
        CREATE INDEX IF NOT EXISTS idx_followup_inquiry
            ON followup(inquiry_id);
        CREATE INDEX IF NOT EXISTS idx_followup_date
            ON followup(followup_date);
    END IF;
END $$;

-- ============================================================================
-- FEE INDEXES
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'fee_invoice') THEN
        CREATE INDEX IF NOT EXISTS idx_fee_invoice_student
            ON fee_invoice(student_id);
        CREATE INDEX IF NOT EXISTS idx_fee_invoice_status
            ON fee_invoice(status);
        CREATE INDEX IF NOT EXISTS idx_fee_invoice_due_date
            ON fee_invoice(due_date);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'fee_payment') THEN
        CREATE INDEX IF NOT EXISTS idx_fee_payment_invoice
            ON fee_payment(invoice_id);
        CREATE INDEX IF NOT EXISTS idx_fee_payment_date
            ON fee_payment(payment_date);
    END IF;
END $$;
