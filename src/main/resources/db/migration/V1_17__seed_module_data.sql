-- ============================================================================
-- V1_17: Seed Data for Empty Modules
-- Migration: V1_17__seed_module_data.sql
--
-- Purpose: Add realistic seed data for fee, exam, enrollment,
--          notification, and activity modules to eliminate empty screens.
-- NOTE: This migration runs in TENANT schemas
-- ============================================================================

-- ============================================================================
-- GRADING SCALE & BOUNDARIES
-- ============================================================================

INSERT INTO grading_scale (id, organization_id, name, description, is_active,
    created_by, created_at, updated_by, updated_at, version, deleted)
VALUES (1, 1, 'Standard 10-Point Scale', 'Standard grading for CBSE curriculum', true,
    'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false)
ON CONFLICT DO NOTHING;

INSERT INTO grade_boundary (id, organization_id, grading_scale_id, grade_label,
    min_marks, max_marks, grade_point, description, is_active,
    created_by, created_at, updated_by, updated_at, version, deleted) VALUES
(1, 1, 1, 'A+', 90, 100, 10.0, 'Outstanding', true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(2, 1, 1, 'A',  80, 89,  9.0, 'Excellent',    true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(3, 1, 1, 'B+', 70, 79,  8.0, 'Very Good',    true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(4, 1, 1, 'B',  60, 69,  7.0, 'Good',         true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(5, 1, 1, 'C',  50, 59,  6.0, 'Average',      true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(6, 1, 1, 'D',  35, 49,  5.0, 'Below Average', true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(7, 1, 1, 'F',  0,  34,  0.0, 'Fail',         true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- EXAM TYPES
-- ============================================================================

INSERT INTO exam_type (id, organization_id, name, description, weight_percentage,
    is_active, created_by, created_at, updated_by, updated_at, version, deleted) VALUES
(1, 1, 'Mid-Term',     'Mid-semester assessment',      30, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(2, 1, 'Final',        'End-of-semester examination',   50, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(3, 1, 'Unit Test',    'Chapter-wise unit test',        10, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(4, 1, 'Practical',    'Lab/practical examination',     10, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- FEE HEADS & GROUPS
-- ============================================================================

INSERT INTO fee_head (id, organization_id, name, code, description, is_refundable,
    is_active, created_by, created_at, updated_by, updated_at, version, deleted) VALUES
(1, 1, 'Tuition Fee',       'TUITION',    'Monthly tuition charges',          false, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(2, 1, 'Admission Fee',     'ADMISSION',  'One-time admission charges',      false, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(3, 1, 'Exam Fee',          'EXAM',       'Per-exam charges',                false, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(4, 1, 'Library Fee',       'LIBRARY',    'Annual library access',           false, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(5, 1, 'Lab Fee',           'LAB',        'Science/Computer lab fee',        false, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(6, 1, 'Transport Fee',     'TRANSPORT',  'School transport charges',        true,  true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(7, 1, 'Sports Fee',        'SPORTS',     'Sports & extracurricular',        false, true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(8, 1, 'Caution Deposit',   'CAUTION',    'Refundable security deposit',     true,  true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false)
ON CONFLICT DO NOTHING;

INSERT INTO fee_group (id, organization_id, name, description,
    is_active, created_by, created_at, updated_by, updated_at, version, deleted) VALUES
(1, 1, 'Academic Fees',   'Core academic charges (tuition, exam, library)', true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(2, 1, 'Facility Fees',   'Lab, sports, and infrastructure charges',       true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(3, 1, 'Transport Fees',  'School bus and transport charges',               true, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- ACADEMIC ENROLLMENT (Link students to current academic year)
-- ============================================================================

INSERT INTO academic_enrollment (id, organization_id, enrollment_number, student_id,
    academic_year_id, class_id, section_id, roll_number, enrollment_date, status,
    created_by, created_at, updated_by, updated_at, version, deleted) VALUES
(1,  1, 'ENR-2025-001', 1,  2, 1, 1,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(2,  1, 'ENR-2025-002', 2,  2, 1, 1,  '02', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(3,  1, 'ENR-2025-003', 3,  2, 1, 2,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(4,  1, 'ENR-2025-004', 4,  2, 2, 3,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(5,  1, 'ENR-2025-005', 5,  2, 2, 3,  '02', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(6,  1, 'ENR-2025-006', 6,  2, 2, 4,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(7,  1, 'ENR-2025-007', 7,  2, 3, 5,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(8,  1, 'ENR-2025-008', 8,  2, 3, 5,  '02', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(9,  1, 'ENR-2025-009', 9,  2, 3, 6,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(10, 1, 'ENR-2025-010', 10, 2, 4, 7,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(11, 1, 'ENR-2025-011', 11, 2, 5, 8,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false),
(12, 1, 'ENR-2025-012', 12, 2, 6, 9,  '01', '2025-04-01', 'ACTIVE', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0, false)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- ACTIVITY LOG ENTRIES (Dashboard feed)
-- ============================================================================

INSERT INTO activity_log (organization_id, entity_type, entity_id, action,
    description, performed_by, performed_at, created_at, version) VALUES
(1, 'STUDENT',    1,  'Student Enrolled',     'Aarav Sharma enrolled in Class 8-A for 2025-26',        'admin',     CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP, 0),
(1, 'STUDENT',    2,  'Student Enrolled',     'Priya Patel enrolled in Class 8-A for 2025-26',         'admin',     CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP, 0),
(1, 'STUDENT',    3,  'Student Enrolled',     'Rohit Kumar enrolled in Class 8-B for 2025-26',         'admin',     CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP, 0),
(1, 'INQUIRY',    1,  'New Inquiry',          'Walk-in inquiry from Vikram Singh for Class 8',         'reception', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP, 0),
(1, 'INQUIRY',    2,  'Inquiry Contacted',    'Phone follow-up with Anjali Verma about Class 9',       'counselor', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP, 0),
(1, 'ATTENDANCE', NULL,'Attendance Marked',   'Class 8-A attendance marked: 28 present, 2 absent',     'teacher1',  CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP, 0),
(1, 'ATTENDANCE', NULL,'Attendance Marked',   'Class 9-A attendance marked: 30 present, 0 absent',     'teacher2',  CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP, 0),
(1, 'ADMISSION',  1,  'Application Received', 'New admission application from Neha Gupta for Class 8', 'reception', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP, 0),
(1, 'ADMISSION',  2,  'Application Approved', 'Admission approved for Siddharth Joshi, Class 10',      'admin',     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP, 0),
(1, 'STAFF',      1,  'Staff Updated',        'Dr. Rajesh Kumar profile updated — new phone number',   'hr',        CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP, 0),
(1, 'LEAVE',      1,  'Leave Approved',       'Sick leave approved for Priya Sharma (May 25-26)',       'principal', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP, 0),
(1, 'INQUIRY',    5,  'Inquiry Converted',    'Arjun Mehta inquiry converted to admission',            'counselor', CURRENT_TIMESTAMP - INTERVAL '1 day',  CURRENT_TIMESTAMP, 0),
(1, 'STUDENT',    5,  'Document Uploaded',    'Transfer certificate uploaded for Kavita Nair',          'admin',     CURRENT_TIMESTAMP - INTERVAL '1 day',  CURRENT_TIMESTAMP, 0),
(1, 'ATTENDANCE', NULL,'Staff Attendance',    'Staff attendance: 15 present, 1 WFH, 1 on leave',       'hr',        CURRENT_TIMESTAMP - INTERVAL '12 hours', CURRENT_TIMESTAMP, 0),
(1, 'STUDENT',    7,  'Student Created',      'New student Aditya Verma added to Class 10-A',          'admin',     CURRENT_TIMESTAMP - INTERVAL '6 hours',  CURRENT_TIMESTAMP, 0),
(1, 'PAYROLL',    NULL,'Payroll Processed',   'May 2026 payroll processed for 15 staff members',       'accountant',CURRENT_TIMESTAMP - INTERVAL '3 hours',  CURRENT_TIMESTAMP, 0),
(1, 'INQUIRY',    8,  'Follow-up Scheduled', 'WhatsApp follow-up scheduled with Ramesh Iyer',         'counselor', CURRENT_TIMESTAMP - INTERVAL '2 hours',  CURRENT_TIMESTAMP, 0),
(1, 'ADMISSION',  3,  'Documents Pending',   'Waiting for birth certificate from Sneha Reddy',        'reception', CURRENT_TIMESTAMP - INTERVAL '1 hour',   CURRENT_TIMESTAMP, 0),
(1, 'ATTENDANCE', NULL,'Attendance Marked',   'Class 10-A attendance marked for today',                'teacher3',  CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP, 0),
(1, 'STUDENT',    10, 'Fee Reminder Sent',   'Fee reminder sent to parent of Manish Tiwari',          'accountant',CURRENT_TIMESTAMP - INTERVAL '15 minutes', CURRENT_TIMESTAMP, 0);
