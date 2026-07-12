-- ============================================================
-- TENANT: Kalinga College Cuttack (kcc-cuttack)
-- Schema: tenant_kcc_cuttack   Organization ID: 4
-- College — enrollment-focused, no Nursery/Primary classes
-- ============================================================

-- 1. REFERENCE DATA
INSERT IGNORE INTO roles (id, role_code, role_name, description, role_type, dashboard_code, system_role, active, display_order, created_by, updated_by, version)
VALUES
(1, 'ROLE_OWNER',   'Organization Owner','Full access',     'ORGANIZATION_OWNER','ADMIN',  TRUE,TRUE,1,'system','system',0),
(2, 'ROLE_ADMIN',   'Organization Admin','Admin',           'ORGANIZATION_ADMIN','ADMIN',  TRUE,TRUE,2,'system','system',0),
(3, 'ROLE_STAFF',   'Staff',             'Staff',           'STAFF',             'STAFF',  TRUE,TRUE,3,'system','system',0),
(4, 'ROLE_STUDENT', 'Student',           'Student',         'STUDENT',           'STUDENT',TRUE,TRUE,4,'system','system',0),
(7, 'ROLE_TEACHER', 'Teacher',           'Lecturer',        'STAFF',             'STAFF',  TRUE,TRUE,6,'system','system',0),
(8, 'ROLE_PRINCIPAL','Principal',        'Principal/HOD',   'ORGANIZATION_ADMIN','ADMIN',  TRUE,TRUE,7,'system','system',0);

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(1,  'DASHBOARD',  'Dashboard',   'Main dashboard',    '/dashboard',               'pi pi-home',           'MODULE',NULL,1, TRUE,TRUE,TRUE, 'system','system',0),
(2,  'ACCESS',     'Access',      'Access management', '/access-management',       'pi pi-shield',         'MODULE',NULL,2, TRUE,TRUE,FALSE,'system','system',0),
(3,  'STUDENTS',   'Students',    'Student module',    '/students',                'pi pi-users',          'MODULE',NULL,3, TRUE,TRUE,FALSE,'system','system',0),
(4,  'STAFF',      'Staff',       'Staff module',      '/staff',                   'pi pi-id-card',        'MODULE',NULL,4, TRUE,TRUE,FALSE,'system','system',0),
(5,  'ATTENDANCE', 'Attendance',  'Attendance',        '/attendance',              'pi pi-calendar-check', 'MODULE',NULL,5, TRUE,TRUE,FALSE,'system','system',0),
(6,  'ACADEMICS',  'Academics',   'Academics',         '/academics',               'pi pi-book',           'MODULE',NULL,6, TRUE,TRUE,FALSE,'system','system',0),
(9,  'EXAMS',      'Exams',       'Exam management',   '/exam-management',         'pi pi-file-check',     'MODULE',NULL,9, TRUE,TRUE,FALSE,'system','system',0),
(10, 'COMMUNICATION','Communication','Notices',        '/communication',           'pi pi-send',           'MODULE',NULL,10,TRUE,TRUE,FALSE,'system','system',0),
(11, 'ENROLLMENT', 'Enrollment',  'Student enrollment','/enrollment-management',   'pi pi-user-plus',      'MODULE',NULL,11,TRUE,TRUE,FALSE,'system','system',0),
(30, 'STUDENTS_DIRECTORY','Directory','All students',  '/students/directory',      'pi pi-list',           'PAGE',  3, 1,TRUE,TRUE,FALSE,'system','system',0),
(40, 'STAFF_DIRECTORY','Staff List','All staff',       '/staff/directory',         'pi pi-list',           'PAGE',  4, 1,TRUE,TRUE,FALSE,'system','system',0),
(50, 'ATTENDANCE_STUDENTS','Att.','Attendance',        '/attendance/students',     'pi pi-check-square',   'PAGE',  5, 1,TRUE,TRUE,FALSE,'system','system',0),
(60, 'ACADEMICS_SETUP','Setup','Academic setup',       '/academics/setup',         'pi pi-cog',            'PAGE',  6, 1,TRUE,TRUE,FALSE,'system','system',0),
(70, 'ACCESS_OVERVIEW','Overview','Overview',          '/access-management/overview','pi pi-home',         'PAGE',  2, 1,TRUE,TRUE,FALSE,'system','system',0),
(73, 'ACCESS_USERS','Users','Users',                   '/access-management/users', 'pi pi-user',           'PAGE',  2, 4,TRUE,TRUE,FALSE,'system','system',0),
(100,'COMM_NOTICES','Notices','Notices',               '/communication/notices',   'pi pi-bell',           'PAGE', 10, 1,TRUE,TRUE,FALSE,'system','system',0);

