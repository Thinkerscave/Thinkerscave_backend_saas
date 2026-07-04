-- =============================================================================
-- PHASE 3: Production-like multi-tenant demo (menus, RBAC, 10 JSB teachers)
-- Idempotent — safe to re-run via DevDataInitializer
-- =============================================================================

-- Align menu routes with Angular /app/* paths
UPDATE menus SET route = '/app' WHERE menu_code = 'DASHBOARD';
UPDATE menus SET route = '/app/students' WHERE menu_code = 'STUDENTS';
UPDATE menus SET route = '/app/staff' WHERE menu_code = 'STAFF';
UPDATE menus SET route = '/app/attendance' WHERE menu_code = 'ATTENDANCE';
UPDATE menus SET route = '/app/academics' WHERE menu_code = 'ACADEMICS';
UPDATE menus SET route = '/app/admissions' WHERE menu_code = 'ADMISSION';
UPDATE menus SET route = '/app/access-management' WHERE menu_code = 'ACCESS';

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(8,  'PLATFORM',      'Tenant Management', 'Platform tenant provisioning', '/app/tenant-management', 'business',      'MODULE', NULL, 8,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(9,  'COMMUNICATION', 'Communication',     'Notices and messaging',        '/app/communication',     'campaign',      'MODULE', NULL, 9,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(10, 'FEES',          'Fees & Billing',    'Fee collection and ledger',    '/app/fees',              'payments',      'MODULE', NULL, 10, TRUE, TRUE, FALSE, 'system', 'system', 0),
(11, 'ORGANIZATION',  'Organization',      'Org profile and branches',     '/app/organization',      'apartment',     'MODULE', NULL, 11, TRUE, TRUE, FALSE, 'system', 'system', 0),
(12, 'EXAMS',         'Examinations',      'Exam schedules and results',   '/app/exams',             'assignment',    'MODULE', NULL, 12, TRUE, TRUE, FALSE, 'system', 'system', 0),
(13, 'AUDIT',         'Audit Logs',        'Platform activity logs',       '/app/organization/activity-logs', 'history', 'MODULE', NULL, 13, TRUE, TRUE, FALSE, 'system', 'system', 0);

-- SUPER_ADMIN: platform only (no student/staff/academic campus modules)
DELETE FROM role_permissions WHERE role_id = 6;

INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(100, 1, 6, 1,  TRUE, FALSE, FALSE, 'system', 'system', 0),
(101, 1, 6, 8,  TRUE, TRUE,  TRUE,  'system', 'system', 0),
(102, 1, 6, 13, TRUE, TRUE,  FALSE, 'system', 'system', 0);

-- Org 1 (JSB) — complete campus RBAC
INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
-- Owner: full campus access (not platform)
(110, 1, 1, 2,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(111, 1, 1, 6,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(112, 1, 1, 7,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(113, 1, 1, 9,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(114, 1, 1, 10, TRUE, TRUE, TRUE, 'system', 'system', 0),
(115, 1, 1, 11, TRUE, TRUE, TRUE, 'system', 'system', 0),
(116, 1, 1, 12, TRUE, TRUE, TRUE, 'system', 'system', 0),
-- Admin: manage campus modules
(120, 1, 2, 1,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(121, 1, 2, 2,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(122, 1, 2, 3,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(123, 1, 2, 4,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(124, 1, 2, 5,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(125, 1, 2, 6,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(126, 1, 2, 7,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(127, 1, 2, 9,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(128, 1, 2, 10, TRUE, TRUE, FALSE, 'system', 'system', 0),
(129, 1, 2, 11, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Staff (teachers): teaching workspace
(130, 1, 3, 1,  TRUE, FALSE, FALSE, 'system', 'system', 0),
(131, 1, 3, 3,  TRUE, FALSE, FALSE, 'system', 'system', 0),
(132, 1, 3, 5,  TRUE, TRUE,  FALSE, 'system', 'system', 0),
(133, 1, 3, 6,  TRUE, FALSE, FALSE, 'system', 'system', 0),
(134, 1, 3, 9,  TRUE, FALSE, FALSE, 'system', 'system', 0),
-- Student portal
(135, 1, 4, 1,  TRUE, FALSE, FALSE, 'system', 'system', 0),
(136, 1, 4, 5,  TRUE, FALSE, FALSE, 'system', 'system', 0),
-- Parent portal
(137, 1, 5, 1,  TRUE, FALSE, FALSE, 'system', 'system', 0),
(138, 1, 5, 9,  TRUE, FALSE, FALSE, 'system', 'system', 0);

-- Org 2 (JSC) staff/student/parent
INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(140, 2, 3, 1, TRUE, FALSE, FALSE, 'system', 'system', 0),
(141, 2, 3, 3, TRUE, FALSE, FALSE, 'system', 'system', 0),
(142, 2, 3, 5, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(143, 2, 3, 6, TRUE, FALSE, FALSE, 'system', 'system', 0),
(144, 2, 4, 1, TRUE, FALSE, FALSE, 'system', 'system', 0),
(145, 2, 5, 1, TRUE, FALSE, FALSE, 'system', 'system', 0),
(146, 2, 2, 7, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(147, 2, 2, 9, TRUE, TRUE,  FALSE, 'system', 'system', 0);

-- Org 3 (ABC) staff/student
INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(150, 3, 3, 1, TRUE, FALSE, FALSE, 'system', 'system', 0),
(151, 3, 3, 3, TRUE, FALSE, FALSE, 'system', 'system', 0),
(152, 3, 3, 5, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(153, 3, 4, 1, TRUE, FALSE, FALSE, 'system', 'system', 0),
(154, 3, 2, 4, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(155, 3, 2, 7, TRUE, TRUE,  FALSE, 'system', 'system', 0);

-- Org 4 (KCC) staff/student
INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(160, 4, 3, 1, TRUE, FALSE, FALSE, 'system', 'system', 0),
(161, 4, 3, 3, TRUE, FALSE, FALSE, 'system', 'system', 0),
(162, 4, 3, 5, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(163, 4, 4, 1, TRUE, FALSE, FALSE, 'system', 'system', 0),
(164, 4, 2, 4, TRUE, TRUE,  FALSE, 'system', 'system', 0);

-- =========================================================
-- JSB: 8 additional teachers (javier.teacher3 .. teacher10)
-- =========================================================
INSERT IGNORE INTO users (
    id, organization_id, user_code, username, email, mobile_number, password,
    first_name, last_name, display_name, profile_image_url, status,
    email_verified, mobile_verified, first_time_login, failed_login_attempts, account_locked,
    created_by, updated_by, version)
VALUES
(22, 1, 'USR000022', 'javier.teacher3',  'teacher3@jsb.edu.in',  '9777111203', 'PLACEHOLDER', 'Minati',     'Dash',    'Minati Dash',       NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(23, 1, 'USR000023', 'javier.teacher4',  'teacher4@jsb.edu.in',  '9777111204', 'PLACEHOLDER', 'Pradeep',    'Mohanty', 'Pradeep Mohanty',   NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(24, 1, 'USR000024', 'javier.teacher5',  'teacher5@jsb.edu.in',  '9777111205', 'PLACEHOLDER', 'Smita',      'Mishra',  'Smita Mishra',      NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(25, 1, 'USR000025', 'javier.teacher6',  'teacher6@jsb.edu.in',  '9777111206', 'PLACEHOLDER', 'Debasis',    'Rout',    'Debasis Rout',      NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(26, 1, 'USR000026', 'javier.teacher7',  'teacher7@jsb.edu.in',  '9777111207', 'PLACEHOLDER', 'Tapaswini',  'Panda',   'Tapaswini Panda',   NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(27, 1, 'USR000027', 'javier.teacher8',  'teacher8@jsb.edu.in',  '9777111208', 'PLACEHOLDER', 'Manas',      'Kumar',   'Manas Kumar',       NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(28, 1, 'USR000028', 'javier.teacher9',  'teacher9@jsb.edu.in',  '9777111209', 'PLACEHOLDER', 'Supriya',    'Das',     'Supriya Das',       NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(29, 1, 'USR000029', 'javier.teacher10', 'teacher10@jsb.edu.in', '9777111210', 'PLACEHOLDER', 'Chinmay',    'Sahu',    'Chinmay Sahu',      NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0);

INSERT IGNORE INTO user_roles (id, user_id, role_id, primary_role, active, created_by, updated_by, version)
VALUES
(22, 22, 3, TRUE, TRUE, 'system', 'system', 0),
(23, 23, 3, TRUE, TRUE, 'system', 'system', 0),
(24, 24, 3, TRUE, TRUE, 'system', 'system', 0),
(25, 25, 3, TRUE, TRUE, 'system', 'system', 0),
(26, 26, 3, TRUE, TRUE, 'system', 'system', 0),
(27, 27, 3, TRUE, TRUE, 'system', 'system', 0),
(28, 28, 3, TRUE, TRUE, 'system', 'system', 0),
(29, 29, 3, TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO staff (
    staff_id, user_id, staff_code, first_name, last_name, gender, date_of_birth,
    mobile_number, email, staff_type, designation, employment_category, employment_status,
    joining_date, highest_qualification, experience_years, active, created_by, updated_by, version)
VALUES
(20, 22, 'JSB-TCHR-003', 'Minati',    'Dash',    'Female', '1991-02-18', '9777111203', 'teacher3@jsb.edu.in',  'TEACHING', 'Odia Teacher',           'PERMANENT', 'ACTIVE', '2021-06-01', 'M.A. Odia, B.Ed.',        7, TRUE, 'system', 'system', 0),
(21, 23, 'JSB-TCHR-004', 'Pradeep',   'Mohanty', 'Male',   '1988-09-25', '9777111204', 'teacher4@jsb.edu.in',  'TEACHING', 'Science Teacher',        'PERMANENT', 'ACTIVE', '2020-07-15', 'M.Sc. Chemistry, B.Ed.',  9, TRUE, 'system', 'system', 0),
(22, 24, 'JSB-TCHR-005', 'Smita',     'Mishra',  'Female', '1990-12-03', '9777111205', 'teacher5@jsb.edu.in',  'TEACHING', 'Social Science Teacher', 'PERMANENT', 'ACTIVE', '2022-04-01', 'M.A. History, B.Ed.',     6, TRUE, 'system', 'system', 0),
(23, 25, 'JSB-TCHR-006', 'Debasis',   'Rout',    'Male',   '1993-05-11', '9777111206', 'teacher6@jsb.edu.in',  'TEACHING', 'Computer Teacher',       'PERMANENT', 'ACTIVE', '2023-07-01', 'B.Tech CSE, B.Ed.',       4, TRUE, 'system', 'system', 0),
(24, 26, 'JSB-TCHR-007', 'Tapaswini', 'Panda',   'Female', '1992-08-30', '9777111207', 'teacher7@jsb.edu.in',  'TEACHING', 'Hindi Teacher',          'PERMANENT', 'ACTIVE', '2021-11-01', 'M.A. Hindi, B.Ed.',       6, TRUE, 'system', 'system', 0),
(25, 27, 'JSB-TCHR-008', 'Manas',     'Kumar',   'Male',   '1989-04-07', '9777111208', 'teacher8@jsb.edu.in',  'TEACHING', 'Physical Education',     'PERMANENT', 'ACTIVE', '2019-06-01', 'B.P.Ed.',                 8, TRUE, 'system', 'system', 0),
(26, 28, 'JSB-TCHR-009', 'Supriya',   'Das',     'Female', '1994-01-22', '9777111209', 'teacher9@jsb.edu.in',  'TEACHING', 'Art Teacher',            'PERMANENT', 'ACTIVE', '2024-01-10', 'BFA, B.Ed.',              3, TRUE, 'system', 'system', 0),
(27, 29, 'JSB-TCHR-010', 'Chinmay',   'Sahu',    'Male',   '1995-06-14', '9777111210', 'teacher10@jsb.edu.in', 'TEACHING', 'Music Teacher',          'PERMANENT', 'ACTIVE', '2024-06-01', 'B.A. Music, Diploma',     2, TRUE, 'system', 'system', 0);

-- Subject assignments for new teachers (classes IV–X)
INSERT IGNORE INTO subject_assignment (
    subject_assignment_id, academic_year_id, class_id, section_id, subject_id,
    teacher_id, periods_per_week, active, remarks, created_by, updated_by, version)
VALUES
(20, 1, 8,  7,  1,  22, 5, TRUE, 'Odia Class IV',        'system', 'system', 0),
(21, 1, 9,  8,  3,  23, 4, TRUE, 'Science Class V',      'system', 'system', 0),
(22, 1, 10, 9,  4,  24, 4, TRUE, 'Social Sci Class VI',  'system', 'system', 0),
(23, 1, 11, 10, 10, 25, 3, TRUE, 'Computer Class VII',   'system', 'system', 0),
(24, 1, 12, 11, 6,  26, 4, TRUE, 'Hindi Class VIII',     'system', 'system', 0),
(25, 1, 13, 12, 17, 27, 4, TRUE, 'PE Class IX',          'system', 'system', 0),
(26, 1, 14, 13, 16, 28, 2, TRUE, 'Art Class X',          'system', 'system', 0),
(27, 1, 6,  4,  8,  29, 2, TRUE, 'Music Class III',      'system', 'system', 0);

-- Staff attendance for June 30 (new teachers)
INSERT IGNORE INTO staff_attendance (
    attendance_id, organization_id, staff_id, staff_name, staff_code, department, designation,
    attendance_date, sign_in_time, sign_out_time, working_minutes, shift, status, remarks,
    marked_by, created_by, updated_by, version)
VALUES
(30, 1, 20, 'Minati Dash',     'JSB-TCHR-003', 'Languages', 'Odia Teacher',           '2026-06-30', '2026-06-30 08:05:00', '2026-06-30 15:30:00', 445, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0),
(31, 1, 21, 'Pradeep Mohanty', 'JSB-TCHR-004', 'Science',   'Science Teacher',        '2026-06-30', '2026-06-30 08:10:00', '2026-06-30 15:35:00', 445, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0),
(32, 1, 22, 'Smita Mishra',    'JSB-TCHR-005', 'Social',    'Social Science Teacher', '2026-06-30', '2026-06-30 08:08:00', '2026-06-30 15:32:00', 444, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0),
(33, 1, 23, 'Debasis Rout',    'JSB-TCHR-006', 'Computer',  'Computer Teacher',       '2026-06-30', '2026-06-30 08:12:00', '2026-06-30 15:40:00', 448, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0),
(34, 1, 24, 'Tapaswini Panda', 'JSB-TCHR-007', 'Languages', 'Hindi Teacher',          '2026-06-30', '2026-06-30 08:06:00', '2026-06-30 15:28:00', 442, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0),
(35, 1, 25, 'Manas Kumar',     'JSB-TCHR-008', 'Sports',    'Physical Education',     '2026-06-30', '2026-06-30 07:55:00', '2026-06-30 15:20:00', 445, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0),
(36, 1, 26, 'Supriya Das',     'JSB-TCHR-009', 'Arts',      'Art Teacher',            '2026-06-30', '2026-06-30 08:15:00', '2026-06-30 15:45:00', 450, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0),
(37, 1, 27, 'Chinmay Sahu',    'JSB-TCHR-010', 'Arts',      'Music Teacher',          '2026-06-30', '2026-06-30 08:20:00', '2026-06-30 15:50:00', 450, 'Morning', 'PRESENT', 'On time', 'javier.admin', 'system', 'system', 0);
