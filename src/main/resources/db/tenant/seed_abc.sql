-- ============================================================
-- TENANT: ABC School Puri (abc-puri)
-- Schema: tenant_abc_puri   Organization ID: 3
-- ============================================================

-- 1. REFERENCE DATA
INSERT IGNORE INTO roles (id, role_code, role_name, description, role_type, dashboard_code, system_role, active, display_order, created_by, updated_by, version)
VALUES
(1,'ROLE_OWNER',  'Organization Owner','Full access',       'ORGANIZATION_OWNER','ADMIN',  TRUE,TRUE,1,'system','system',0),
(2,'ROLE_ADMIN',  'Organization Admin','Admin',             'ORGANIZATION_ADMIN','ADMIN',  TRUE,TRUE,2,'system','system',0),
(3,'ROLE_STAFF',  'Staff',             'Staff',             'STAFF',             'STAFF',  TRUE,TRUE,3,'system','system',0),
(4,'ROLE_STUDENT','Student',           'Student',           'STUDENT',           'STUDENT',TRUE,TRUE,4,'system','system',0),
(5,'ROLE_PARENT', 'Parent',            'Parent',            'PARENT',            'PARENT', TRUE,TRUE,5,'system','system',0),
(7,'ROLE_TEACHER','Teacher',           'Teacher',           'STAFF',             'STAFF',  TRUE,TRUE,6,'system','system',0);

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(1, 'DASHBOARD',  'Dashboard','Main dashboard',   '/dashboard',              'pi pi-home',           'MODULE',NULL,1,TRUE,TRUE,TRUE, 'system','system',0),
(2, 'ACCESS',     'Access',   'Access management','/access-management',      'pi pi-shield',         'MODULE',NULL,2,TRUE,TRUE,FALSE,'system','system',0),
(3, 'STUDENTS',   'Students', 'Student module',   '/students',               'pi pi-users',          'MODULE',NULL,3,TRUE,TRUE,FALSE,'system','system',0),
(4, 'STAFF',      'Staff',    'Staff module',     '/staff',                  'pi pi-id-card',        'MODULE',NULL,4,TRUE,TRUE,FALSE,'system','system',0),
(5, 'ATTENDANCE', 'Attendance','Attendance',       '/attendance',             'pi pi-calendar-check', 'MODULE',NULL,5,TRUE,TRUE,FALSE,'system','system',0),
(6, 'ACADEMICS',  'Academics','Academics',         '/academics',              'pi pi-book',           'MODULE',NULL,6,TRUE,TRUE,FALSE,'system','system',0),
(7, 'ADMISSION',  'Admission','Admission',         '/admission',              'pi pi-inbox',          'MODULE',NULL,7,TRUE,TRUE,FALSE,'system','system',0),
(9, 'EXAMS',      'Exams',    'Exam management',  '/exam-management',        'pi pi-file-check',     'MODULE',NULL,9,TRUE,TRUE,FALSE,'system','system',0),
(10,'COMMUNICATION','Communication','Notices',     '/communication',          'pi pi-send',           'MODULE',NULL,10,TRUE,TRUE,FALSE,'system','system',0),
(30,'STUDENTS_DIRECTORY','Directory','All students','/students/directory',   'pi pi-list',           'PAGE',  3,1,TRUE,TRUE,FALSE,'system','system',0),
(40,'STAFF_DIRECTORY','Staff List','All staff',   '/staff/directory',        'pi pi-list',           'PAGE',  4,1,TRUE,TRUE,FALSE,'system','system',0),
(50,'ATTENDANCE_STUDENTS','Student Att.','Att.',   '/attendance/students',   'pi pi-check-square',   'PAGE',  5,1,TRUE,TRUE,FALSE,'system','system',0),
(60,'ACADEMICS_SETUP','Setup','Academic setup',    '/academics/setup',        'pi pi-cog',            'PAGE',  6,1,TRUE,TRUE,FALSE,'system','system',0),
(70,'ACCESS_OVERVIEW','Overview','Overview',       '/access-management/overview','pi pi-home',        'PAGE',  2,1,TRUE,TRUE,FALSE,'system','system',0),
(73,'ACCESS_USERS','Users','Users',                '/access-management/users','pi pi-user',           'PAGE',  2,4,TRUE,TRUE,FALSE,'system','system',0),
(100,'COMM_NOTICES','Notices','School notices',    '/communication/notices',  'pi pi-bell',           'PAGE', 10,1,TRUE,TRUE,FALSE,'system','system',0);