-- 2. ORG DATA
INSERT IGNORE INTO customers (id, customer_code, legal_name, display_name, customer_type, status, email, mobile_number, website, address_line_1, city, state, country, postal_code, onboarding_completed, active, created_by, updated_by, version)
VALUES (3,'CUS000003','Kalinga Learning Foundation','Kalinga Learning Foundation','EDUCATION_GROUP','ACTIVE','contact@kalinga.edu.in','9692003300','https://kalinga.edu.in','Plot 7, Sector 5','Cuttack','Odisha','India','753014',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO organizations (id, organization_code, customer_id, organization_name, short_name, institution_type, board_name, email, mobile_number, website, address_line_1, city, state, country, postal_code, time_zone, currency, language, logo_url, status, active, onboarding_completed, created_by, updated_by, version)
VALUES (4,'ORG000004',3,'Kalinga College Cuttack','KCC','COLLEGE','UTKAL UNIVERSITY','principal@kcc.edu.in','9777444400','https://kcc.edu.in','College Road, Badambadi','Cuttack','Odisha','India','753012','Asia/Kolkata','INR','en-IN','https://cdn.thinkerscave.local/org/kcc.png','ACTIVE',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO tenant_registry (id, tenant_identifier, organization_id, schema_name, database_version, migration_version, provision_status, active, created_by, updated_by, version)
VALUES (4,'kcc-cuttack',4,'tenant_kcc_cuttack','1.0','1.0','COMPLETED',TRUE,'system','system',0);

INSERT IGNORE INTO organization_configurations (id, organization_id, default_academic_year, academic_year_start_month, student_code_pattern, employee_code_pattern, admission_number_pattern, receipt_number_pattern, invoice_number_pattern, currency, time_zone, language, date_format, created_by, updated_by, version)
VALUES (4,4,'2026-27',4,'KCC/STU/{YY}/{SEQ}','KCC/EMP/{YY}/{SEQ}','KCC/ADM/{YY}/{SEQ}','KCC/REC/{YY}/{SEQ}','KCC/INV/{YY}/{SEQ}','INR','Asia/Kolkata','en-IN','dd-MM-yyyy','system','system',0);

INSERT IGNORE INTO subscription_plans (id, plan_code, plan_name, monthly_price, yearly_price, student_limit, staff_limit, branch_limit, storage_limit_gb, trial_days, display_order, visible, active, created_by, updated_by, version)
VALUES (3,'ENTERPRISE','Enterprise Plan',9999.00,89999.00,5000,400,20,1000,30,3,TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO organization_subscriptions (id, organization_id, subscription_plan_id, start_date, end_date, billing_cycle, plan_price, discount_amount, final_amount, auto_renew, status, active, created_by, updated_by, version)
VALUES (4,4,3,'2026-01-01','2026-12-31','YEARLY',99990.00,10000.00,89990.00,TRUE,'ACTIVE',TRUE,'system','system',0);

INSERT IGNORE INTO security_policies (id, organization_id, min_password_length, require_uppercase, require_lowercase, require_numbers, require_special_chars, password_expiry_days, password_history_count, max_failed_attempts, lockout_duration_minutes, session_timeout_minutes, max_concurrent_sessions, allow_remember_me, require_two_factor, active, created_by, updated_by, version)
VALUES (1,4,8,TRUE,TRUE,TRUE,FALSE,90,5,5,30,60,5,TRUE,FALSE,TRUE,'system','system',0);

-- 3. USERS
INSERT IGNORE INTO users (id, organization_id, user_code, username, email, mobile_number, password, first_name, last_name, display_name, status, email_verified, mobile_verified, first_time_login, failed_login_attempts, account_locked, created_by, updated_by, version)
VALUES
(1,4,'USR000001','kalinga.owner',    'owner@kcc.edu.in',     '9777444400','PLACEHOLDER','Dr. Namita','Sahoo',    'Dr. Namita Sahoo',  'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(2,4,'USR000002','kalinga.admin',    'admin@kcc.edu.in',     '9777444401','PLACEHOLDER','Sourav',   'Swain',    'Sourav Swain',      'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(3,4,'USR000003','kalinga.principal','hod@kcc.edu.in',       '9777444402','PLACEHOLDER','Manoj',    'Barik',    'Prof. Manoj Barik', 'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(4,4,'USR000004','kalinga.teacher1', 'teacher1@kcc.edu.in',  '9777444501','PLACEHOLDER','Madhuri',  'Tripathy', 'Madhuri Tripathy',  'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(5,4,'USR000005','kalinga.teacher2', 'teacher2@kcc.edu.in',  '9777444502','PLACEHOLDER','Suresh',   'Nanda',    'Suresh Nanda',      'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(6,4,'USR000006','kalinga.student1', 'student1@kcc.edu.in',  '9777444601','PLACEHOLDER','Ritika',   'Panda',    'Ritika Panda',      'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(7,4,'USR000007','kalinga.student2', 'student2@kcc.edu.in',  '9777444602','PLACEHOLDER','Debasish', 'Barik',    'Debasish Barik',    'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0);

INSERT IGNORE INTO user_roles (id, user_id, role_id, primary_role, active, created_by, updated_by, version)
VALUES (1,1,1,TRUE,TRUE,'system','system',0),(2,2,2,TRUE,TRUE,'system','system',0),(3,3,8,TRUE,TRUE,'system','system',0),(4,4,7,TRUE,TRUE,'system','system',0),(5,5,7,TRUE,TRUE,'system','system',0),(6,6,4,TRUE,TRUE,'system','system',0),(7,7,4,TRUE,TRUE,'system','system',0);

-- 4. ROLE PERMISSIONS
INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
-- OWNER
(10,4,1,1,TRUE,TRUE,TRUE,'system','system',0),(11,4,1,2,TRUE,TRUE,TRUE,'system','system',0),(12,4,1,3,TRUE,TRUE,TRUE,'system','system',0),(13,4,1,4,TRUE,TRUE,TRUE,'system','system',0),
(14,4,1,5,TRUE,TRUE,TRUE,'system','system',0),(15,4,1,6,TRUE,TRUE,TRUE,'system','system',0),(16,4,1,9,TRUE,TRUE,TRUE,'system','system',0),(17,4,1,10,TRUE,TRUE,TRUE,'system','system',0),
(18,4,1,11,TRUE,TRUE,TRUE,'system','system',0),(19,4,1,30,TRUE,TRUE,TRUE,'system','system',0),(20,4,1,40,TRUE,TRUE,TRUE,'system','system',0),(21,4,1,50,TRUE,TRUE,TRUE,'system','system',0),
(22,4,1,60,TRUE,TRUE,TRUE,'system','system',0),(23,4,1,70,TRUE,TRUE,TRUE,'system','system',0),(24,4,1,73,TRUE,TRUE,TRUE,'system','system',0),(25,4,1,100,TRUE,TRUE,TRUE,'system','system',0),
-- ADMIN
(30,4,2,1,TRUE,TRUE,FALSE,'system','system',0),(31,4,2,3,TRUE,TRUE,FALSE,'system','system',0),(32,4,2,4,TRUE,TRUE,FALSE,'system','system',0),(33,4,2,5,TRUE,TRUE,FALSE,'system','system',0),
(34,4,2,6,TRUE,TRUE,FALSE,'system','system',0),(35,4,2,9,TRUE,TRUE,FALSE,'system','system',0),(36,4,2,10,TRUE,TRUE,FALSE,'system','system',0),(37,4,2,11,TRUE,TRUE,FALSE,'system','system',0),
(38,4,2,30,TRUE,TRUE,FALSE,'system','system',0),(39,4,2,40,TRUE,TRUE,FALSE,'system','system',0),(40,4,2,50,TRUE,TRUE,FALSE,'system','system',0),(41,4,2,100,TRUE,TRUE,FALSE,'system','system',0),
-- PRINCIPAL (HOD)
(50,4,8,1,TRUE,FALSE,FALSE,'system','system',0),(51,4,8,3,TRUE,TRUE,FALSE,'system','system',0),(52,4,8,4,TRUE,TRUE,FALSE,'system','system',0),(53,4,8,5,TRUE,TRUE,TRUE,'system','system',0),
(54,4,8,6,TRUE,TRUE,TRUE,'system','system',0),(55,4,8,9,TRUE,TRUE,TRUE,'system','system',0),(56,4,8,11,TRUE,TRUE,FALSE,'system','system',0),(57,4,8,100,TRUE,TRUE,FALSE,'system','system',0),
-- TEACHER (Lecturer)
(60,4,7,1,TRUE,FALSE,FALSE,'system','system',0),(61,4,7,3,TRUE,FALSE,FALSE,'system','system',0),(62,4,7,5,TRUE,TRUE,FALSE,'system','system',0),
(63,4,7,6,TRUE,TRUE,FALSE,'system','system',0),(64,4,7,9,TRUE,TRUE,FALSE,'system','system',0),(65,4,7,100,TRUE,FALSE,FALSE,'system','system',0),
-- STUDENT
(70,4,4,1,TRUE,FALSE,FALSE,'system','system',0),(71,4,4,5,TRUE,FALSE,FALSE,'system','system',0),(72,4,4,6,TRUE,FALSE,FALSE,'system','system',0),(73,4,4,9,TRUE,FALSE,FALSE,'system','system',0),(74,4,4,100,TRUE,FALSE,FALSE,'system','system',0);

-- 5. ORGANIZATION MODULES
INSERT IGNORE INTO organization_modules (id, organization_id, menu_id, enabled, created_by, updated_by, version)
VALUES (1,4,1,TRUE,'system','system',0),(2,4,2,TRUE,'system','system',0),(3,4,3,TRUE,'system','system',0),(4,4,4,TRUE,'system','system',0),(5,4,5,TRUE,'system','system',0),(6,4,6,TRUE,'system','system',0),(7,4,9,TRUE,'system','system',0),(8,4,10,TRUE,'system','system',0),(9,4,11,TRUE,'system','system',0);

-- 6. ACADEMIC STRUCTURE (College — degree programs)
INSERT IGNORE INTO academic_year (academic_year_id, year_code, year_name, start_date, end_date, current_year, active, created_by, updated_by, version)
VALUES (1,'AY2026','Academic Year 2026-27','2026-04-01','2027-03-31',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO academic_class (class_id, academic_year_id, class_code, class_name, academic_stage, display_order, active, created_by, updated_by, version)
VALUES
(1,1,'B.COM-I',  'B.Com First Year',   'GRADUATE',1,TRUE,'system','system',0),
(2,1,'B.COM-II', 'B.Com Second Year',  'GRADUATE',2,TRUE,'system','system',0),
(3,1,'B.COM-III','B.Com Third Year',   'GRADUATE',3,TRUE,'system','system',0),
(4,1,'MBA-I',    'MBA First Year',     'POST_GRADUATE',4,TRUE,'system','system',0),
(5,1,'MBA-II',   'MBA Second Year',    'POST_GRADUATE',5,TRUE,'system','system',0);

INSERT IGNORE INTO academic_section (section_id, class_id, section_name, capacity, active, created_by, updated_by, version)
VALUES (1,1,'A',60,TRUE,'system','system',0),(2,1,'B',60,TRUE,'system','system',0),(3,2,'A',60,TRUE,'system','system',0),(4,3,'A',55,TRUE,'system','system',0),(5,4,'A',40,TRUE,'system','system',0),(6,5,'A',35,TRUE,'system','system',0);

INSERT IGNORE INTO subject (subject_id, subject_code, subject_name, subject_type, active, created_by, updated_by, version)
VALUES
(1,'ACC','Accountancy','CORE',TRUE,'system','system',0),
(2,'BUS','Business Studies','CORE',TRUE,'system','system',0),
(3,'ECO','Economics','CORE',TRUE,'system','system',0),
(4,'MGT','Management','CORE',TRUE,'system','system',0),
(5,'HRM','Human Resource Mgmt','ELECTIVE',TRUE,'system','system',0),
(6,'FIN','Financial Management','ELECTIVE',TRUE,'system','system',0),
(7,'MKT','Marketing Management','ELECTIVE',TRUE,'system','system',0),
(8,'ORI','Odia',           'LANGUAGE',TRUE,'system','system',0);

-- 7. STAFF
INSERT IGNORE INTO staff (staff_id, user_id, staff_code, first_name, last_name, gender, date_of_birth, mobile_number, email, staff_type, designation, employment_category, employment_status, joining_date, highest_qualification, experience_years, active, created_by, updated_by, version)
VALUES
(1,1,'KCC-OWN-001','Dr. Namita','Sahoo',   'Female','1970-04-05','9777444400','owner@kcc.edu.in',    'NON_TEACHING','College Principal','PERMANENT','ACTIVE','2016-07-01','Ph.D. Management','22',TRUE,'system','system',0),
(2,2,'KCC-ADM-001','Sourav',   'Swain',    'Male',  '1982-11-14','9777444401','admin@kcc.edu.in',    'NON_TEACHING','Administrator',   'PERMANENT','ACTIVE','2018-01-15','B.Com, MBA','14',TRUE,'system','system',0),
(3,3,'KCC-HOD-001','Manoj',    'Barik',    'Male',  '1968-07-22','9777444402','hod@kcc.edu.in',      'TEACHING',    'HOD Commerce',    'PERMANENT','ACTIVE','2010-06-01','M.Com, Ph.D','24',TRUE,'system','system',0),
(4,4,'KCC-TCH-001','Madhuri',  'Tripathy', 'Female','1991-01-18','9777444501','teacher1@kcc.edu.in', 'TEACHING',    'Commerce Lecturer','PERMANENT','ACTIVE','2020-08-01','M.Com, NET','10',TRUE,'system','system',0),
(5,5,'KCC-TCH-002','Suresh',   'Nanda',    'Male',  '1986-03-29','9777444502','teacher2@kcc.edu.in', 'TEACHING',    'Economics Lecturer','PERMANENT','ACTIVE','2019-07-01','M.A. Economics, NET','11',TRUE,'system','system',0);

-- 8. STUDENTS
INSERT IGNORE INTO student (student_id, student_code, admission_number, roll_number, first_name, last_name, gender, date_of_birth, religion, nationality, mother_tongue, mobile_number, email, photo_url, admission_date, status, transport_required, hostel_required, same_address, user_id, created_by, updated_by, version)
VALUES
(1,'KCC-STU-0001','KCC-ADM-26001','1','Ritika',   'Panda',     'Female','2006-05-22','Hindu','Indian','Odia',9777444601,'student1@kcc.edu.in','https://cdn.thinkerscave.local/students/ritika.png',   '2026-07-01','ACTIVE',FALSE,TRUE, TRUE,6,'system','system',0),
(2,'KCC-STU-0002','KCC-ADM-26002','2','Debasish', 'Barik',     'Male',  '2005-12-11','Hindu','Indian','Odia',9777444602,'student2@kcc.edu.in','https://cdn.thinkerscave.local/students/debasish.png', '2026-07-01','ACTIVE',FALSE,TRUE, TRUE,7,'system','system',0),
(3,'KCC-STU-0003','KCC-ADM-26003','3','Sreeja',   'Sahu',      'Female','2006-08-15','Hindu','Indian','Odia',9777444603,'sreeja@kcc.edu.in',  'https://cdn.thinkerscave.local/students/sreeja.png',   '2026-07-01','ACTIVE',FALSE,TRUE, TRUE,NULL,'system','system',0),
(4,'KCC-STU-0004','KCC-ADM-26004','4','Aniket',   'Mahapatra', 'Male',  '2005-10-03','Hindu','Indian','Odia',9777444604,'aniket@kcc.edu.in',  'https://cdn.thinkerscave.local/students/aniket.png',   '2026-07-01','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(5,'KCC-STU-0005','KCC-ADM-26005','5','Pallavi',  'Sahoo',     'Female','2006-02-12','Hindu','Indian','Odia',9777444605,'pallavi@kcc.edu.in', 'https://cdn.thinkerscave.local/students/pallavi.png',  '2026-07-01','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(6,'KCC-STU-0006','KCC-ADM-26006','6','Subrat',   'Mohanty',   'Male',  '2003-09-17','Hindu','Indian','Odia',9777444606,'subrat@kcc.edu.in',  'https://cdn.thinkerscave.local/students/subrat.png',   '2024-07-01','ACTIVE',FALSE,TRUE, TRUE,NULL,'system','system',0),
(7,'KCC-STU-0007','KCC-ADM-26007','7','Ankita',   'Rath',      'Female','2004-06-08','Hindu','Indian','Odia',9777444607,'ankita@kcc.edu.in',  'https://cdn.thinkerscave.local/students/ankita.png',   '2024-07-01','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0);

INSERT IGNORE INTO student_enrollment (enrollment_id, student_id, academic_year_id, class_id, section_id, roll_number, status, active, created_by, updated_by, version)
VALUES
(1,1,1,1,1,'1','ACTIVE',TRUE,'system','system',0),
(2,2,1,1,1,'2','ACTIVE',TRUE,'system','system',0),
(3,3,1,1,2,'1','ACTIVE',TRUE,'system','system',0),
(4,4,1,1,2,'2','ACTIVE',TRUE,'system','system',0),
(5,5,1,1,1,'3','ACTIVE',TRUE,'system','system',0),
(6,6,1,3,4,'1','ACTIVE',TRUE,'system','system',0),
(7,7,1,2,3,'1','ACTIVE',TRUE,'system','system',0);

-- 9. COMMUNICATION
INSERT IGNORE INTO notices (notice_id, organization_id, title, content, notice_type, status, publish_date, expiry_date, created_by_user_id, created_by, updated_by, version)
VALUES
(1,4,'College Orientation Week','Orientation week for new students begins July 1st 2026. All first-year students must attend.','GENERAL','PUBLISHED','2026-06-20','2026-07-10',1,'system','system',0),
(2,4,'University Exam Schedule','Utkal University examination schedule for November 2026 has been released. Check the notice board.','EXAM','PUBLISHED','2026-07-01','2026-11-30',2,'system','system',0),
(3,4,'Hostel Registration Open','Hostel registrations for 2026-27 are open. Students should register before July 15th.','GENERAL','PUBLISHED','2026-06-25','2026-07-15',1,'system','system',0);

INSERT IGNORE INTO notice_audience (id, notice_id, audience_type, created_by, updated_by, version)
VALUES (1,1,'STUDENTS','system','system',0),(2,2,'ALL','system','system',0),(3,3,'STUDENTS','system','system',0);
