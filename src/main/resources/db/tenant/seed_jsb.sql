-- ============================================================
-- TENANT: Javier School Bhubaneswar (jsb-bhubaneswar)
-- Schema: tenant_jsb_bhubaneswar   Organization ID: 1
-- Loaded by DevDataInitializer into the tenant_jsb_bhubaneswar MySQL database.
-- INSERT IGNORE keeps the script idempotent.
-- ============================================================

-- ============================================================
-- 1. REFERENCE DATA — Roles & Menus (replicated from platform)
-- ============================================================

INSERT IGNORE INTO roles (id, role_code, role_name, description, role_type, dashboard_code, system_role, active, display_order, created_by, updated_by, version)
VALUES
(1, 'ROLE_OWNER',       'Organization Owner',      'Campus owner with full access',    'ORGANIZATION_OWNER', 'ADMIN',  TRUE, TRUE, 1, 'system', 'system', 0),
(2, 'ROLE_ADMIN',       'Organization Admin',       'Campus administrator',              'ORGANIZATION_ADMIN', 'ADMIN',  TRUE, TRUE, 2, 'system', 'system', 0),
(3, 'ROLE_STAFF',       'Staff',                    'Teaching / non-teaching staff',     'STAFF',              'STAFF',  TRUE, TRUE, 3, 'system', 'system', 0),
(4, 'ROLE_STUDENT',     'Student',                  'Student portal access',             'STUDENT',            'STUDENT',TRUE, TRUE, 4, 'system', 'system', 0),
(5, 'ROLE_PARENT',      'Parent',                   'Parent portal access',              'PARENT',             'PARENT', TRUE, TRUE, 5, 'system', 'system', 0),
(7, 'ROLE_TEACHER',     'Teacher',                  'Classroom teacher',                 'STAFF',              'STAFF',  TRUE, TRUE, 6, 'system', 'system', 0),
(8, 'ROLE_PRINCIPAL',   'Principal',                'Campus principal',                  'ORGANIZATION_ADMIN', 'ADMIN',  TRUE, TRUE, 7, 'system', 'system', 0),
(9, 'ROLE_RECEPTIONIST','Receptionist',             'Front-desk receptionist',           'STAFF',              'STAFF',  TRUE, TRUE, 8, 'system', 'system', 0),
(10,'ROLE_ACCOUNTANT',  'Accountant',               'Finance and fee management',        'STAFF',              'STAFF',  TRUE, TRUE, 9, 'system', 'system', 0);

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(1,  'DASHBOARD',           'Dashboard',          'Main dashboard',                '/dashboard',               'pi pi-home',              'MODULE', NULL, 1,  TRUE, TRUE, TRUE,  'system', 'system', 0),
(2,  'ACCESS',              'Access',             'Access management',             '/access-management',       'pi pi-shield',            'MODULE', NULL, 2,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(3,  'STUDENTS',            'Students',           'Student module',                '/students',                'pi pi-users',             'MODULE', NULL, 3,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(4,  'STAFF',               'Staff',              'Staff module',                  '/staff',                   'pi pi-id-card',           'MODULE', NULL, 4,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(5,  'ATTENDANCE',          'Attendance',         'Attendance module',             '/attendance',              'pi pi-calendar-check',    'MODULE', NULL, 5,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(6,  'ACADEMICS',           'Academics',          'Academics module',              '/academics',               'pi pi-book',              'MODULE', NULL, 6,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(7,  'ADMISSION',           'Admission',          'Admission module',              '/admission',               'pi pi-inbox',             'MODULE', NULL, 7,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(8,  'FEES',                'Fees',               'Fee management',                '/fees',                    'pi pi-wallet',            'MODULE', NULL, 8,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(9,  'EXAMS',               'Exams',              'Exam management',               '/exam-management',         'pi pi-file-check',        'MODULE', NULL, 9,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(10, 'COMMUNICATION',       'Communication',      'Notices and messages',          '/communication',           'pi pi-send',              'MODULE', NULL, 10, TRUE, TRUE, FALSE, 'system', 'system', 0),
(11, 'ENROLLMENT',          'Enrollment',         'Student enrollment',            '/enrollment-management',   'pi pi-user-plus',         'MODULE', NULL, 11, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: ADMISSION
(20, 'ADMISSION_OVERVIEW',  'Admission Overview', 'Overview',                      '/admission/overview',      'pi pi-chart-bar',         'PAGE',   7,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(21, 'ADMISSION_INQUIRIES', 'Inquiries',          'Admission inquiries',           '/admission/inquiries',     'pi pi-question-circle',   'PAGE',   7,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(22, 'ADMISSION_APPLY',     'Applications',       'Admission applications',        '/admission/applications',  'pi pi-file-edit',         'PAGE',   7,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: STUDENTS
(30, 'STUDENTS_DIRECTORY',  'Student Directory',  'All students',                  '/students/directory',      'pi pi-list',              'PAGE',   3,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(31, 'STUDENTS_TRANSFERS',  'Transfers',          'Transfer requests',             '/students/transfers',      'pi pi-arrow-right-arrow-left','PAGE',3,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(32, 'STUDENTS_DOCUMENTS',  'Documents',          'Student documents',             '/students/documents',      'pi pi-file',              'PAGE',   3,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: STAFF
(40, 'STAFF_DIRECTORY',     'Staff Directory',    'All staff',                     '/staff/directory',         'pi pi-list',              'PAGE',   4,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(41, 'STAFF_RESPONSIBILITIES','Responsibilities', 'Staff responsibilities',        '/staff/responsibilities',  'pi pi-sitemap',           'PAGE',   4,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(42, 'STAFF_PAYROLL',       'Payroll',            'Staff payroll',                 '/staff/payroll',           'pi pi-credit-card',       'PAGE',   4,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(43, 'STAFF_LEAVE',         'Leave',              'Staff leave',                   '/staff/leave',             'pi pi-calendar-times',    'PAGE',   4,  4, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: ATTENDANCE
(50, 'ATTENDANCE_STUDENTS', 'Student Attendance', 'Student attendance',            '/attendance/students',     'pi pi-check-square',      'PAGE',   5,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(51, 'ATTENDANCE_STAFF',    'Staff Attendance',   'Staff attendance',              '/attendance/staff',        'pi pi-check-circle',      'PAGE',   5,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(52, 'ATTENDANCE_REPORTS',  'Reports',            'Attendance reports',            '/attendance/reports',      'pi pi-chart-line',        'PAGE',   5,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: ACADEMICS
(60, 'ACADEMICS_SETUP',     'Academic Setup',     'Year and class setup',          '/academics/setup',         'pi pi-cog',               'PAGE',   6,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(61, 'ACADEMICS_TIMETABLE', 'Timetable',          'Class timetable',               '/academics/timetable',     'pi pi-calendar',          'PAGE',   6,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(62, 'ACADEMICS_SYLLABUS',  'Syllabus',           'Course syllabus',               '/academics/syllabus',      'pi pi-book',              'PAGE',   6,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: ACCESS
(70, 'ACCESS_OVERVIEW',     'Overview',           'Access overview',               '/access-management/overview','pi pi-home',            'PAGE',   2,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(71, 'ACCESS_ROLES',        'Roles',              'Role management',               '/access-management/roles', 'pi pi-tag',               'PAGE',   2,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(72, 'ACCESS_MENUS',        'Menus',              'Menu management',               '/access-management/menus', 'pi pi-bars',              'PAGE',   2,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(73, 'ACCESS_USERS',        'Users',              'User management',               '/access-management/users', 'pi pi-user',              'PAGE',   2,  4, TRUE, TRUE, FALSE, 'system', 'system', 0),
(74, 'ACCESS_SECURITY',     'Security',           'Security policy',               '/access-management/security','pi pi-lock',            'PAGE',   2,  5, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: FEES
(80, 'FEES_OVERVIEW',       'Fee Overview',       'Fee dashboard',                 '/fees/overview',           'pi pi-home',              'PAGE',   8,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(81, 'FEES_SETUP',          'Fee Setup',          'Configure fee structure',       '/fees/setup',              'pi pi-cog',               'PAGE',   8,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(82, 'FEES_LEDGER',         'Ledger',             'Fee ledger',                    '/fees/ledger',             'pi pi-book',              'PAGE',   8,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(83, 'FEES_PAYMENTS',       'Payments',           'Collect payments',              '/fees/payments',           'pi pi-credit-card',       'PAGE',   8,  4, TRUE, TRUE, FALSE, 'system', 'system', 0),
(84, 'FEES_RECEIPTS',       'Receipts',           'Payment receipts',              '/fees/receipts',           'pi pi-receipt',           'PAGE',   8,  5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(85, 'FEES_ADJUSTMENTS',    'Adjustments',        'Fee concessions',               '/fees/adjustments',        'pi pi-percentage',        'PAGE',   8,  6, TRUE, TRUE, FALSE, 'system', 'system', 0),
(86, 'FEES_REPORTS',        'Fee Reports',        'Fee reports',                   '/fees/reports',            'pi pi-chart-bar',         'PAGE',   8,  7, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Sub-pages: COMMUNICATION
(100,'COMM_NOTICES',        'Notices',            'School notices',                '/communication/notices',   'pi pi-bell',              'PAGE',   10, 1, TRUE, TRUE, FALSE, 'system', 'system', 0);

-- ============================================================
-- 2. PLATFORM REFERENCE — Customer & Organization
-- ============================================================

INSERT IGNORE INTO customers (
    id, customer_code, legal_name, display_name, customer_type, status,
    email, mobile_number, alternate_mobile_number, website,
    address_line_1, address_line_2, city, state, country, postal_code,
    preferred_communication, onboarding_completed, active, created_by, updated_by, version
)
VALUES
(1, 'CUS000001', 'Javier Education Group', 'Javier Education Group', 'EDUCATION_GROUP', 'ACTIVE',
 'founder@javier.edu.in', '9777001100', '9437001100', 'https://javier.edu.in',
 'Plot No. 12, Patia', 'Near Infocity', 'Bhubaneswar', 'Odisha', 'India', '751024',
 'WHATSAPP', TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO organizations (
    id, organization_code, customer_id, organization_name, short_name, institution_type, board_name,
    email, mobile_number, website, address_line_1, address_line_2, city, state, country, postal_code,
    time_zone, currency, language, logo_url, status, active, onboarding_completed, created_by, updated_by, version
)
VALUES
(1, 'ORG000001', 1, 'Javier School Bhubaneswar', 'JSB', 'SCHOOL', 'CBSE',
 'principal@jsb.edu.in', '9777111100', 'https://jsb.edu.in',
 'Plot 12, Patia', 'Near Infocity', 'Bhubaneswar', 'Odisha', 'India', '751024',
 'Asia/Kolkata', 'INR', 'en-IN', 'https://cdn.thinkerscave.local/org/jsb.png',
 'ACTIVE', TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO tenant_registry (
    id, tenant_identifier, organization_id, schema_name, database_version, migration_version,
    provision_status, active, created_by, updated_by, version
)
VALUES
(1, 'jsb-bhubaneswar', 1, 'tenant_jsb_bhubaneswar', '1.0', '1.0', 'COMPLETED', TRUE, 'system', 'system', 0);

INSERT IGNORE INTO organization_configurations (
    id, organization_id, default_academic_year, academic_year_start_month, student_code_pattern,
    employee_code_pattern, admission_number_pattern, receipt_number_pattern, invoice_number_pattern,
    currency, time_zone, language, date_format, created_by, updated_by, version
)
VALUES
(1, 1, '2026-27', 4, 'JSB/STU/{YY}/{SEQ}', 'JSB/EMP/{YY}/{SEQ}', 'JSB/ADM/{YY}/{SEQ}',
 'JSB/REC/{YY}/{SEQ}', 'JSB/INV/{YY}/{SEQ}', 'INR', 'Asia/Kolkata', 'en-IN', 'dd-MM-yyyy',
 'system', 'system', 0);

INSERT IGNORE INTO subscription_plans (
    id, plan_code, plan_name, monthly_price, yearly_price, student_limit, staff_limit,
    branch_limit, storage_limit_gb, trial_days, display_order, recommended, visible, active,
    created_by, updated_by, version
)
VALUES
(2, 'GROWTH', 'Growth Plan', 2999.00, 25999.00, 1200, 200, 5, 200, 14, 2, TRUE, TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO organization_subscriptions (
    id, organization_id, subscription_plan_id, start_date, end_date, billing_cycle,
    plan_price, discount_amount, final_amount, student_limit_override, staff_limit_override,
    auto_renew, status, active, created_by, updated_by, version
)
VALUES
(1, 1, 2, '2026-01-01', '2026-12-31', 'YEARLY', 29990.00, 5000.00, 24990.00, 1200, 200, TRUE, 'ACTIVE', TRUE, 'system', 'system', 0);

-- ============================================================
-- 3. SECURITY POLICY
-- ============================================================

INSERT IGNORE INTO security_policies (
    id, organization_id, min_password_length, require_uppercase, require_lowercase,
    require_numbers, require_special_chars, password_expiry_days, password_history_count,
    max_failed_attempts, lockout_duration_minutes, session_timeout_minutes,
    max_concurrent_sessions, allow_remember_me, require_two_factor, active, created_by, updated_by, version
)
VALUES
(1, 1, 8, TRUE, TRUE, TRUE, FALSE, 90, 5, 5, 30, 60, 3, TRUE, FALSE, TRUE, 'system', 'system', 0);

-- ============================================================
-- 4. USERS  (passwords must be set via app encoder / ops reset — not mass-overwritten on startup)
-- ============================================================

INSERT IGNORE INTO users (
    id, organization_id, user_code, username, email, mobile_number, password,
    first_name, last_name, display_name, status, email_verified, mobile_verified,
    first_time_login, failed_login_attempts, account_locked, created_by, updated_by, version
)
VALUES
(1, 1, 'USR000001', 'javier.owner',     'owner@jsb.edu.in',     '9777111100', 'PLACEHOLDER', 'Sanjay',   'Mohanty',    'Sanjay Mohanty',    'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(2, 1, 'USR000002', 'javier.admin',     'admin@jsb.edu.in',     '9777111101', 'PLACEHOLDER', 'Ananya',   'Dash',       'Ananya Dash',       'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(3, 1, 'USR000003', 'javier.principal', 'principal@jsb.edu.in', '9777111102', 'PLACEHOLDER', 'Ramesh',   'Mohapatra',  'Ramesh Mohapatra',  'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(4, 1, 'USR000004', 'javier.teacher1',  'teacher1@jsb.edu.in',  '9777111201', 'PLACEHOLDER', 'Rupesh',   'Pati',       'Rupesh Pati',       'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(5, 1, 'USR000005', 'javier.teacher2',  'teacher2@jsb.edu.in',  '9777111202', 'PLACEHOLDER', 'Saswati',  'Senapati',   'Saswati Senapati',  'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(6, 1, 'USR000006', 'javier.receptionist','reception@jsb.edu.in','9777111301','PLACEHOLDER', 'Lipika',   'Rath',       'Lipika Rath',       'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(7, 1, 'USR000007', 'javier.accountant','accounts@jsb.edu.in',  '9777111302', 'PLACEHOLDER', 'Bikash',   'Rout',       'Bikash Rout',       'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(8, 1, 'USR000008', 'javier.student1',  'student1@jsb.edu.in',  '9777111401', 'PLACEHOLDER', 'Aarav',    'Mohanty',    'Aarav Mohanty',     'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0),
(9, 1, 'USR000009', 'javier.parent1',   'parent1@jsb.edu.in',   '9777111501', 'PLACEHOLDER', 'Ranjit',   'Mohanty',    'Ranjit Mohanty',    'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, 'system', 'system', 0);

INSERT IGNORE INTO user_roles (id, user_id, role_id, primary_role, active, created_by, updated_by, version)
VALUES
(1, 1, 1,  TRUE,  TRUE, 'system', 'system', 0),  -- Owner
(2, 2, 2,  TRUE,  TRUE, 'system', 'system', 0),  -- Admin
(3, 3, 8,  TRUE,  TRUE, 'system', 'system', 0),  -- Principal
(4, 4, 7,  TRUE,  TRUE, 'system', 'system', 0),  -- Teacher
(5, 5, 7,  TRUE,  TRUE, 'system', 'system', 0),  -- Teacher
(6, 6, 9,  TRUE,  TRUE, 'system', 'system', 0),  -- Receptionist
(7, 7, 10, TRUE,  TRUE, 'system', 'system', 0),  -- Accountant
(8, 8, 4,  TRUE,  TRUE, 'system', 'system', 0),  -- Student
(9, 9, 5,  TRUE,  TRUE, 'system', 'system', 0);  -- Parent

-- ============================================================
-- 5. ROLE PERMISSIONS (comprehensive — all modules for JSB)
-- ============================================================

INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
-- OWNER (role 1) — full access to all menus
(10, 1, 1, 1,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(11, 1, 1, 2,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(12, 1, 1, 3,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(13, 1, 1, 4,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(14, 1, 1, 5,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(15, 1, 1, 6,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(16, 1, 1, 7,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(17, 1, 1, 8,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(18, 1, 1, 9,  TRUE, TRUE, TRUE, 'system', 'system', 0),
(19, 1, 1, 10, TRUE, TRUE, TRUE, 'system', 'system', 0),
(20, 1, 1, 11, TRUE, TRUE, TRUE, 'system', 'system', 0),
(21, 1, 1, 20, TRUE, TRUE, TRUE, 'system', 'system', 0),
(22, 1, 1, 21, TRUE, TRUE, TRUE, 'system', 'system', 0),
(23, 1, 1, 22, TRUE, TRUE, TRUE, 'system', 'system', 0),
(24, 1, 1, 30, TRUE, TRUE, TRUE, 'system', 'system', 0),
(25, 1, 1, 31, TRUE, TRUE, TRUE, 'system', 'system', 0),
(26, 1, 1, 32, TRUE, TRUE, TRUE, 'system', 'system', 0),
(27, 1, 1, 40, TRUE, TRUE, TRUE, 'system', 'system', 0),
(28, 1, 1, 41, TRUE, TRUE, TRUE, 'system', 'system', 0),
(29, 1, 1, 42, TRUE, TRUE, TRUE, 'system', 'system', 0),
(30, 1, 1, 43, TRUE, TRUE, TRUE, 'system', 'system', 0),
(31, 1, 1, 50, TRUE, TRUE, TRUE, 'system', 'system', 0),
(32, 1, 1, 51, TRUE, TRUE, TRUE, 'system', 'system', 0),
(33, 1, 1, 52, TRUE, TRUE, TRUE, 'system', 'system', 0),
(34, 1, 1, 60, TRUE, TRUE, TRUE, 'system', 'system', 0),
(35, 1, 1, 61, TRUE, TRUE, TRUE, 'system', 'system', 0),
(36, 1, 1, 62, TRUE, TRUE, TRUE, 'system', 'system', 0),
(37, 1, 1, 70, TRUE, TRUE, TRUE, 'system', 'system', 0),
(38, 1, 1, 71, TRUE, TRUE, TRUE, 'system', 'system', 0),
(39, 1, 1, 72, TRUE, TRUE, TRUE, 'system', 'system', 0),
(40, 1, 1, 73, TRUE, TRUE, TRUE, 'system', 'system', 0),
(41, 1, 1, 74, TRUE, TRUE, TRUE, 'system', 'system', 0),
(42, 1, 1, 80, TRUE, TRUE, TRUE, 'system', 'system', 0),
(43, 1, 1, 81, TRUE, TRUE, TRUE, 'system', 'system', 0),
(44, 1, 1, 82, TRUE, TRUE, TRUE, 'system', 'system', 0),
(45, 1, 1, 83, TRUE, TRUE, TRUE, 'system', 'system', 0),
(46, 1, 1, 84, TRUE, TRUE, TRUE, 'system', 'system', 0),
(47, 1, 1, 85, TRUE, TRUE, TRUE, 'system', 'system', 0),
(48, 1, 1, 86, TRUE, TRUE, TRUE, 'system', 'system', 0),
(49, 1, 1, 100,TRUE, TRUE, TRUE, 'system', 'system', 0),
-- ADMIN (role 2) — full org-level access, no security config
(60, 1, 2, 1,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(61, 1, 2, 2,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(62, 1, 2, 3,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(63, 1, 2, 4,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(64, 1, 2, 5,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(65, 1, 2, 6,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(66, 1, 2, 7,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(67, 1, 2, 8,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(68, 1, 2, 9,  TRUE, TRUE, FALSE, 'system', 'system', 0),
(69, 1, 2, 10, TRUE, TRUE, FALSE, 'system', 'system', 0),
(70, 1, 2, 11, TRUE, TRUE, FALSE, 'system', 'system', 0),
(71, 1, 2, 20, TRUE, TRUE, FALSE, 'system', 'system', 0),
(72, 1, 2, 21, TRUE, TRUE, FALSE, 'system', 'system', 0),
(73, 1, 2, 22, TRUE, TRUE, FALSE, 'system', 'system', 0),
(74, 1, 2, 30, TRUE, TRUE, FALSE, 'system', 'system', 0),
(75, 1, 2, 31, TRUE, TRUE, FALSE, 'system', 'system', 0),
(76, 1, 2, 32, TRUE, TRUE, FALSE, 'system', 'system', 0),
(77, 1, 2, 40, TRUE, TRUE, FALSE, 'system', 'system', 0),
(78, 1, 2, 41, TRUE, TRUE, FALSE, 'system', 'system', 0),
(79, 1, 2, 42, TRUE, TRUE, TRUE,  'system', 'system', 0),
(80, 1, 2, 43, TRUE, TRUE, FALSE, 'system', 'system', 0),
(81, 1, 2, 50, TRUE, TRUE, FALSE, 'system', 'system', 0),
(82, 1, 2, 51, TRUE, TRUE, FALSE, 'system', 'system', 0),
(83, 1, 2, 52, TRUE, TRUE, FALSE, 'system', 'system', 0),
(84, 1, 2, 60, TRUE, TRUE, FALSE, 'system', 'system', 0),
(85, 1, 2, 61, TRUE, TRUE, FALSE, 'system', 'system', 0),
(86, 1, 2, 62, TRUE, TRUE, FALSE, 'system', 'system', 0),
(87, 1, 2, 70, TRUE, TRUE, FALSE, 'system', 'system', 0),
(88, 1, 2, 71, TRUE, TRUE, FALSE, 'system', 'system', 0),
(89, 1, 2, 73, TRUE, TRUE, FALSE, 'system', 'system', 0),
(90, 1, 2, 80, TRUE, TRUE, FALSE, 'system', 'system', 0),
(91, 1, 2, 81, TRUE, TRUE, FALSE, 'system', 'system', 0),
(92, 1, 2, 82, TRUE, TRUE, FALSE, 'system', 'system', 0),
(93, 1, 2, 83, TRUE, TRUE, TRUE,  'system', 'system', 0),
(94, 1, 2, 84, TRUE, TRUE, FALSE, 'system', 'system', 0),
(95, 1, 2, 85, TRUE, TRUE, TRUE,  'system', 'system', 0),
(96, 1, 2, 86, TRUE, TRUE, FALSE, 'system', 'system', 0),
(97, 1, 2, 100,TRUE, TRUE, FALSE, 'system', 'system', 0),
-- PRINCIPAL (role 8)
(110,1, 8, 1,  TRUE, FALSE, FALSE,'system', 'system', 0),
(111,1, 8, 3,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(112,1, 8, 4,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(113,1, 8, 5,  TRUE, TRUE,  TRUE, 'system', 'system', 0),
(114,1, 8, 6,  TRUE, TRUE,  TRUE, 'system', 'system', 0),
(115,1, 8, 7,  TRUE, TRUE,  TRUE, 'system', 'system', 0),
(116,1, 8, 9,  TRUE, TRUE,  TRUE, 'system', 'system', 0),
(117,1, 8, 10, TRUE, TRUE,  FALSE,'system', 'system', 0),
(118,1, 8, 30, TRUE, TRUE,  FALSE,'system', 'system', 0),
(119,1, 8, 40, TRUE, TRUE,  FALSE,'system', 'system', 0),
(120,1, 8, 50, TRUE, TRUE,  TRUE, 'system', 'system', 0),
(121,1, 8, 60, TRUE, TRUE,  FALSE,'system', 'system', 0),
(122,1, 8, 100,TRUE, TRUE,  FALSE,'system', 'system', 0),
-- TEACHER (role 7)
(130,1, 7, 1,  TRUE, FALSE, FALSE,'system', 'system', 0),
(131,1, 7, 3,  TRUE, FALSE, FALSE,'system', 'system', 0),
(132,1, 7, 5,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(133,1, 7, 6,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(134,1, 7, 9,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(135,1, 7, 10, TRUE, FALSE, FALSE,'system', 'system', 0),
(136,1, 7, 50, TRUE, TRUE,  FALSE,'system', 'system', 0),
(137,1, 7, 60, TRUE, TRUE,  FALSE,'system', 'system', 0),
(138,1, 7, 62, TRUE, TRUE,  FALSE,'system', 'system', 0),
(139,1, 7, 100,TRUE, FALSE, FALSE,'system', 'system', 0),
-- RECEPTIONIST (role 9)
(150,1, 9, 1,  TRUE, FALSE, FALSE,'system', 'system', 0),
(151,1, 9, 3,  TRUE, FALSE, FALSE,'system', 'system', 0),
(152,1, 9, 7,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(153,1, 9, 5,  TRUE, FALSE, FALSE,'system', 'system', 0),
(154,1, 9, 10, TRUE, FALSE, FALSE,'system', 'system', 0),
(155,1, 9, 20, TRUE, TRUE,  FALSE,'system', 'system', 0),
(156,1, 9, 21, TRUE, TRUE,  FALSE,'system', 'system', 0),
(157,1, 9, 22, TRUE, TRUE,  FALSE,'system', 'system', 0),
(158,1, 9, 30, TRUE, FALSE, FALSE,'system', 'system', 0),
(159,1, 9, 100,TRUE, FALSE, FALSE,'system', 'system', 0),
-- ACCOUNTANT (role 10)
(170,1, 10,1,  TRUE, FALSE, FALSE,'system', 'system', 0),
(171,1, 10,3,  TRUE, FALSE, FALSE,'system', 'system', 0),
(172,1, 10,8,  TRUE, TRUE,  TRUE, 'system', 'system', 0),
(173,1, 10,80, TRUE, TRUE,  FALSE,'system', 'system', 0),
(174,1, 10,81, TRUE, TRUE,  FALSE,'system', 'system', 0),
(175,1, 10,82, TRUE, TRUE,  FALSE,'system', 'system', 0),
(176,1, 10,83, TRUE, TRUE,  TRUE, 'system', 'system', 0),
(177,1, 10,84, TRUE, TRUE,  FALSE,'system', 'system', 0),
(178,1, 10,85, TRUE, TRUE,  TRUE, 'system', 'system', 0),
(179,1, 10,86, TRUE, TRUE,  FALSE,'system', 'system', 0),
(180,1, 10,100,TRUE, FALSE, FALSE,'system', 'system', 0),
-- STAFF (role 3)
(190,1, 3, 1,  TRUE, FALSE, FALSE,'system', 'system', 0),
(191,1, 3, 3,  TRUE, FALSE, FALSE,'system', 'system', 0),
(192,1, 3, 5,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(193,1, 3, 6,  TRUE, TRUE,  FALSE,'system', 'system', 0),
(194,1, 3, 9,  TRUE, FALSE, FALSE,'system', 'system', 0),
(195,1, 3, 10, TRUE, FALSE, FALSE,'system', 'system', 0),
(196,1, 3, 50, TRUE, TRUE,  FALSE,'system', 'system', 0),
(197,1, 3, 100,TRUE, FALSE, FALSE,'system', 'system', 0),
-- STUDENT (role 4)
(200,1, 4, 1,  TRUE, FALSE, FALSE,'system', 'system', 0),
(201,1, 4, 5,  TRUE, FALSE, FALSE,'system', 'system', 0),
(202,1, 4, 6,  TRUE, FALSE, FALSE,'system', 'system', 0),
(203,1, 4, 9,  TRUE, FALSE, FALSE,'system', 'system', 0),
(204,1, 4, 10, TRUE, FALSE, FALSE,'system', 'system', 0),
(205,1, 4, 50, TRUE, FALSE, FALSE,'system', 'system', 0),
(206,1, 4, 82, TRUE, FALSE, FALSE,'system', 'system', 0),
(207,1, 4, 84, TRUE, FALSE, FALSE,'system', 'system', 0),
(208,1, 4, 100,TRUE, FALSE, FALSE,'system', 'system', 0),
-- PARENT (role 5)
(210,1, 5, 1,  TRUE, FALSE, FALSE,'system', 'system', 0),
(211,1, 5, 5,  TRUE, FALSE, FALSE,'system', 'system', 0),
(212,1, 5, 9,  TRUE, FALSE, FALSE,'system', 'system', 0),
(213,1, 5, 10, TRUE, FALSE, FALSE,'system', 'system', 0),
(214,1, 5, 82, TRUE, FALSE, FALSE,'system', 'system', 0),
(215,1, 5, 84, TRUE, FALSE, FALSE,'system', 'system', 0),
(216,1, 5, 100,TRUE, FALSE, FALSE,'system', 'system', 0);

-- ============================================================
-- 6. ORGANIZATION MODULES (enabled modules for JSB)
-- ============================================================

INSERT IGNORE INTO organization_modules (id, organization_id, menu_id, enabled, created_by, updated_by, version)
VALUES
(1, 1, 1,  TRUE, 'system', 'system', 0),
(2, 1, 2,  TRUE, 'system', 'system', 0),
(3, 1, 3,  TRUE, 'system', 'system', 0),
(4, 1, 4,  TRUE, 'system', 'system', 0),
(5, 1, 5,  TRUE, 'system', 'system', 0),
(6, 1, 6,  TRUE, 'system', 'system', 0),
(7, 1, 7,  TRUE, 'system', 'system', 0),
(8, 1, 8,  TRUE, 'system', 'system', 0),
(9, 1, 9,  TRUE, 'system', 'system', 0),
(10,1, 10, TRUE, 'system', 'system', 0),
(11,1, 11, TRUE, 'system', 'system', 0);

-- ============================================================
-- 7. ACADEMIC STRUCTURE
-- ============================================================

INSERT IGNORE INTO academic_year (academic_year_id, year_code, year_name, start_date, end_date, current_year, active, created_by, updated_by, version)
VALUES
(1, 'AY2026', 'Academic Year 2026-27', '2026-04-01', '2027-03-31', TRUE,  TRUE, 'system', 'system', 0),
(2, 'AY2025', 'Academic Year 2025-26', '2025-04-01', '2026-03-31', FALSE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO academic_class (class_id, academic_year_id, class_code, class_name, academic_stage, display_order, active, created_by, updated_by, version)
VALUES
(1, 1, 'NURSERY', 'Nursery',     'PRE_PRIMARY',      1, TRUE, 'system', 'system', 0),
(2, 1, 'LKG',     'Lower KG',   'PRE_PRIMARY',      2, TRUE, 'system', 'system', 0),
(3, 1, 'UKG',     'Upper KG',   'PRE_PRIMARY',      3, TRUE, 'system', 'system', 0),
(4, 1, 'I',       'Class I',    'PRIMARY',           4, TRUE, 'system', 'system', 0),
(5, 1, 'II',      'Class II',   'PRIMARY',           5, TRUE, 'system', 'system', 0),
(6, 1, 'III',     'Class III',  'PRIMARY',           6, TRUE, 'system', 'system', 0),
(7, 1, 'IV',      'Class IV',   'PRIMARY',           7, TRUE, 'system', 'system', 0),
(8, 1, 'V',       'Class V',    'UPPER_PRIMARY',     8, TRUE, 'system', 'system', 0),
(9, 1, 'VI',      'Class VI',   'UPPER_PRIMARY',     9, TRUE, 'system', 'system', 0),
(10,1, 'X',       'Class X',    'SECONDARY',        10, TRUE, 'system', 'system', 0),
(11,1, 'XII-SCI', 'Class XII Science','SENIOR_SECONDARY',11,TRUE,'system','system', 0);

INSERT IGNORE INTO academic_section (section_id, class_id, section_name, capacity, active, created_by, updated_by, version)
VALUES
(1,  4,  'A', 45, TRUE, 'system', 'system', 0),
(2,  4,  'B', 45, TRUE, 'system', 'system', 0),
(3,  4,  'C', 40, TRUE, 'system', 'system', 0),
(4,  5,  'A', 45, TRUE, 'system', 'system', 0),
(5,  5,  'B', 45, TRUE, 'system', 'system', 0),
(6,  6,  'A', 45, TRUE, 'system', 'system', 0),
(7,  6,  'B', 40, TRUE, 'system', 'system', 0),
(8,  7,  'A', 45, TRUE, 'system', 'system', 0),
(9,  8,  'A', 45, TRUE, 'system', 'system', 0),
(10, 9,  'A', 45, TRUE, 'system', 'system', 0),
(11, 10, 'A', 40, TRUE, 'system', 'system', 0),
(12, 11, 'A', 40, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO subject (subject_id, subject_code, subject_name, subject_type, active, created_by, updated_by, version)
VALUES
(1, 'ENG', 'English',            'CORE',     TRUE, 'system', 'system', 0),
(2, 'MTH', 'Mathematics',        'CORE',     TRUE, 'system', 'system', 0),
(3, 'SCI', 'Science',            'CORE',     TRUE, 'system', 'system', 0),
(4, 'SST', 'Social Studies',     'CORE',     TRUE, 'system', 'system', 0),
(5, 'ORI', 'Odia',               'LANGUAGE', TRUE, 'system', 'system', 0),
(6, 'COM', 'Computer',           'SKILL',    TRUE, 'system', 'system', 0),
(7, 'HIN', 'Hindi',              'LANGUAGE', TRUE, 'system', 'system', 0),
(8, 'GK',  'General Knowledge',  'SKILL',    TRUE, 'system', 'system', 0),
(9, 'PHY', 'Physics',            'CORE',     TRUE, 'system', 'system', 0),
(10,'CHM', 'Chemistry',          'CORE',     TRUE, 'system', 'system', 0);

-- ============================================================
-- 8. STAFF
-- ============================================================

INSERT IGNORE INTO staff (
    staff_id, user_id, staff_code, first_name, last_name, gender, date_of_birth, blood_group,
    mobile_number, email, staff_type, designation, employment_category, employment_status,
    joining_date, highest_qualification, experience_years, active, created_by, updated_by, version
)
VALUES
(1, 1, 'JSB-OWN-001', 'Sanjay',  'Mohanty',   'Male',   '1975-06-14', 'A+',  '9777111100', 'owner@jsb.edu.in',    'NON_TEACHING', 'School Owner',   'PERMANENT', 'ACTIVE', '2018-04-01', 'MBA Finance', 20, TRUE, 'system', 'system', 0),
(2, 2, 'JSB-ADM-001', 'Ananya',  'Dash',      'Female', '1987-05-10', 'O+',  '9777111101', 'admin@jsb.edu.in',    'NON_TEACHING', 'Office Admin',   'PERMANENT', 'ACTIVE', '2020-01-15', 'B.Com, MBA',  11, TRUE, 'system', 'system', 0),
(3, 3, 'JSB-PRL-001', 'Ramesh',  'Mohapatra', 'Male',   '1970-01-22', 'B+',  '9777111102', 'principal@jsb.edu.in','NON_TEACHING', 'Principal',      'PERMANENT', 'ACTIVE', '2019-06-01', 'M.Ed, Ph.D',  22, TRUE, 'system', 'system', 0),
(4, 4, 'JSB-TCH-001', 'Rupesh',  'Pati',      'Male',   '1990-03-14', 'B+',  '9777111201', 'teacher1@jsb.edu.in', 'TEACHING',     'English Teacher','PERMANENT', 'ACTIVE', '2022-06-01', 'M.A. English, B.Ed.', 8, TRUE, 'system', 'system', 0),
(5, 5, 'JSB-TCH-002', 'Saswati', 'Senapati',  'Female', '1992-07-09', 'A+',  '9777111202', 'teacher2@jsb.edu.in', 'TEACHING',     'Maths Teacher',  'PERMANENT', 'ACTIVE', '2023-02-15', 'M.Sc. Math, B.Ed.',   6, TRUE, 'system', 'system', 0),
(6, 6, 'JSB-RCP-001', 'Lipika',  'Rath',      'Female', '1993-09-05', 'O+',  '9777111301', 'reception@jsb.edu.in','NON_TEACHING', 'Receptionist',   'PERMANENT', 'ACTIVE', '2021-04-01', 'B.A., DCA',   7, TRUE, 'system', 'system', 0),
(7, 7, 'JSB-ACC-001', 'Bikash',  'Rout',      'Male',   '1988-02-28', 'AB+', '9777111302', 'accounts@jsb.edu.in', 'NON_TEACHING', 'Accountant',     'PERMANENT', 'ACTIVE', '2020-07-01', 'B.Com, CA Inter', 10, TRUE,'system', 'system', 0);

-- ============================================================
-- 9. STUDENTS & PARENTS
-- ============================================================

INSERT IGNORE INTO parent (
    parent_id, parent_code, first_name, last_name, gender, mobile_number, email,
    occupation, annual_income, active, user_id, created_by, updated_by, version
)
VALUES
(1, 'PAR000001', 'Ranjit',  'Mohanty', 'Male',   '9777111501', 'parent1@jsb.edu.in', 'Business', 950000.00, TRUE, 9, 'system', 'system', 0),
(2, 'PAR000002', 'Sangita', 'Mishra',  'Female', '9777111502', 'parent2@jsb.edu.in', 'Teacher',  720000.00, TRUE, NULL,'system', 'system', 0),
(3, 'PAR000003', 'Sarat',   'Behera',  'Male',   '9777111503', 'parent3@jsb.edu.in', 'Engineer', 850000.00, TRUE, NULL,'system', 'system', 0);

INSERT IGNORE INTO student (
    student_id, student_code, admission_number, roll_number, first_name, last_name, gender,
    date_of_birth, religion, nationality, mother_tongue, mobile_number, email, photo_url,
    admission_date, status, transport_required, hostel_required, same_address, user_id,
    created_by, updated_by, version
)
VALUES
(1, 'JSB-STU-0001', 'JSB-ADM-26001', '1', 'Aarav',  'Mohanty', 'Male',   '2019-05-18', 'Hindu', 'Indian', 'Odia', 9777111401, 'student1@jsb.edu.in', 'https://cdn.thinkerscave.local/students/aarav.png',  '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, 8, 'system', 'system', 0),
(2, 'JSB-STU-0002', 'JSB-ADM-26002', '2', 'Ishani', 'Mishra',  'Female', '2019-08-24', 'Hindu', 'Indian', 'Odia', 9777111402, 'ishani@jsb.edu.in',   'https://cdn.thinkerscave.local/students/ishani.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL,'system', 'system', 0),
(3, 'JSB-STU-0003', 'JSB-ADM-26003', '3', 'Aditya', 'Behera',  'Male',   '2018-12-03', 'Hindu', 'Indian', 'Odia', 9777111403, 'aditya@jsb.edu.in',   'https://cdn.thinkerscave.local/students/aditya.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL,'system', 'system', 0),
(4, 'JSB-STU-0004', 'JSB-ADM-26004', '4', 'Tanvi',  'Nayak',   'Female', '2018-07-13', 'Hindu', 'Indian', 'Odia', 9777111404, 'tanvi@jsb.edu.in',    'https://cdn.thinkerscave.local/students/tanvi.png',  '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL,'system', 'system', 0),
(5, 'JSB-STU-0005', 'JSB-ADM-26005', '5', 'Sambit', 'Behera',  'Male',   '2018-04-02', 'Hindu', 'Indian', 'Odia', 9777111405, 'sambit@jsb.edu.in',   'https://cdn.thinkerscave.local/students/sambit.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL,'system', 'system', 0),
(6, 'JSB-STU-0006', 'JSB-ADM-26006', '1', 'Pria',   'Jena',    'Female', '2017-10-11', 'Hindu', 'Indian', 'Odia', 9777111406, 'priya@jsb.edu.in',    'https://cdn.thinkerscave.local/students/priya.png',  '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL,'system', 'system', 0);

INSERT IGNORE INTO student_parents (id, student_id, parent_id, relation, primary_contact, created_by, updated_by, version)
VALUES
(1, 1, 1, 'FATHER', TRUE,  'system', 'system', 0),
(2, 1, 2, 'MOTHER', FALSE, 'system', 'system', 0),
(3, 2, 2, 'MOTHER', TRUE,  'system', 'system', 0),
(4, 3, 3, 'FATHER', TRUE,  'system', 'system', 0);

INSERT IGNORE INTO student_enrollment (
    enrollment_id, student_id, academic_year_id, class_id, section_id, roll_number, status, active, created_by, updated_by, version
)
VALUES
(1, 1, 1, 4, 1, '1', 'ACTIVE', TRUE, 'system', 'system', 0),
(2, 2, 1, 4, 1, '2', 'ACTIVE', TRUE, 'system', 'system', 0),
(3, 3, 1, 5, 4, '1', 'ACTIVE', TRUE, 'system', 'system', 0),
(4, 4, 1, 5, 4, '2', 'ACTIVE', TRUE, 'system', 'system', 0),
(5, 5, 1, 4, 2, '1', 'ACTIVE', TRUE, 'system', 'system', 0),
(6, 6, 1, 6, 6, '1', 'ACTIVE', TRUE, 'system', 'system', 0);

-- ============================================================
-- 10. ATTENDANCE — sample records
-- ============================================================

INSERT IGNORE INTO attendance_settings (id, organization_id, attendance_mode, late_threshold_minutes, half_day_threshold_minutes, active, created_by, updated_by, version)
VALUES
(1, 1, 'DAILY', 15, 180, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO student_attendance (
    attendance_id, student_id, academic_year_id, class_id, section_id, attendance_date, status, remarks, marked_by, created_by, updated_by, version
)
VALUES
(1, 1, 1, 4, 1, '2026-07-07', 'PRESENT', NULL, 4, 'system', 'system', 0),
(2, 2, 1, 4, 1, '2026-07-07', 'PRESENT', NULL, 4, 'system', 'system', 0),
(3, 3, 1, 5, 4, '2026-07-07', 'ABSENT',  'Sick leave', 5, 'system', 'system', 0),
(4, 4, 1, 5, 4, '2026-07-07', 'PRESENT', NULL, 5, 'system', 'system', 0),
(5, 1, 1, 4, 1, '2026-07-08', 'PRESENT', NULL, 4, 'system', 'system', 0),
(6, 2, 1, 4, 1, '2026-07-08', 'LATE',    'Arrived 20 mins late', 4, 'system', 'system', 0);

-- ============================================================
-- 11. ADMISSION INQUIRIES — sample
-- ============================================================

INSERT IGNORE INTO inquiry (
    inquiry_id, inquiry_code, organization_id, student_name, student_dob, parent_name,
    parent_mobile, email, class_interested, source, status, last_follow_up_date,
    last_follow_up_type, remarks, assigned_to, created_by, updated_by, version
)
VALUES
(1, 'INQ-JSB-001', 1, 'Arya Sharma',   '2020-03-10', 'Suresh Sharma',  '9438001001', 'arya@mail.com', 'Nursery', 'WALK_IN',   'NEW',       NULL, NULL, 'Interested in CBSE curriculum', 3, 'system', 'system', 0),
(2, 'INQ-JSB-002', 1, 'Nishant Panda', '2019-11-18', 'Bijay Panda',    '9438001002', 'nish@mail.com', 'Class I', 'REFERENCE', 'CONTACTED', '2026-07-01', 'CALL', 'Good family', 3, 'system', 'system', 0),
(3, 'INQ-JSB-003', 1, 'Prachi Sahu',   '2018-06-22', 'Sita Sahu',      '9438001003', NULL,            'Class II','ONLINE',    'CONVERTED', '2026-07-03', 'VISIT','Admission confirmed', 6, 'system', 'system', 0);

INSERT IGNORE INTO inquiry_follow_up (follow_up_id, inquiry_id, follow_up_date, follow_up_type, notes, next_follow_up_date, done_by, created_by, updated_by, version)
VALUES
(1, 2, '2026-07-01', 'CALL',  'Called parent, interested in visiting campus', '2026-07-05', 3, 'system', 'system', 0),
(2, 3, '2026-07-03', 'VISIT', 'Parent visited campus, liked facilities, paid registration', NULL, 3, 'system', 'system', 0);

-- ============================================================
-- 12. COMMUNICATION — notices
-- ============================================================

INSERT IGNORE INTO notices (
    notice_id, organization_id, title, content, notice_type, status, publish_date, expiry_date,
    created_by_user_id, created_by, updated_by, version
)
VALUES
(1, 1, 'School Reopening Notice', 'School will reopen on 1st April 2026 after summer break. All students must attend.', 'GENERAL', 'PUBLISHED', '2026-03-25', '2026-04-05', 2, 'system', 'system', 0),
(2, 1, 'Parent Teacher Meeting', 'PTM scheduled for 10th August 2026. All parents are requested to attend.', 'EVENT', 'PUBLISHED', '2026-07-28', '2026-08-11', 3, 'system', 'system', 0),
(3, 1, 'Mid Term Exam Schedule', 'Mid term examinations will be held from 15th September 2026. Detailed schedule attached.', 'EXAM', 'PUBLISHED', '2026-08-30', '2026-09-22', 3, 'system', 'system', 0);

INSERT IGNORE INTO notice_audience (id, notice_id, audience_type, created_by, updated_by, version)
VALUES
(1, 1, 'ALL',     'system', 'system', 0),
(2, 2, 'PARENTS', 'system', 'system', 0),
(3, 3, 'ALL',     'system', 'system', 0);