-- 2. ORG DATA
INSERT IGNORE INTO customers (id, customer_code, legal_name, display_name, customer_type, status, email, mobile_number, website, address_line_1, city, state, country, postal_code, onboarding_completed, active, created_by, updated_by, version)
VALUES (2,'CUS000002','ABC School Trust','ABC School Trust','SCHOOL','ACTIVE','trust@abcschool.edu.in','9861002200','https://abcschool.edu.in','Plot 44, Chandrasekharpur','Bhubaneswar','Odisha','India','751016',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO organizations (id, organization_code, customer_id, organization_name, short_name, institution_type, board_name, email, mobile_number, website, address_line_1, city, state, country, postal_code, time_zone, currency, language, logo_url, status, active, onboarding_completed, created_by, updated_by, version)
VALUES (3,'ORG000003',2,'ABC School Puri','ABCP','SCHOOL','ICSE','principal@abcpuri.edu.in','9777333300','https://abcpuri.edu.in','Marine Drive Road','Puri','Odisha','India','752001','Asia/Kolkata','INR','en-IN','https://cdn.thinkerscave.local/org/abc-puri.png','ACTIVE',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO tenant_registry (id, tenant_identifier, organization_id, schema_name, database_version, migration_version, provision_status, active, created_by, updated_by, version)
VALUES (3,'abc-puri',3,'tenant_abc_puri','1.0','1.0','COMPLETED',TRUE,'system','system',0);

INSERT IGNORE INTO organization_configurations (id, organization_id, default_academic_year, academic_year_start_month, student_code_pattern, employee_code_pattern, admission_number_pattern, receipt_number_pattern, invoice_number_pattern, currency, time_zone, language, date_format, created_by, updated_by, version)
VALUES (3,3,'2026-27',4,'ABCP/STU/{YY}/{SEQ}','ABCP/EMP/{YY}/{SEQ}','ABCP/ADM/{YY}/{SEQ}','ABCP/REC/{YY}/{SEQ}','ABCP/INV/{YY}/{SEQ}','INR','Asia/Kolkata','en-IN','dd-MM-yyyy','system','system',0);

INSERT IGNORE INTO subscription_plans (id, plan_code, plan_name, monthly_price, yearly_price, student_limit, staff_limit, branch_limit, storage_limit_gb, trial_days, display_order, visible, active, created_by, updated_by, version)
VALUES (1,'STARTER','Starter Plan',1499.00,12999.00,300,50,1,50,14,1,TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO organization_subscriptions (id, organization_id, subscription_plan_id, start_date, end_date, billing_cycle, plan_price, discount_amount, final_amount, auto_renew, status, active, created_by, updated_by, version)
VALUES (3,3,1,'2026-01-01','2026-12-31','YEARLY',9990.00,0.00,9990.00,TRUE,'ACTIVE',TRUE,'system','system',0);

INSERT IGNORE INTO security_policies (id, organization_id, min_password_length, require_uppercase, require_lowercase, require_numbers, require_special_chars, password_expiry_days, password_history_count, max_failed_attempts, lockout_duration_minutes, session_timeout_minutes, max_concurrent_sessions, allow_remember_me, require_two_factor, active, created_by, updated_by, version)
VALUES (1,3,8,TRUE,TRUE,TRUE,FALSE,90,5,5,30,60,3,TRUE,FALSE,TRUE,'system','system',0);

-- 3. USERS
INSERT IGNORE INTO users (id, organization_id, user_code, username, email, mobile_number, password, first_name, last_name, display_name, status, email_verified, mobile_verified, first_time_login, failed_login_attempts, account_locked, created_by, updated_by, version)
VALUES
(1,3,'USR000001','abc.owner',   'owner@abcpuri.edu.in',   '9777333300','PLACEHOLDER','Dr. Madhumita','Das',    'Dr. Madhumita Das','ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(2,3,'USR000002','abc.admin',   'admin@abcpuri.edu.in',   '9777333301','PLACEHOLDER','Priya',        'Patnaik','Priya Patnaik',    'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(3,3,'USR000003','abc.teacher1','teacher1@abcpuri.edu.in','9777333401','PLACEHOLDER','Suman',        'Nayak',  'Suman Nayak',      'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(4,3,'USR000004','abc.teacher2','teacher2@abcpuri.edu.in','9777333402','PLACEHOLDER','Reena',        'Biswal', 'Reena Biswal',     'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(5,3,'USR000005','abc.student1','student1@abcpuri.edu.in','9777333501','PLACEHOLDER','Ishita',       'Das',    'Ishita Das',       'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0),
(6,3,'USR000006','abc.parent1', 'parent1@abcpuri.edu.in', '9777333601','PLACEHOLDER','Ramesh',       'Das',    'Ramesh Das',       'ACTIVE',TRUE,TRUE,FALSE,0,FALSE,'system','system',0);

INSERT IGNORE INTO user_roles (id, user_id, role_id, primary_role, active, created_by, updated_by, version)
VALUES (1,1,1,TRUE,TRUE,'system','system',0),(2,2,2,TRUE,TRUE,'system','system',0),(3,3,7,TRUE,TRUE,'system','system',0),(4,4,7,TRUE,TRUE,'system','system',0),(5,5,4,TRUE,TRUE,'system','system',0),(6,6,5,TRUE,TRUE,'system','system',0);

-- 4. ROLE PERMISSIONS
INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(10,3,1,1,TRUE,TRUE,TRUE,'system','system',0),(11,3,1,2,TRUE,TRUE,TRUE,'system','system',0),(12,3,1,3,TRUE,TRUE,TRUE,'system','system',0),(13,3,1,4,TRUE,TRUE,TRUE,'system','system',0),
(14,3,1,5,TRUE,TRUE,TRUE,'system','system',0),(15,3,1,6,TRUE,TRUE,TRUE,'system','system',0),(16,3,1,7,TRUE,TRUE,TRUE,'system','system',0),(17,3,1,9,TRUE,TRUE,TRUE,'system','system',0),
(18,3,1,10,TRUE,TRUE,TRUE,'system','system',0),(19,3,1,30,TRUE,TRUE,TRUE,'system','system',0),(20,3,1,40,TRUE,TRUE,TRUE,'system','system',0),(21,3,1,50,TRUE,TRUE,TRUE,'system','system',0),
(22,3,1,60,TRUE,TRUE,TRUE,'system','system',0),(23,3,1,70,TRUE,TRUE,TRUE,'system','system',0),(24,3,1,73,TRUE,TRUE,TRUE,'system','system',0),(25,3,1,100,TRUE,TRUE,TRUE,'system','system',0),
(30,3,2,1,TRUE,TRUE,FALSE,'system','system',0),(31,3,2,3,TRUE,TRUE,FALSE,'system','system',0),(32,3,2,4,TRUE,TRUE,FALSE,'system','system',0),(33,3,2,5,TRUE,TRUE,FALSE,'system','system',0),
(34,3,2,6,TRUE,TRUE,FALSE,'system','system',0),(35,3,2,7,TRUE,TRUE,FALSE,'system','system',0),(36,3,2,9,TRUE,TRUE,FALSE,'system','system',0),(37,3,2,10,TRUE,TRUE,FALSE,'system','system',0),
(38,3,2,30,TRUE,TRUE,FALSE,'system','system',0),(39,3,2,40,TRUE,TRUE,FALSE,'system','system',0),(40,3,2,50,TRUE,TRUE,FALSE,'system','system',0),(41,3,2,100,TRUE,TRUE,FALSE,'system','system',0),
(50,3,7,1,TRUE,FALSE,FALSE,'system','system',0),(51,3,7,3,TRUE,FALSE,FALSE,'system','system',0),(52,3,7,5,TRUE,TRUE,FALSE,'system','system',0),(53,3,7,6,TRUE,TRUE,FALSE,'system','system',0),(54,3,7,100,TRUE,FALSE,FALSE,'system','system',0),
(60,3,4,1,TRUE,FALSE,FALSE,'system','system',0),(61,3,4,5,TRUE,FALSE,FALSE,'system','system',0),(62,3,4,6,TRUE,FALSE,FALSE,'system','system',0),(63,3,4,100,TRUE,FALSE,FALSE,'system','system',0),
(70,3,5,1,TRUE,FALSE,FALSE,'system','system',0),(71,3,5,10,TRUE,FALSE,FALSE,'system','system',0),(72,3,5,100,TRUE,FALSE,FALSE,'system','system',0);

-- 5. ORGANIZATION MODULES
INSERT IGNORE INTO organization_modules (id, organization_id, menu_id, enabled, created_by, updated_by, version)
VALUES (1,3,1,TRUE,'system','system',0),(2,3,2,TRUE,'system','system',0),(3,3,3,TRUE,'system','system',0),(4,3,4,TRUE,'system','system',0),(5,3,5,TRUE,'system','system',0),(6,3,6,TRUE,'system','system',0),(7,3,7,TRUE,'system','system',0),(8,3,9,TRUE,'system','system',0),(9,3,10,TRUE,'system','system',0);

-- 6. ACADEMIC STRUCTURE
INSERT IGNORE INTO academic_year (academic_year_id, year_code, year_name, start_date, end_date, current_year, active, created_by, updated_by, version)
VALUES (1,'AY2026','Academic Year 2026-27','2026-04-01','2027-03-31',TRUE,TRUE,'system','system',0);

INSERT IGNORE INTO academic_class (class_id, academic_year_id, class_code, class_name, academic_stage, display_order, active, created_by, updated_by, version)
VALUES
(1,1,'I',  'Class I',   'PRIMARY',       1,TRUE,'system','system',0),
(2,1,'II', 'Class II',  'PRIMARY',       2,TRUE,'system','system',0),
(3,1,'V',  'Class V',   'UPPER_PRIMARY', 3,TRUE,'system','system',0),
(4,1,'X',  'Class X',   'SECONDARY',     4,TRUE,'system','system',0),
(5,1,'XII','Class XII',  'SENIOR_SECONDARY',5,TRUE,'system','system',0);

INSERT IGNORE INTO academic_section (section_id, class_id, section_name, capacity, active, created_by, updated_by, version)
VALUES (1,1,'A',40,TRUE,'system','system',0),(2,2,'A',40,TRUE,'system','system',0),(3,3,'A',40,TRUE,'system','system',0),(4,4,'A',35,TRUE,'system','system',0),(5,5,'Science',35,TRUE,'system','system',0);

INSERT IGNORE INTO subject (subject_id, subject_code, subject_name, subject_type, active, created_by, updated_by, version)
VALUES (1,'ENG','English','CORE',TRUE,'system','system',0),(2,'MTH','Mathematics','CORE',TRUE,'system','system',0),(3,'SCI','Science','CORE',TRUE,'system','system',0),(4,'ORI','Odia','LANGUAGE',TRUE,'system','system',0),(5,'HIN','Hindi','LANGUAGE',TRUE,'system','system',0);

-- 7. STAFF
INSERT IGNORE INTO staff (staff_id, user_id, staff_code, first_name, last_name, gender, date_of_birth, mobile_number, email, staff_type, designation, employment_category, employment_status, joining_date, highest_qualification, experience_years, active, created_by, updated_by, version)
VALUES
(1,1,'ABCP-OWN-001','Dr. Madhumita','Das','Female','1970-09-01','9777333300','owner@abcpuri.edu.in','NON_TEACHING','Managing Trustee','PERMANENT','ACTIVE','2018-07-01','Ph.D. Education','20',TRUE,'system','system',0),
(2,2,'ABCP-ADM-001','Priya','Patnaik','Female','1988-09-03','9777333301','admin@abcpuri.edu.in','NON_TEACHING','Campus Admin','PERMANENT','ACTIVE','2019-11-01','B.A., DCA','12',TRUE,'system','system',0),
(3,3,'ABCP-TCH-001','Suman','Nayak','Male','1989-11-21','9777333401','teacher1@abcpuri.edu.in','TEACHING','Science Teacher','PERMANENT','ACTIVE','2021-07-10','M.Sc. Physics, B.Ed.','9',TRUE,'system','system',0),
(4,4,'ABCP-TCH-002','Reena','Biswal','Female','1991-03-15','9777333402','teacher2@abcpuri.edu.in','TEACHING','Math Teacher','PERMANENT','ACTIVE','2022-06-01','M.Sc. Math, B.Ed.','7',TRUE,'system','system',0);

-- 8. STUDENTS
INSERT IGNORE INTO student (student_id, student_code, admission_number, roll_number, first_name, last_name, gender, date_of_birth, religion, nationality, mother_tongue, mobile_number, email, photo_url, admission_date, status, transport_required, hostel_required, same_address, user_id, created_by, updated_by, version)
VALUES
(1,'ABCP-STU-0001','ABCP-ADM-26001','1','Ishita',  'Das',     'Female','2019-01-14','Hindu','Indian','Odia',9777333501,'student1@abcpuri.edu.in','https://cdn.thinkerscave.local/students/ishita.png','2026-04-05','ACTIVE',FALSE,FALSE,TRUE,5,'system','system',0),
(2,'ABCP-STU-0002','ABCP-ADM-26002','2','Sai',     'Panda',   'Male',  '2018-06-20','Hindu','Indian','Odia',9777333502,'sai@abcpuri.edu.in',     'https://cdn.thinkerscave.local/students/sai.png',   '2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(3,'ABCP-STU-0003','ABCP-ADM-26003','3','Anushka', 'Jena',    'Female','2017-09-09','Hindu','Indian','Odia',9777333503,'anushka@abcpuri.edu.in', 'https://cdn.thinkerscave.local/students/anushka.png','2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(4,'ABCP-STU-0004','ABCP-ADM-26004','4','Kabir',   'Pradhan', 'Male',  '2017-03-08','Hindu','Indian','Odia',9777333504,'kabir@abcpuri.edu.in',   'https://cdn.thinkerscave.local/students/kabir.png', '2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0),
(5,'ABCP-STU-0005','ABCP-ADM-26005','5','Tushar',  'Nayak',   'Male',  '2018-01-17','Hindu','Indian','Odia',9777333505,'tushar@abcpuri.edu.in',  'https://cdn.thinkerscave.local/students/tushar.png','2026-04-05','ACTIVE',FALSE,FALSE,TRUE,NULL,'system','system',0);

INSERT IGNORE INTO student_enrollment (enrollment_id, student_id, academic_year_id, class_id, section_id, roll_number, status, active, created_by, updated_by, version)
VALUES (1,1,1,1,1,'1','ACTIVE',TRUE,'system','system',0),(2,2,1,1,1,'2','ACTIVE',TRUE,'system','system',0),(3,3,1,2,2,'1','ACTIVE',TRUE,'system','system',0),(4,4,1,2,2,'2','ACTIVE',TRUE,'system','system',0),(5,5,1,1,1,'3','ACTIVE',TRUE,'system','system',0);

-- 9. COMMUNICATION
INSERT IGNORE INTO notices (notice_id, organization_id, title, content, notice_type, status, publish_date, expiry_date, created_by_user_id, created_by, updated_by, version)
VALUES
(1,3,'Admission Open 2026-27','Admissions are now open for session 2026-27. Visit the school office or apply online.','GENERAL','PUBLISHED','2026-02-01','2026-04-30',1,'system','system',0),
(2,3,'ICSE Board Results','Class X ICSE board results have been declared. School congratulates all students.','GENERAL','PUBLISHED','2026-05-10','2026-05-31',2,'system','system',0);

INSERT IGNORE INTO notice_audience (id, notice_id, audience_type, created_by, updated_by, version)
VALUES (1,1,'ALL','system','system',0),(2,2,'ALL','system','system',0);
