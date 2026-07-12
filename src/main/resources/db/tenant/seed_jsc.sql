-- ============================================================
-- TENANT: Javier School Cuttack (jsc-cuttack)
-- Schema: tenant_jsc_cuttack   Organization ID: 2
-- ============================================================

-- 1. REFERENCE DATA
INSERT IGNORE INTO roles (id, role_code, role_name, description, role_type, dashboard_code, system_role, active, display_order, created_by, updated_by, version)
VALUES
(1, 'ROLE_OWNER',  'Organization Owner','Full access', 'ORGANIZATION_OWNER','ADMIN',  TRUE,TRUE,1,'system','system',0),
(2, 'ROLE_ADMIN',  'Organization Admin', 'Admin',      'ORGANIZATION_ADMIN','ADMIN',  TRUE,TRUE,2,'system','system',0),
(3, 'ROLE_STAFF',  'Staff',             'Staff',       'STAFF',             'STAFF',  TRUE,TRUE,3,'system','system',0),
(4, 'ROLE_STUDENT','Student',           'Student',     'STUDENT',           'STUDENT',TRUE,TRUE,4,'system','system',0),
(5, 'ROLE_PARENT', 'Parent',            'Parent',      'PARENT',            'PARENT', TRUE,TRUE,5,'system','system',0),
(7, 'ROLE_TEACHER','Teacher',           'Teacher',     'STAFF',             'STAFF',  TRUE,TRUE,6,'system','system',0),
(9, 'ROLE_RECEPTIONIST','Receptionist', 'Receptionist','STAFF',             'STAFF',  TRUE,TRUE,8,'system','system',0);

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(1,  'DASHBOARD',           'Dashboard',     'Main dashboard',        '/dashboard',              'pi pi-home',           'MODULE',NULL, 1,TRUE,TRUE,TRUE, 'system','system',0),
(2,  'ACCESS',              'Access',         'Access management',     '/access-management',      'pi pi-shield',         'MODULE',NULL, 2,TRUE,TRUE,FALSE,'system','system',0),
(3,  'STUDENTS',            'Students',       'Student module',        '/students',               'pi pi-users',          'MODULE',NULL, 3,TRUE,TRUE,FALSE,'system','system',0),
(4,  'STAFF',               'Staff',          'Staff module',          '/staff',                  'pi pi-id-card',        'MODULE',NULL, 4,TRUE,TRUE,FALSE,'system','system',0),
(5,  'ATTENDANCE',          'Attendance',     'Attendance module',     '/attendance',             'pi pi-calendar-check', 'MODULE',NULL, 5,TRUE,TRUE,FALSE,'system','system',0),
(6,  'ACADEMICS',           'Academics',      'Academics module',      '/academics',              'pi pi-book',           'MODULE',NULL, 6,TRUE,TRUE,FALSE,'system','system',0),
(7,  'ADMISSION',           'Admission',      'Admission module',      '/admission',              'pi pi-inbox',          'MODULE',NULL, 7,TRUE,TRUE,FALSE,'system','system',0),
(9,  'EXAMS',               'Exams',          'Exam management',       '/exam-management',        'pi pi-file-check',     'MODULE',NULL, 9,TRUE,TRUE,FALSE,'system','system',0),
(10, 'COMMUNICATION',       'Communication',  'Notices and messages',  '/communication',          'pi pi-send',           'MODULE',NULL,10,TRUE,TRUE,FALSE,'system','system',0),
(11, 'ENROLLMENT',          'Enrollment',     'Student enrollment',    '/enrollment-management',  'pi pi-user-plus',      'MODULE',NULL,11,TRUE,TRUE,FALSE,'system','system',0),
(20, 'ADMISSION_OVERVIEW',  'Adm. Overview',  'Overview',              '/admission/overview',     'pi pi-chart-bar',      'PAGE',  7, 1,TRUE,TRUE,FALSE,'system','system',0),
(21, 'ADMISSION_INQUIRIES', 'Inquiries',      'Inquiries',             '/admission/inquiries',    'pi pi-question-circle','PAGE',  7, 2,TRUE,TRUE,FALSE,'system','system',0),
(22, 'ADMISSION_APPLY',     'Applications',   'Applications',          '/admission/applications', 'pi pi-file-edit',      'PAGE',  7, 3,TRUE,TRUE,FALSE,'system','system',0),
(30, 'STUDENTS_DIRECTORY',  'Directory',      'All students',          '/students/directory',     'pi pi-list',           'PAGE',  3, 1,TRUE,TRUE,FALSE,'system','system',0),
(40, 'STAFF_DIRECTORY',     'Staff List',     'All staff',             '/staff/directory',        'pi pi-list',           'PAGE',  4, 1,TRUE,TRUE,FALSE,'system','system',0),
(41, 'STAFF_RESPONSIBILITIES','Responsibilities','Responsibilities',   '/staff/responsibilities', 'pi pi-sitemap',        'PAGE',  4, 2,TRUE,TRUE,FALSE,'system','system',0),
(50, 'ATTENDANCE_STUDENTS', 'Student Att.',   'Student attendance',    '/attendance/students',    'pi pi-check-square',   'PAGE',  5, 1,TRUE,TRUE,FALSE,'system','system',0),
(51, 'ATTENDANCE_STAFF',    'Staff Att.',     'Staff attendance',      '/attendance/staff',       'pi pi-check-circle',   'PAGE',  5, 2,TRUE,TRUE,FALSE,'system','system',0),
(60, 'ACADEMICS_SETUP',     'Setup',          'Academic setup',        '/academics/setup',        'pi pi-cog',            'PAGE',  6, 1,TRUE,TRUE,FALSE,'system','system',0),
(61, 'ACADEMICS_TIMETABLE', 'Timetable',      'Timetable',             '/academics/timetable',    'pi pi-calendar',       'PAGE',  6, 2,TRUE,TRUE,FALSE,'system','system',0),
(70, 'ACCESS_OVERVIEW',     'Overview',       'Access overview',       '/access-management/overview','pi pi-home',        'PAGE',  2, 1,TRUE,TRUE,FALSE,'system','system',0),
(71, 'ACCESS_ROLES',        'Roles',          'Roles',                 '/access-management/roles','pi pi-tag',            'PAGE',  2, 2,TRUE,TRUE,FALSE,'system','system',0),
(73, 'ACCESS_USERS',        'Users',          'Users',                 '/access-management/users','pi pi-user',           'PAGE',  2, 4,TRUE,TRUE,FALSE,'system','system',0),
(100,'COMM_NOTICES',        'Notices',        'School notices',        '/communication/notices',  'pi pi-bell',           'PAGE', 10, 1,TRUE,TRUE,FALSE,'system','system',0);

