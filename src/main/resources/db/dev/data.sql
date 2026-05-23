-- ============================================
-- DEV Demo Data for H2 Database
-- ThinkersCave SaaS - Education Management Platform
-- ============================================
-- Runs AFTER Hibernate creates entity tables (defer-datasource-initialization=true).
-- Password for all users: "Password@123" (BCrypt encoded below)
-- Audit columns (created_date, last_modified_date, etc.) omitted - they are nullable.
-- ============================================

-- ============================================
-- 0. NON-ENTITY TABLES (not managed by Hibernate)
-- ============================================
CREATE TABLE IF NOT EXISTS user_tenant_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255),
    tenant_id VARCHAR(100) NOT NULL DEFAULT 'public',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tenant_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    features TEXT,
    max_users INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 1. ORGANISATION
-- ============================================
INSERT INTO organisation (org_id, org_code, org_name, brand_name, org_url, type, city, state, country, zip_code, email, phone_number, website, establishment_date, subscription_type, registration_number, affiliation, tenant_schema, description) VALUES
(1, 'ORG001', 'ThinkersCave Academy', 'ThinkersCave', '/app', 'SCHOOL', 'Bangalore', 'Karnataka', 'India', '560001', 'admin@thinkerscave.com', '9876543210', 'https://thinkerscave.com', '2020-06-15', 'PREMIUM', 'REG-2020-BLR-001', 'CBSE', 'public', 'Premier educational institution'),
(2, 'ORG002', 'ThinkersCave Engineering College', 'TC Engineering', '/app', 'COLLEGE', 'Mumbai', 'Maharashtra', 'India', '400001', 'engineering@thinkerscave.com', '9876543211', 'https://engineering.thinkerscave.com', '2018-08-01', 'ENTERPRISE', 'REG-2018-MUM-002', 'AICTE', 'public', 'Engineering college');

-- ============================================
-- 2. ROLES
-- ============================================
INSERT INTO role_master (role_id, role_name, role_code, description, is_active, role_type, organization_id) VALUES
(1, 'Super Admin', 'SUPER_ADMIN', 'Full system access', TRUE, 'ADMIN', 1),
(2, 'Admin', 'ADMIN', 'Organization administrator', TRUE, 'ADMIN', 1),
(3, 'Teacher', 'TEACHER', 'Teaching staff', TRUE, 'SCHOOL', 1),
(4, 'Student', 'STUDENT', 'Student access', TRUE, 'SCHOOL', 1),
(5, 'Parent', 'PARENT', 'Parent/Guardian access', TRUE, 'SCHOOL', 1),
(6, 'Counsellor', 'COUNSELLOR', 'Admission counsellor', TRUE, 'SCHOOL', 1),
(7, 'Accountant', 'ACCOUNTANT', 'Finance team', TRUE, 'SCHOOL', 1),
(8, 'IT Support', 'IT_SUPPORT', 'Technical support', TRUE, 'ADMIN', 1);

-- ============================================
-- 3. USERS (Password: Password@123)
-- ============================================
INSERT INTO users (id, user_code, first_name, middle_name, last_name, email, mobile_number, user_name, password, address, city, state, country, zip_code, gender, is_blocked, is_2fa_enabled, attempts, is_first_time_login, is_email_verified, is_mobile_verified, date_of_birth, last_login_date) VALUES
(1,  'USR001', 'Rajesh',  NULL, 'Kumar',    'admin@thinkerscave.com',          9876543210, 'superadmin',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '100 MG Road',       'Bangalore', 'Karnataka', 'India', '560001', 'Male',   FALSE, FALSE, 0, FALSE, TRUE, TRUE, '1985-03-15', '2026-05-20'),
(2,  'USR002', 'Priya',   NULL, 'Sharma',   'priya.sharma@thinkerscave.com',   9876543211, 'admin',        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '45 Park Street',    'Bangalore', 'Karnataka', 'India', '560002', 'Female', FALSE, FALSE, 0, FALSE, TRUE, TRUE, '1988-07-22', '2026-05-19'),
(3,  'USR003', 'Amit',    'K',  'Verma',    'amit.verma@thinkerscave.com',     9876543212, 'teacher1',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '78 Residency Road', 'Bangalore', 'Karnataka', 'India', '560025', 'Male',   FALSE, FALSE, 0, FALSE, TRUE, TRUE, '1990-11-10', '2026-05-18'),
(4,  'USR004', 'Sunita',  NULL, 'Patel',    'sunita.patel@thinkerscave.com',   9876543213, 'teacher2',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '22 Brigade Road',   'Bangalore', 'Karnataka', 'India', '560001', 'Female', FALSE, FALSE, 0, FALSE, TRUE, TRUE, '1992-04-05', '2026-05-17'),
(5,  'USR005', 'Rahul',   NULL, 'Singh',    'rahul.singh@thinkerscave.com',    9876543214, 'student1',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '56 Koramangala',    'Bangalore', 'Karnataka', 'India', '560034', 'Male',   FALSE, FALSE, 0, FALSE, TRUE, TRUE, '2008-09-12', NULL),
(6,  'USR006', 'Ananya',  NULL, 'Reddy',    'ananya.reddy@thinkerscave.com',   9876543215, 'student2',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '34 Whitefield',     'Bangalore', 'Karnataka', 'India', '560066', 'Female', FALSE, FALSE, 0, FALSE, TRUE, TRUE, '2009-01-25', NULL),
(7,  'USR007', 'Vikram',  NULL, 'Malhotra', 'vikram.malhotra@thinkerscave.com',9876543216, 'student3',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '89 Indiranagar',    'Bangalore', 'Karnataka', 'India', '560038', 'Male',   FALSE, FALSE, 0, FALSE, TRUE, TRUE, '2008-06-18', NULL),
(8,  'USR008', 'Meera',   NULL, 'Nair',     'meera.nair@thinkerscave.com',     9876543217, 'parent1',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '56 Koramangala',    'Bangalore', 'Karnataka', 'India', '560034', 'Female', FALSE, FALSE, 0, FALSE, TRUE, TRUE, '1978-12-03', NULL),
(9,  'USR009', 'Deepak',  NULL, 'Joshi',    'deepak.joshi@thinkerscave.com',   9876543218, 'counsellor1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '12 HSR Layout',     'Bangalore', 'Karnataka', 'India', '560102', 'Male',   FALSE, FALSE, 0, FALSE, TRUE, TRUE, '1991-08-14', '2026-05-20'),
(10, 'USR010', 'Kavitha', NULL, 'Menon',    'kavitha.menon@thinkerscave.com',  9876543219, 'accountant1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '67 Jayanagar',      'Bangalore', 'Karnataka', 'India', '560041', 'Female', FALSE, FALSE, 0, FALSE, TRUE, TRUE, '1989-05-30', '2026-05-20');

-- ============================================
-- 4. USER-ROLE MAPPING
-- ============================================
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 3), (5, 4), (6, 4), (7, 4), (8, 5), (9, 6), (10, 7);

-- ============================================
-- 5. ORGANIZATION-USER MAPPING
-- ============================================
INSERT INTO organization_users (id, organization_id, user_id, role_name, is_active, joined_at, updated_at) VALUES
(1, 1, 1, 'SUPER_ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 2, 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 3, 'TEACHER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 4, 'TEACHER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 1, 5, 'STUDENT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 1, 6, 'STUDENT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 1, 7, 'STUDENT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 1, 8, 'PARENT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 1, 9, 'COUNSELLOR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 1, 10, 'ACCOUNTANT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- 6. USER-TENANT MAPPING (non-entity table)
-- ============================================
INSERT INTO user_tenant_mapping (email, username, tenant_id, is_active) VALUES
('admin@thinkerscave.com', 'superadmin', 'public', TRUE),
('priya.sharma@thinkerscave.com', 'admin', 'public', TRUE),
('amit.verma@thinkerscave.com', 'teacher1', 'public', TRUE),
('sunita.patel@thinkerscave.com', 'teacher2', 'public', TRUE),
('rahul.singh@thinkerscave.com', 'student1', 'public', TRUE),
('ananya.reddy@thinkerscave.com', 'student2', 'public', TRUE),
('vikram.malhotra@thinkerscave.com', 'student3', 'public', TRUE),
('meera.nair@thinkerscave.com', 'parent1', 'public', TRUE),
('deepak.joshi@thinkerscave.com', 'counsellor1', 'public', TRUE),
('kavitha.menon@thinkerscave.com', 'accountant1', 'public', TRUE);

-- ============================================
-- 7. TENANT CONFIG (non-entity table)
-- ============================================
INSERT INTO tenant_config (tenant_id, tenant_name, subdomain, is_active, features, max_users) VALUES
('public', 'ThinkersCave Academy', 'demo', TRUE, '{"courseLabel":"Course","studentLabel":"Student","staffLabel":"Staff"}', 500);

-- ============================================
-- 8. PRIVILEGES
-- ============================================
INSERT INTO privilege_master (privilege_id, privilege_name) VALUES
(1, 'VIEW'), (2, 'ADD'), (3, 'EDIT'), (4, 'DELETE'), (5, 'APPROVE');

-- ============================================
-- 9. MENUS
-- ============================================
INSERT INTO menu_master (menu_id, menu_code, name, description, url, icon, menu_order, is_active) VALUES
(1, 'MENU_DASHBOARD', 'Dashboard', 'Main dashboard', '/app', 'pi pi-home', 1, TRUE),
(2, 'MENU_STUDENT', 'Student Management', 'Manage students', '/app/managestudent', 'pi pi-users', 2, TRUE),
(3, 'MENU_STAFF', 'Staff Management', 'Manage staff', '/app/staff', 'pi pi-id-card', 3, TRUE),
(4, 'MENU_ACADEMICS', 'Academics', 'Academic management', '/app/academics', 'pi pi-book', 4, TRUE),
(5, 'MENU_ATTENDANCE', 'Attendance', 'Attendance tracking', '/app/attendance', 'pi pi-calendar', 5, TRUE),
(6, 'MENU_INQUIRY', 'Inquiry & Admissions', 'Inquiry management', '/app/inquiry', 'pi pi-phone', 6, TRUE),
(7, 'MENU_FEES', 'Fee Management', 'Fee and payments', '/app/fees', 'pi pi-wallet', 7, TRUE),
(8, 'MENU_ADMIN', 'Administration', 'System admin', '/app/admin', 'pi pi-cog', 8, TRUE),
(9, 'MENU_REPORTS', 'Reports', 'Reports and analytics', '/app/reports', 'pi pi-chart-bar', 9, TRUE);

-- ============================================
-- 10. SUB-MENUS
-- ============================================
INSERT INTO sub_menu_master (sub_menu_id, sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url, sub_menu_icon, sub_menu_order, is_active, menu_id) VALUES
(1, 'Overview', 'DASHBOARD_OVERVIEW', 'Dashboard overview', '/app', 'pi pi-home', 1, TRUE, 1),
(2, 'Manage Students', 'STUDENT_MANAGE', 'Student CRUD', '/app/managestudent', 'pi pi-users', 1, TRUE, 2),
(3, 'Manage Class', 'STUDENT_CLASS', 'Class management', '/app/manage-class', 'pi pi-th-large', 2, TRUE, 2),
(4, 'Manage Section', 'STUDENT_SECTION', 'Section management', '/app/manage-section', 'pi pi-table', 3, TRUE, 2),
(5, 'Manage Staff', 'STAFF_MANAGE', 'Staff CRUD', '/app/staff', 'pi pi-id-card', 1, TRUE, 3),
(6, 'Departments', 'STAFF_DEPT', 'Department management', '/app/manage-department', 'pi pi-sitemap', 2, TRUE, 3),
(7, 'Branches', 'STAFF_BRANCH', 'Branch management', '/app/manage-branch', 'pi pi-building', 3, TRUE, 3),
(8, 'Salary', 'STAFF_SALARY', 'Payroll management', '/app/salary', 'pi pi-money-bill', 4, TRUE, 3),
(9, 'Leave', 'STAFF_LEAVE', 'Leave management', '/app/leave', 'pi pi-calendar-minus', 5, TRUE, 3),
(10, 'Courses', 'ACAD_COURSES', 'Course management', '/app/academics/courses', 'pi pi-book', 1, TRUE, 4),
(11, 'Subjects', 'ACAD_SUBJECTS', 'Subject management', '/app/academics/subjects', 'pi pi-file', 2, TRUE, 4),
(12, 'Syllabus', 'ACAD_SYLLABUS', 'Syllabus management', '/app/academics/syllabus', 'pi pi-list', 3, TRUE, 4),
(13, 'Academic Structure', 'ACAD_STRUCTURE', 'Academic hierarchy', '/app/academics/structure', 'pi pi-share-alt', 4, TRUE, 4),
(14, 'Curriculum', 'ACAD_CURRICULUM', 'Subject-course mapping', '/app/academics/curriculum', 'pi pi-link', 5, TRUE, 4),
(15, 'Class Attendance', 'ATT_CLASS', 'Student attendance', '/app/attendance/class', 'pi pi-check-square', 1, TRUE, 5),
(16, 'Staff Attendance', 'ATT_STAFF', 'Staff attendance', '/app/attendance/staff', 'pi pi-user-edit', 2, TRUE, 5),
(17, 'Hostel Attendance', 'ATT_HOSTEL', 'Hostel attendance', '/app/attendance/hostel', 'pi pi-building', 3, TRUE, 5),
(18, 'Manage Inquiry', 'INQ_MANAGE', 'Inquiry CRUD', '/app/inquiry/manage', 'pi pi-phone', 1, TRUE, 6),
(19, 'Follow-ups', 'INQ_FOLLOWUP', 'Inquiry follow-ups', '/app/inquiry/followup', 'pi pi-reply', 2, TRUE, 6),
(20, 'Menu Management', 'ADMIN_MENU', 'Menu configuration', '/app/manage-menu', 'pi pi-bars', 1, TRUE, 8),
(21, 'Role Management', 'ADMIN_ROLE', 'Role and permissions', '/app/role/manage', 'pi pi-shield', 2, TRUE, 8),
(22, 'Organization', 'ADMIN_ORG', 'Organization settings', '/app/organization-registration', 'pi pi-globe', 3, TRUE, 8);

-- ============================================
-- 11. SUBMENU-PRIVILEGE MAPPING
-- ============================================
INSERT INTO submenu_privilege_mapping (mapping_id, sub_menu_id, privilege_id) VALUES
(1,1,1),(2,2,1),(3,2,2),(4,2,3),(5,2,4),(6,3,1),(7,3,2),(8,3,3),(9,3,4),
(10,4,1),(11,4,2),(12,4,3),(13,4,4),(14,5,1),(15,5,2),(16,5,3),(17,5,4),
(18,6,1),(19,6,2),(20,6,3),(21,6,4),(22,7,1),(23,7,2),(24,7,3),(25,7,4),
(26,8,1),(27,8,2),(28,8,3),(29,9,1),(30,9,2),(31,9,3),(32,9,5),
(33,10,1),(34,10,2),(35,10,3),(36,10,4),(37,11,1),(38,11,2),(39,11,3),(40,11,4),
(41,12,1),(42,12,2),(43,12,3),(44,12,4),(45,12,5),(46,13,1),(47,13,2),(48,13,3),
(49,14,1),(50,14,2),(51,14,3),(52,15,1),(53,15,2),(54,15,3),
(55,16,1),(56,16,2),(57,16,3),(58,17,1),(59,17,2),(60,17,3),
(61,18,1),(62,18,2),(63,18,3),(64,18,4),(65,19,1),(66,19,2),(67,19,3),
(68,20,1),(69,20,2),(70,20,3),(71,20,4),(72,21,1),(73,21,2),(74,21,3),(75,21,4),
(76,22,1),(77,22,2),(78,22,3);

-- ============================================
-- 12. ROLE-SUBMENU-PRIVILEGE MAPPING
-- ============================================
INSERT INTO role_submenu_privilege_mapping (mapping_id, role_id, sub_menu_id, privilege_id) VALUES
-- SUPER_ADMIN: all
(1,1,1,1),(2,1,2,1),(3,1,2,2),(4,1,2,3),(5,1,2,4),
(6,1,3,1),(7,1,3,2),(8,1,3,3),(9,1,3,4),(10,1,4,1),(11,1,4,2),(12,1,4,3),(13,1,4,4),
(14,1,5,1),(15,1,5,2),(16,1,5,3),(17,1,5,4),(18,1,6,1),(19,1,6,2),(20,1,6,3),(21,1,6,4),
(22,1,7,1),(23,1,7,2),(24,1,7,3),(25,1,7,4),(26,1,8,1),(27,1,8,2),(28,1,8,3),
(29,1,9,1),(30,1,9,2),(31,1,9,3),(32,1,9,5),
(33,1,10,1),(34,1,10,2),(35,1,10,3),(36,1,10,4),(37,1,11,1),(38,1,11,2),(39,1,11,3),(40,1,11,4),
(41,1,12,1),(42,1,12,2),(43,1,12,3),(44,1,12,4),(45,1,12,5),
(46,1,13,1),(47,1,13,2),(48,1,13,3),(49,1,14,1),(50,1,14,2),(51,1,14,3),
(52,1,15,1),(53,1,15,2),(54,1,15,3),(55,1,16,1),(56,1,16,2),(57,1,16,3),
(58,1,17,1),(59,1,17,2),(60,1,17,3),(61,1,18,1),(62,1,18,2),(63,1,18,3),(64,1,18,4),
(65,1,19,1),(66,1,19,2),(67,1,19,3),(68,1,20,1),(69,1,20,2),(70,1,20,3),(71,1,20,4),
(72,1,21,1),(73,1,21,2),(74,1,21,3),(75,1,21,4),(76,1,22,1),(77,1,22,2),(78,1,22,3),
-- ADMIN
(79,2,1,1),(80,2,2,1),(81,2,2,2),(82,2,2,3),(83,2,2,4),
(84,2,3,1),(85,2,3,2),(86,2,3,3),(87,2,5,1),(88,2,5,2),(89,2,5,3),(90,2,5,4),
(91,2,10,1),(92,2,10,2),(93,2,11,1),(94,2,11,2),(95,2,12,1),(96,2,12,2),(97,2,12,5),
(98,2,15,1),(99,2,15,2),(100,2,18,1),(101,2,18,2),(102,2,18,3),
-- TEACHER
(103,3,1,1),(104,3,10,1),(105,3,11,1),(106,3,12,1),(107,3,15,1),(108,3,15,2),(109,3,2,1);

-- ============================================
-- 13. DEPARTMENTS
-- ============================================
INSERT INTO department (department_id, department_name, description, department_code, is_active, organization_id) VALUES
(1, 'Mathematics', 'Mathematics Department', 'DEPT-MATH', TRUE, 1),
(2, 'Science', 'Science and Laboratory Department', 'DEPT-SCI', TRUE, 1),
(3, 'English', 'English Language and Literature', 'DEPT-ENG', TRUE, 1),
(4, 'Computer Science', 'Computer Science and IT', 'DEPT-CS', TRUE, 1),
(5, 'Administration', 'Administrative Department', 'DEPT-ADMIN', TRUE, 1);

-- ============================================
-- 14. BRANCHES
-- ============================================
INSERT INTO branch (branch_id, branch_name, location, branch_code, is_active, organization_id) VALUES
(1, 'Main Campus', 'Bangalore, MG Road', 'BR-MAIN', TRUE, 1),
(2, 'Whitefield Campus', 'Bangalore, Whitefield', 'BR-WF', TRUE, 1);

-- ============================================
-- 15. STAFF
-- ============================================
INSERT INTO staff (staff_id, staff_code, first_name, middle_name, last_name, email, mobile_number, gender, date_of_birth, hire_date, address, city, state, remarks, is_active, organization_id, user_id, branch_id, department_id) VALUES
(1, 'STF001', 'Amit', 'K', 'Verma', 'amit.verma@thinkerscave.com', 9876543212, 'Male', '1990-11-10', '2021-06-01', '78 Residency Road', 'Bangalore', 'Karnataka', 'Senior Math Teacher', TRUE, 1, 3, 1, 1),
(2, 'STF002', 'Sunita', NULL, 'Patel', 'sunita.patel@thinkerscave.com', 9876543213, 'Female', '1992-04-05', '2022-01-15', '22 Brigade Road', 'Bangalore', 'Karnataka', 'Science Teacher', TRUE, 1, 4, 1, 2);

-- ============================================
-- 16. CLASSES
-- ============================================
INSERT INTO "CLASS" (class_id, class_name, organization_id) VALUES
(1, 'Class 8', 1), (2, 'Class 9', 1), (3, 'Class 10', 1),
(4, 'Class 11 - Science', 1), (5, 'Class 11 - Commerce', 1),
(6, 'Class 12 - Science', 1), (7, 'Class 12 - Commerce', 1);

-- ============================================
-- 17. SECTIONS
-- ============================================
INSERT INTO section (section_id, section_name, class_entity_class_id) VALUES
(1, 'Section A', 1), (2, 'Section B', 1),
(3, 'Section A', 2), (4, 'Section B', 2),
(5, 'Section A', 3), (6, 'Section B', 3),
(7, 'Section A', 4), (8, 'Section A', 5),
(9, 'Section A', 6), (10, 'Section A', 7);

-- ============================================
-- 18. GUARDIANS (has own created_at/updated_at, NOT Auditable)
-- ============================================
INSERT INTO guardian (guardian_id, first_name, middle_name, last_name, relation, email, mobile_number, address, created_at, updated_at) VALUES
(1, 'Meera', NULL, 'Nair', 'Mother', 'meera.nair@thinkerscave.com', 9876543217, '56 Koramangala, Bangalore', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Suresh', NULL, 'Reddy', 'Father', 'suresh.reddy@gmail.com', 9876543220, '34 Whitefield, Bangalore', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Arun', NULL, 'Malhotra', 'Father', 'arun.malhotra@gmail.com', 9876543221, '89 Indiranagar, Bangalore', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- 19. ADDRESSES (for students)
-- ============================================
INSERT INTO address (id, country, state, city, zip_code, address_line) VALUES
(1, 'India', 'Karnataka', 'Bangalore', '560034', '56 Koramangala, 1st Block'),
(2, 'India', 'Karnataka', 'Bangalore', '560034', '56 Koramangala, 1st Block'),
(3, 'India', 'Karnataka', 'Bangalore', '560066', '34 Whitefield Main Road'),
(4, 'India', 'Karnataka', 'Bangalore', '560066', '34 Whitefield Main Road'),
(5, 'India', 'Karnataka', 'Bangalore', '560038', '89 Indiranagar, 3rd Stage'),
(6, 'India', 'Karnataka', 'Bangalore', '560038', '89 Indiranagar, 3rd Stage');

-- ============================================
-- 20. STUDENTS
-- ============================================
INSERT INTO student (student_id, first_name, last_name, email, mobile_number, gender, age, is_same_address, date_of_birth, enrollment_date, roll_number, remarks, is_active, organization_id, current_address_id, permanent_address_id, class_id, section_id, user_id, guardian_id) VALUES
(1, 'Rahul', 'Singh', 'rahul.singh@thinkerscave.com', 9876543214, 'Male', 17, TRUE, '2008-09-12', '2024-06-15', 'TC-2024-001', 'Good at math', TRUE, 1, 1, 2, 3, 5, 5, 1),
(2, 'Ananya', 'Reddy', 'ananya.reddy@thinkerscave.com', 9876543215, 'Female', 16, FALSE, '2009-01-25', '2024-06-16', 'TC-2024-002', 'Science enthusiast', TRUE, 1, 3, 4, 2, 3, 6, 2),
(3, 'Vikram', 'Malhotra', 'vikram.malhotra@thinkerscave.com', 9876543216, 'Male', 17, NULL, '2008-06-18', '2024-06-17', 'TC-2024-003', 'Sports captain', TRUE, 1, 5, 6, 3, 5, 7, 3);

-- ============================================
-- 21. COURSES
-- ============================================
INSERT INTO courses (course_id, course_code, course_name, description, duration_months, duration_years, level, degree_type, total_credits, min_credits_required, category, total_semesters, is_active, organization_id) VALUES
(1, 'CBSE-10', 'CBSE Class 10 Program', 'Central Board secondary education', 12, 1, 'SECONDARY', 'CERTIFICATE', 40, 36, 'ACADEMIC', 2, TRUE, 1),
(2, 'CBSE-12-SCI', 'CBSE Class 12 Science', 'Higher secondary science stream', 24, 2, 'HIGHER_SECONDARY', 'CERTIFICATE', 50, 45, 'ACADEMIC', 4, TRUE, 1),
(3, 'CBSE-12-COM', 'CBSE Class 12 Commerce', 'Higher secondary commerce stream', 24, 2, 'HIGHER_SECONDARY', 'CERTIFICATE', 48, 42, 'ACADEMIC', 4, TRUE, 1);

-- ============================================
-- 22. SUBJECTS
-- ============================================
INSERT INTO subjects (subject_id, subject_code, subject_name, description, credits, category, theory_hours, practical_hours, lab_hours, is_active, max_marks, passing_marks, organization_id) VALUES
(1, 'MATH-10', 'Mathematics', 'CBSE Class 10 Mathematics', 5, 'CORE', 4, 0, 0, TRUE, 100, 33, 1),
(2, 'SCI-10', 'Science', 'CBSE Class 10 Science', 5, 'CORE', 3, 1, 1, TRUE, 100, 33, 1),
(3, 'ENG-10', 'English', 'CBSE English Language and Literature', 5, 'CORE', 4, 0, 0, TRUE, 100, 33, 1),
(4, 'SST-10', 'Social Science', 'History, Geography, Political Science', 4, 'CORE', 4, 0, 0, TRUE, 100, 33, 1),
(5, 'HIN-10', 'Hindi', 'Hindi Language and Literature', 4, 'CORE', 3, 0, 0, TRUE, 100, 33, 1),
(6, 'CS-10', 'Computer Science', 'Introduction to Programming', 3, 'ELECTIVE', 2, 0, 2, TRUE, 100, 33, 1),
(7, 'PHY-12', 'Physics', 'CBSE Class 12 Physics', 5, 'CORE', 3, 0, 2, TRUE, 100, 33, 1),
(8, 'CHEM-12', 'Chemistry', 'CBSE Class 12 Chemistry', 5, 'CORE', 3, 0, 2, TRUE, 100, 33, 1);

-- ============================================
-- 23. ACADEMIC YEARS
-- ============================================
INSERT INTO academic_years (academic_year_id, year_code, year_name, start_date, end_date, is_current, is_active, description, organization_id) VALUES
(1, 'AY-2025-26', 'Academic Year 2025-26', '2025-04-01', '2026-03-31', TRUE, TRUE, 'Current academic year', 1),
(2, 'AY-2024-25', 'Academic Year 2024-25', '2024-04-01', '2025-03-31', FALSE, TRUE, 'Previous academic year', 1);

-- ============================================
-- 24. SEMESTERS
-- ============================================
INSERT INTO semesters (semester_id, semester_name, semester_number, start_date, end_date, is_current, is_active, description, academic_year_id, organization_id) VALUES
(1, 'Term 1', 1, '2025-04-01', '2025-09-30', FALSE, TRUE, 'First term', 1, 1),
(2, 'Term 2', 2, '2025-10-01', '2026-03-31', TRUE, TRUE, 'Second term', 1, 1);

-- ============================================
-- 25. COURSE-SUBJECT MAPPING
-- ============================================
INSERT INTO course_subject_mapping (mapping_id, course_id, subject_id, semester, is_mandatory, is_active, display_order, organization_id) VALUES
(1, 1, 1, 1, TRUE, TRUE, 1, 1), (2, 1, 2, 1, TRUE, TRUE, 2, 1),
(3, 1, 3, 1, TRUE, TRUE, 3, 1), (4, 1, 4, 1, TRUE, TRUE, 4, 1),
(5, 1, 5, 1, TRUE, TRUE, 5, 1), (6, 1, 6, 1, FALSE, TRUE, 6, 1),
(7, 2, 7, 1, TRUE, TRUE, 1, 1), (8, 2, 8, 1, TRUE, TRUE, 2, 1);

-- ============================================
-- 26. SYLLABUS
-- ============================================
INSERT INTO syllabus (syllabus_id, syllabus_code, title, description, total_hours, version, status, is_active, subject_id, academic_year_id, organization_id) VALUES
(1, 'SYL-MATH-10-2025', 'Mathematics Class 10', 'Complete CBSE Math syllabus', 180, '1.0', 'PUBLISHED', TRUE, 1, 1, 1),
(2, 'SYL-SCI-10-2025', 'Science Class 10', 'Complete CBSE Science syllabus', 200, '1.0', 'PUBLISHED', TRUE, 2, 1, 1),
(3, 'SYL-ENG-10-2025', 'English Class 10', 'CBSE English syllabus', 160, '1.0', 'APPROVED', TRUE, 3, 1, 1);

-- ============================================
-- 27. CHAPTERS
-- ============================================
INSERT INTO chapters (chapter_id, chapter_name, chapter_number, description, estimated_hours, learning_objectives, is_active, syllabus_id) VALUES
(1, 'Real Numbers', 1, 'Properties of real numbers', 15, 'Understand real number properties', TRUE, 1),
(2, 'Polynomials', 2, 'Zeros of polynomials', 20, 'Find zeros and verify relationships', TRUE, 1),
(3, 'Pair of Linear Equations', 3, 'Solving linear equations', 25, 'Solve using multiple methods', TRUE, 1),
(4, 'Quadratic Equations', 4, 'Standard form, factorization', 25, 'Solve quadratic equations', TRUE, 1),
(5, 'Trigonometry', 5, 'Ratios, identities, heights', 30, 'Apply trigonometric concepts', TRUE, 1),
(6, 'Chemical Reactions', 1, 'Types of chemical reactions', 20, 'Identify and balance reactions', TRUE, 2),
(7, 'Acids, Bases and Salts', 2, 'Properties, pH scale', 18, 'Understand pH concept', TRUE, 2),
(8, 'Light', 3, 'Reflection and Refraction', 25, 'Apply laws of optics', TRUE, 2);

-- ============================================
-- 28. TOPICS
-- ============================================
INSERT INTO topics (topic_id, topic_name, topic_number, description, estimated_hours, is_active, chapter_id) VALUES
(1, 'Euclids Division Lemma', 1, 'Statement and proof', 3, TRUE, 1),
(2, 'Fundamental Theorem of Arithmetic', 2, 'Prime factorization', 4, TRUE, 1),
(3, 'Irrational Numbers', 3, 'Proving irrationality', 4, TRUE, 1),
(4, 'Decimal Expansions', 4, 'Rational number decimals', 4, TRUE, 1),
(5, 'Geometrical Meaning of Zeros', 1, 'Graphs and zeros', 5, TRUE, 2),
(6, 'Zeros and Coefficients', 2, 'Sum and product of zeros', 7, TRUE, 2),
(7, 'Division Algorithm', 3, 'Polynomial division', 8, TRUE, 2),
(8, 'Chemical Equations', 1, 'Writing and balancing', 5, TRUE, 6),
(9, 'Types of Reactions', 2, 'Combination, decomposition', 8, TRUE, 6),
(10, 'Oxidation in Daily Life', 3, 'Corrosion and rancidity', 7, TRUE, 6);

-- ============================================
-- 29. ATTENDANCE
-- ============================================
INSERT INTO attendance (id, organization_id, attendance_type, reference_id, reference_name, attendance_date, status, class_id, class_name, section_name, shift, department, room_number, remarks, marked_by) VALUES
(1, 1, 'CLASS', 1, 'Rahul Singh', '2026-05-20', 'PRESENT', 3, 'Class 10', 'Section A', 'Morning', NULL, 'A-101', NULL, 'teacher1'),
(2, 1, 'CLASS', 2, 'Ananya Reddy', '2026-05-20', 'PRESENT', 2, 'Class 9', 'Section A', 'Morning', NULL, 'A-102', NULL, 'teacher1'),
(3, 1, 'CLASS', 3, 'Vikram Malhotra', '2026-05-20', 'ABSENT', 3, 'Class 10', 'Section A', 'Morning', NULL, 'A-101', 'Medical leave', 'teacher1'),
(4, 1, 'CLASS', 1, 'Rahul Singh', '2026-05-19', 'PRESENT', 3, 'Class 10', 'Section A', 'Morning', NULL, 'A-101', NULL, 'teacher1'),
(5, 1, 'CLASS', 2, 'Ananya Reddy', '2026-05-19', 'LATE', 2, 'Class 9', 'Section A', 'Morning', NULL, 'A-102', 'Arrived 10 min late', 'teacher1'),
(6, 1, 'CLASS', 3, 'Vikram Malhotra', '2026-05-19', 'PRESENT', 3, 'Class 10', 'Section A', 'Morning', NULL, 'A-101', NULL, 'teacher1'),
(7, 1, 'STAFF', 1, 'Amit K Verma', '2026-05-20', 'PRESENT', NULL, NULL, NULL, 'Morning', 'Mathematics', NULL, NULL, 'admin'),
(8, 1, 'STAFF', 2, 'Sunita Patel', '2026-05-20', 'PRESENT', NULL, NULL, NULL, 'Morning', 'Science', NULL, NULL, 'admin'),
(9, 1, 'STAFF', 1, 'Amit K Verma', '2026-05-19', 'PRESENT', NULL, NULL, NULL, 'Morning', 'Mathematics', NULL, NULL, 'admin'),
(10, 1, 'STAFF', 2, 'Sunita Patel', '2026-05-19', 'WFH', NULL, NULL, NULL, 'Morning', 'Science', NULL, 'Working from home', 'admin');

-- ============================================
-- 30. INQUIRIES
-- ============================================
INSERT INTO inquiry (inquiry_id, name, mobile_number, email, class_interested_in, address, inquiry_source, referred_by, comments, assigned_counselor_id, status, is_deleted, last_follow_up_date) VALUES
(1, 'Arjun Kapoor', '9988776601', 'arjun.kapoor@gmail.com', 'Class 10', '23 MG Road, Bangalore', 'WEBSITE', NULL, 'Interested in CBSE curriculum', 9, 'NEW', FALSE, NULL),
(2, 'Sneha Gupta', '9988776602', 'sneha.gupta@gmail.com', 'Class 9', '45 HSR Layout', 'WALK_IN', 'Mr. Rajan', 'Visited campus', 9, 'CONTACTED', FALSE, '2026-05-18 10:30:00'),
(3, 'Karthik Iyer', '9988776603', 'karthik.i@gmail.com', 'Class 11 - Science', '67 Jayanagar', 'PHONE', NULL, 'Asking about science stream', 9, 'FOLLOW_UP_REQUIRED', FALSE, '2026-05-19 14:00:00'),
(4, 'Preethi Nair', '9988776604', 'preethi.nair@gmail.com', 'Class 8', '12 Indiranagar', 'SOCIAL_MEDIA', NULL, 'Wants brochure', 9, 'READY_FOR_ADMISSION', FALSE, '2026-05-20 11:00:00'),
(5, 'Rohan Mehta', '9988776605', 'rohan.mehta@gmail.com', 'Class 10', '88 Koramangala', 'WEBSITE', NULL, 'Online inquiry, wants callback', NULL, 'NEW', FALSE, NULL);

-- ============================================
-- 31. FOLLOW-UPS
-- ============================================
INSERT INTO follow_up (id, follow_up_type, remarks, status_after_follow_up, follow_up_date, next_follow_up_date, inquiry_id) VALUES
(1, 'CALL', 'Called parent, discussed admission process', 'CONTACTED', '2026-05-18 10:30:00', '2026-05-22', 2),
(2, 'CALL', 'Explained science stream prerequisites', 'FOLLOW_UP_REQUIRED', '2026-05-19 14:00:00', '2026-05-23', 3),
(3, 'WHATSAPP', 'Sent brochure and fee structure', 'FOLLOW_UP_REQUIRED', '2026-05-19 16:00:00', '2026-05-25', 3),
(4, 'WALK_IN', 'Parent visited, ready to submit application', 'READY_FOR_ADMISSION', '2026-05-20 11:00:00', NULL, 4);

-- ============================================
-- 32. LEAVE REQUESTS
-- ============================================
INSERT INTO leave_requests (id, organization_id, staff_id, staff_name, department, leave_type, start_date, end_date, days, reason, status, applied_by, approved_by) VALUES
(1, 1, 1, 'Amit K Verma', 'Mathematics', 'SICK', '2026-05-25', '2026-05-26', 2, 'Fever and cold', 'PENDING', 'teacher1', NULL),
(2, 1, 2, 'Sunita Patel', 'Science', 'VACATION', '2026-06-01', '2026-06-05', 5, 'Family vacation', 'APPROVED', 'teacher2', 'admin'),
(3, 1, 1, 'Amit K Verma', 'Mathematics', 'PERSONAL', '2026-04-15', '2026-04-15', 1, 'Personal work', 'APPROVED', 'teacher1', 'admin');

-- ============================================
-- 33. STAFF PAYROLL
-- ============================================
INSERT INTO staff_payroll (id, organization_id, staff_id, staff_name, department, designation, basic, hra, special_allowance, academic_allowance, medical_allowance, travel_allowance, dearness_allowance, other_allowance, professional_tax, income_tax, provident_fund, effective_from) VALUES
(1, 1, 1, 'Amit K Verma', 'Mathematics', 'Senior Teacher', 45000.00, 18000.00, 5000.00, 3000.00, 2000.00, 3000.00, 2000.00, 0.00, 200.00, 5000.00, 5400.00, '2025-04-01'),
(2, 1, 2, 'Sunita Patel', 'Science', 'Teacher', 38000.00, 15200.00, 4000.00, 2500.00, 2000.00, 2500.00, 1500.00, 0.00, 200.00, 3500.00, 4560.00, '2025-04-01');

-- ============================================
-- 34. ADMISSION FORM TEMPLATE
-- ============================================
INSERT INTO admission_form_template (id, tenant_id, title, description, guidelines, is_active) VALUES
(1, 'public', 'Standard Admission Form 2025-26', 'Application form for admissions', 'Fill all required fields. Upload docs in PDF/JPG.', TRUE);

-- ============================================
-- 35. OWNER DETAILS
-- ============================================
INSERT INTO owner_details (owner_id, owner_code, owner_name, gender, owner_email, owner_mobile, user_id, org_id) VALUES
(1, 'OWN001', 'Rajesh Kumar', 'Male', 'admin@thinkerscave.com', '9876543210', 1, 1);