-- 2. ORG DATA
INSERT IGNORE INTO customers (id, customer_code, legal_name, display_name, customer_type, status, email, mobile_number, website, address_line_1, city, state, country, postal_code, onboarding_completed, active, created_by, updated_by, version)
VALUES (1,'CUS000001','Javier Education Group','Javier Education Group','EDUCATION_GROUP','ACTIVE','founder@javier.edu.in','9777001100','https://javier.edu.in','Plot No. 12, Patia','Bhubaneswar','Odisha','India','751024',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO organizations (id, organization_code, customer_id, organization_name, short_name, institution_type, board_name, email, mobile_number, website, address_line_1, city, state, country, postal_code, time_zone, currency, language, logo_url, status, active, onboarding_completed, created_by, updated_by, version)
VALUES (2,'ORG000002',1,'Javier School Cuttack','JSC','SCHOOL','CBSE','principal@jsc.edu.in','9777222200','https://jsc.edu.in','Plot 7, Sector 5','Cuttack','Odisha','India','753014','Asia/Kolkata','INR','en-IN','https://cdn.thinkerscave.local/org/jsc.png','ACTIVE',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO tenant_registry (id, tenant_identifier, organization_id, schema_name, database_version, migration_version, provision_status, active, created_by, updated_by, version)
VALUES (2,'jsc-cuttack',2,'tenant_jsc_cuttack','1.0','1.0','COMPLETED',TRUE,'system','system',0);

INSERT IGNORE INTO organization_configurations (id, organization_id, default_academic_year, academic_year_start_month, student_code_pattern, employee_code_pattern, admission_number_pattern, receipt_number_pattern, invoice_number_pattern, currency, time_zone, language, date_format, created_by, updated_by, version)
VALUES (2,2,'2026-27',4,'JSC/STU/{YY}/{SEQ}','JSC/EMP/{YY}/{SEQ}','JSC/ADM/{YY}/{SEQ}','JSC/REC/{YY}/{SEQ}','JSC/INV/{YY}/{SEQ}','INR','Asia/Kolkata','en-IN','dd-MM-yyyy','system','system',0);

INSERT IGNORE INTO subscription_plans (id, plan_code, plan_name, monthly_price, yearly_price, student_limit, staff_limit, branch_limit, storage_limit_gb, trial_days, display_order, visible, active, created_by, updated_by, version)
VALUES (1,'STARTER','Starter Plan',1499.00,12999.00,300,50,1,50,14,1,TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO organization_subscriptions (id, organization_id, subscription_plan_id, start_date, end_date, billing_cycle, plan_price, discount_amount, final_amount, auto_renew, status, active, created_by, updated_by, version)
VALUES (2,2,1,'2026-01-01','2026-12-31','YEARLY',9990.00,0.00,9990.00,TRUE,'ACTIVE',TRUE,'system','system',0);

INSERT IGNORE INTO security_policies (id, organization_id, min_password_length, require_uppercase, require_lowercase, require_numbers, require_special_chars, password_expiry_days, password_history_count, max_failed_attempts, lockout_duration_minutes, session_timeout_minutes, max_concurrent_sessions, allow_remember_me, require_two_factor, active, created_by, updated_by, version)
VALUES (1,2,8,TRUE,TRUE,TRUE,FALSE,90,5,5,30,60,3,TRUE,FALSE,TRUE,'system','system',0);

-- 3. USERS
INSERT IGNORE INTO users (id, organization_id, user_code, username, email, mobile_number, password, first_name, last_name, display_name, status, email_verified, mobile_verified, first_time_login, failed_login_attempts, account_locked, created_by, updated_by, version)
VALUES
(1, 2, 'USR000001', 'jsc.owner',    'owner@jsc.edu.in',    '9777222100', 'PLACEHOLDER', 'Priya',    'Sahoo',    'Priya Sahoo',    'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(2, 2, 'USR000002', 'jsc.admin',    'admin@jsc.edu.in',    '9777222101', 'PLACEHOLDER', 'Bibhuti',  'Das',      'Bibhuti Das',    'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(3, 2, 'USR000003', 'jsc.teacher1', 'teacher1@jsc.edu.in', '9777222201', 'PLACEHOLDER', 'Kiran',    'Jena',     'Kiran Jena',     'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(4, 2, 'USR000004', 'jsc.teacher2', 'teacher2@jsc.edu.in', '9777222202', 'PLACEHOLDER', 'Asmita',   'Panda',    'Asmita Panda',   'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(5, 2, 'USR000005', 'jsc.receptionist','reception@jsc.edu.in','9777222301','PLACEHOLDER','Seema',   'Rath',     'Seema Rath',     'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(6, 2, 'USR000006', 'jsc.student1', 'student1@jsc.edu.in', '9777222401', 'PLACEHOLDER', 'Arjun',    'Pani',     'Arjun Pani',     'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(7, 2, 'USR000007', 'jsc.parent1',  'parent1@jsc.edu.in',  '9777222501', 'PLACEHOLDER', 'Deepak',   'Pani',     'Deepak Pani',    'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0);

INSERT IGNORE INTO user_roles (id, user_id, role_id, primary_role, active, created_by, updated_by, version)
VALUES
(1,1,1,TRUE,TRUE,'system','system',0),
(2,2,2,TRUE,TRUE,'system','system',0),
(3,3,7,TRUE,TRUE,'system','system',0),
(4,4,7,TRUE,TRUE,'system','system',0),
(5,5,9,TRUE,TRUE,'system','system',0),
(6,6,4,TRUE,TRUE,'system','system',0),
(7,7,5,TRUE,TRUE,'system','system',0);

-- 4. ROLE PERMISSIONS
INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
-- OWNER
(10,2,1,1,TRUE,TRUE,TRUE,'system','system',0),(11,2,1,2,TRUE,TRUE,TRUE,'system','system',0),(12,2,1,3,TRUE,TRUE,TRUE,'system','system',0),
(13,2,1,4,TRUE,TRUE,TRUE,'system','system',0),(14,2,1,5,TRUE,TRUE,TRUE,'system','system',0),(15,2,1,6,TRUE,TRUE,TRUE,'system','system',0),
(16,2,1,7,TRUE,TRUE,TRUE,'system','system',0),(17,2,1,9,TRUE,TRUE,TRUE,'system','system',0),(18,2,1,10,TRUE,TRUE,TRUE,'system','system',0),
(19,2,1,11,TRUE,TRUE,TRUE,'system','system',0),(20,2,1,30,TRUE,TRUE,TRUE,'system','system',0),(21,2,1,40,TRUE,TRUE,TRUE,'system','system',0),
(22,2,1,50,TRUE,TRUE,TRUE,'system','system',0),(23,2,1,60,TRUE,TRUE,TRUE,'system','system',0),(24,2,1,70,TRUE,TRUE,TRUE,'system','system',0),
(25,2,1,71,TRUE,TRUE,TRUE,'system','system',0),(26,2,1,73,TRUE,TRUE,TRUE,'system','system',0),(27,2,1,100,TRUE,TRUE,TRUE,'system','system',0),
-- ADMIN
(30,2,2,1,TRUE,TRUE,FALSE,'system','system',0),(31,2,2,3,TRUE,TRUE,FALSE,'system','system',0),(32,2,2,4,TRUE,TRUE,FALSE,'system','system',0),
(33,2,2,5,TRUE,TRUE,FALSE,'system','system',0),(34,2,2,6,TRUE,TRUE,FALSE,'system','system',0),(35,2,2,7,TRUE,TRUE,FALSE,'system','system',0),
(36,2,2,9,TRUE,TRUE,FALSE,'system','system',0),(37,2,2,10,TRUE,TRUE,FALSE,'system','system',0),(38,2,2,11,TRUE,TRUE,FALSE,'system','system',0),
(39,2,2,30,TRUE,TRUE,FALSE,'system','system',0),(40,2,2,40,TRUE,TRUE,FALSE,'system','system',0),(41,2,2,50,TRUE,TRUE,FALSE,'system','system',0),
(42,2,2,60,TRUE,TRUE,FALSE,'system','system',0),(43,2,2,70,TRUE,TRUE,FALSE,'system','system',0),(44,2,2,73,TRUE,TRUE,FALSE,'system','system',0),(45,2,2,100,TRUE,TRUE,FALSE,'system','system',0),
-- RECEPTIONIST
(50,2,9,1,TRUE,FALSE,FALSE,'system','system',0),(51,2,9,3,TRUE,FALSE,FALSE,'system','system',0),(52,2,9,7,TRUE,TRUE,FALSE,'system','system',0),
(53,2,9,20,TRUE,TRUE,FALSE,'system','system',0),(54,2,9,21,TRUE,TRUE,FALSE,'system','system',0),(55,2,9,22,TRUE,TRUE,FALSE,'system','system',0),(56,2,9,100,TRUE,FALSE,FALSE,'system','system',0),
-- TEACHER
(60,2,7,1,TRUE,FALSE,FALSE,'system','system',0),(61,2,7,3,TRUE,FALSE,FALSE,'system','system',0),(62,2,7,5,TRUE,TRUE,FALSE,'system','system',0),
(63,2,7,6,TRUE,TRUE,FALSE,'system','system',0),(64,2,7,50,TRUE,TRUE,FALSE,'system','system',0),(65,2,7,100,TRUE,FALSE,FALSE,'system','system',0),
-- STUDENT
(70,2,4,1,TRUE,FALSE,FALSE,'system','system',0),(71,2,4,5,TRUE,FALSE,FALSE,'system','system',0),(72,2,4,6,TRUE,FALSE,FALSE,'system','system',0),(73,2,4,100,TRUE,FALSE,FALSE,'system','system',0),
-- PARENT
(80,2,5,1,TRUE,FALSE,FALSE,'system','system',0),(81,2,5,10,TRUE,FALSE,FALSE,'system','system',0),(82,2,5,100,TRUE,FALSE,FALSE,'system','system',0);

-- 5. ORGANIZATION MODULES
INSERT IGNORE INTO organization_modules (id, organization_id, menu_id, enabled, created_by, updated_by, version)
VALUES (1,2,1,TRUE,'system','system',0),(2,2,2,TRUE,'system','system',0),(3,2,3,TRUE,'system','system',0),(4,2,4,TRUE,'system','system',0),(5,2,5,TRUE,'system','system',0),(6,2,6,TRUE,'system','system',0),(7,2,7,TRUE,'system','system',0),(8,2,9,TRUE,'system','system',0),(9,2,10,TRUE,'system','system',0),(10,2,11,TRUE,'system','system',0);

-- 6. ACADEMIC STRUCTURE
INSERT IGNORE INTO academic_year (academic_year_id, year_code, year_name, start_date, end_date, current_year, active, created_by, updated_by, version)
VALUES (1,'AY2026','Academic Year 2026-27','2026-04-01','2027-03-31',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO academic_class (class_id, academic_year_id, class_code, class_name, academic_stage, display_order, active, created_by, updated_by, version)
VALUES
(1,1,'I',  'Class I',   'PRIMARY',       1,TRUE,'system','system',0),
(2,1,'II', 'Class II',  'PRIMARY',       2,TRUE,'system','system',0),
(3,1,'III','Class III', 'PRIMARY',       3,TRUE,'system','system',0),
(4,1,'IV', 'Class IV',  'PRIMARY',       4,TRUE,'system','system',0),
(5,1,'V',  'Class V',   'UPPER_PRIMARY', 5,TRUE,'system','system',0),
(6,1,'X',  'Class X',   'SECONDARY',     6,TRUE,'system','system',0);

INSERT IGNORE INTO academic_section (section_id, class_id, section_name, capacity, active, created_by, updated_by, version)
VALUES (1,1,'A',40,TRUE,'system','system',0),(2,1,'B',40,TRUE,'system','system',0),(3,2,'A',40,TRUE,'system','system',0),(4,2,'B',40,TRUE,'system','system',0),(5,3,'A',40,TRUE,'system','system',0),(6,6,'A',35,TRUE,'system','system',0);

INSERT IGNORE INTO subject (subject_id, subject_code, subject_name, subject_type, active, created_by, updated_by, version)
VALUES (1,'ENG','English','CORE',TRUE,'system','system',0),(2,'MTH','Mathematics','CORE',TRUE,'system','system',0),(3,'SCI','Science','CORE',TRUE,'system','system',0),(4,'ORI','Odia','LANGUAGE',TRUE,'system','system',0),(5,'HIN','Hindi','LANGUAGE',TRUE,'system','system',0);

-- 7. STAFF
INSERT IGNORE INTO staff (staff_id, user_id, staff_code, first_name, last_name, gender, date_of_birth, mobile_number, email, staff_type, designation, employment_category, employment_status, joining_date, highest_qualification, experience_years, active, created_by, updated_by, version)
VALUES
(1,1,'JSC-OWN-001','Priya','Sahoo','Female','1980-04-12','9777222100','owner@jsc.edu.in','NON_TEACHING','School Owner','PERMANENT','ACTIVE','2020-04-01','MBA','15',TRUE,'system','system',0),
(2,2,'JSC-ADM-001','Bibhuti','Das','Male','1985-09-07','9777222101','admin@jsc.edu.in','NON_TEACHING','Administrator','PERMANENT','ACTIVE','2021-01-01','B.Com','10',TRUE,'system','system',0),
(3,3,'JSC-TCH-001','Kiran','Jena','Female','1991-05-18','9777222201','teacher1@jsc.edu.in','TEACHING','English Teacher','PERMANENT','ACTIVE','2022-08-01','M.A. English, B.Ed.','7',TRUE,'system','system',0),
(4,4,'JSC-TCH-002','Asmita','Panda','Female','1993-11-24','9777222202','teacher2@jsc.edu.in','TEACHING','Math Teacher','PERMANENT','ACTIVE','2023-04-01','M.Sc. Math, B.Ed.','5',TRUE,'system','system',0);

-- 8. STUDENTS
INSERT IGNORE INTO student (student_id, student_code, admission_number, roll_number, first_name, last_name, gender, date_of_birth, religion, nationality, mother_tongue, mobile_number, email, photo_url, admission_date, status, transport_required, hostel_required, same_address, user_id, created_by, updated_by, version)
VALUES
(1,'JSC-STU-0001','JSC-ADM-26001','1','Arjun', 'Pani',    'Male',  '2019-02-11','Hindu','Indian','Odia',9777222401,'student1@jsc.edu.in','https://cdn.thinkerscave.local/students/arjun.png', '2026-04-05','ACTIVE',FALSE,FALSE,TRUE,6,'system','system',0),
(2,'JSC-STU-0002','JSC-ADM-26002','2','Manya', 'Swain',   'Female','2018-10-02','Hindu','Indian','Odia',9777222402,'manya@jsc.edu.in',   'https://cdn.thinkerscave.local/students/manya.png', '2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(3,'JSC-STU-0003','JSC-ADM-26003','3','Kunal', 'Das',     'Male',  '2017-04-19','Hindu','Indian','Odia',9777222403,'kunal@jsc.edu.in',   'https://cdn.thinkerscave.local/students/kunal.png', '2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(4,'JSC-STU-0004','JSC-ADM-26004','4','Riya',  'Mohanty', 'Female','2017-11-28','Hindu','Indian','Odia',9777222404,'riya@jsc.edu.in',    'https://cdn.thinkerscave.local/students/riya.png',  '2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(5,'JSC-STU-0005','JSC-ADM-26005','5','Nitya', 'Mohanty', 'Female','2018-12-27','Hindu','Indian','Odia',9777222405,'nitya@jsc.edu.in',   'https://cdn.thinkerscave.local/students/nitya.png', '2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0);

INSERT IGNORE INTO student_enrollment (enrollment_id, student_id, academic_year_id, class_id, section_id, roll_number, status, active, created_by, updated_by, version)
VALUES (1,1,1,1,1,'1','ACTIVE',TRUE,'system','system',0),(2,2,1,2,3,'1','ACTIVE',TRUE,'system','system',0),(3,3,1,2,4,'2','ACTIVE',TRUE,'system','system',0),(4,4,1,1,2,'2','ACTIVE',TRUE,'system','system',0),(5,5,1,1,2,'3','ACTIVE',TRUE,'system','system',0);

-- 9. COMMUNICATION
INSERT IGNORE INTO notices (notice_id, organization_id, title, content, notice_type, status, publish_date, expiry_date, created_by_user_id, created_by, updated_by, version)
VALUES
(1,2,'Welcome Back to School','Welcome to the new academic year 2026-27. Classes begin from 2nd April.','GENERAL','PUBLISHED','2026-03-28','2026-04-10',2,'system','system',0),
(2,2,'Annual Sports Day','Annual Sports Day will be held on 20th July 2026.','EVENT','PUBLISHED','2026-07-05','2026-07-21',2,'system','system',0);

INSERT IGNORE INTO notice_audience (id, notice_id, audience_type, created_by, updated_by, version)
VALUES (1,1,'ALL','system','system',0),(2,2,'ALL','system','system',0);
