-- ============================================
-- DEV Seed Data for MySQL
-- ThinkersCave SaaS - Odisha school / college demo data
-- INSERT IGNORE makes the script idempotent.
-- DevDataInitializer re-hashes user passwords to BCrypt("Password@123").
-- ============================================

-- =========================================================
-- PLATFORM MODULE
-- =========================================================

INSERT IGNORE INTO customers (
	id, customer_code, legal_name, display_name, customer_type, status,
	email, mobile_number, alternate_mobile_number, website, tax_number,
	registration_number, address_line_1, address_line_2, city, state, country,
	postal_code, logo_url, preferred_communication, onboarding_completed, active,
	remarks, created_by, updated_by, version
)
VALUES
(1, 'CUS000001', 'Javier Education Group', 'Javier Education Group', 'EDUCATION_GROUP', 'ACTIVE',
 'founder@javier.edu.in', '9777001100', '9437001100', 'https://javier.edu.in', 'ODJEG1234A',
 'REG-JEG-2024-001', 'Plot No. 12, Patia', 'Near Infocity', 'Bhubaneswar', 'Odisha', 'India',
 '751024', 'https://cdn.thinkerscave.local/logos/javier-group.png', 'WHATSAPP', TRUE, TRUE,
 'Group account for Javier School campuses in Odisha', 'system', 'system', 0),
(2, 'CUS000002', 'ABC School Trust', 'ABC School Trust', 'SCHOOL', 'ACTIVE',
 'trust@abcschool.edu.in', '9861002200', '9337002200', 'https://abcschool.edu.in', 'ODABC6789B',
 'REG-ABC-2018-014', 'Plot 44, Chandrasekharpur', 'Near Nalco Square', 'Bhubaneswar', 'Odisha', 'India',
 '751016', 'https://cdn.thinkerscave.local/logos/abc-school.png', 'EMAIL', TRUE, TRUE,
 'Single school trust operating under Odisha board', 'system', 'system', 0),
(3, 'CUS000003', 'Kalinga Learning Foundation', 'Kalinga Learning Foundation', 'EDUCATION_GROUP', 'ACTIVE',
 'contact@kalinga.edu.in', '9692003300', '9338003300', 'https://kalinga.edu.in', 'ODKLF2468C',
 'REG-KLF-2019-011', 'Plot 7, Sector 5', 'Near New Bus Stand', 'Cuttack', 'Odisha', 'India',
 '753014', 'https://cdn.thinkerscave.local/logos/kalinga-foundation.png', 'PHONE', TRUE, TRUE,
 'Group account for Kalinga campuses', 'system', 'system', 0);

INSERT IGNORE INTO customer_contacts (
	id, contact_code, customer_id, full_name, designation, contact_type, email, mobile_number,
	alternate_mobile_number, department, primary_contact, created_by, updated_by, version
)
VALUES
(1, 'CTC000001', 1, 'Sanjay Mohanty', 'Founder & Chairman', 'PRIMARY', 'founder@javier.edu.in', '9777001100', '9437001100', 'Board Office', TRUE, 'system', 'system', 0),
(2, 'CTC000002', 2, 'Dr. Madhumita Das', 'Managing Trustee', 'PRIMARY', 'trust@abcschool.edu.in', '9861002200', '9337002200', 'Trust Office', TRUE, 'system', 'system', 0),
(3, 'CTC000003', 3, 'Raghunath Patra', 'Secretary', 'PRIMARY', 'contact@kalinga.edu.in', '9692003300', '9338003300', 'Head Office', TRUE, 'system', 'system', 0);

INSERT IGNORE INTO organizations (
	id, organization_code, customer_id, organization_name, short_name, institution_type, board_name,
	email, mobile_number, alternate_mobile_number, website, address_line_1, address_line_2, city, state,
	country, postal_code, time_zone, currency, language, logo_url, status, active, onboarding_completed,
	remarks, created_by, updated_by, version
)
VALUES
(1, 'ORG000001', 1, 'Javier School Bhubaneswar', 'JSB', 'SCHOOL', 'CBSE',
 'principal@jsb.edu.in', '9777111100', '9437111100', 'https://jsb.edu.in',
 'Plot 12, Patia', 'Near Infocity', 'Bhubaneswar', 'Odisha', 'India', '751024',
 'Asia/Kolkata', 'INR', 'en-IN', 'https://cdn.thinkerscave.local/org/jsb.png', 'ACTIVE', TRUE, TRUE,
 'Main campus in Bhubaneswar', 'system', 'system', 0),
(2, 'ORG000002', 1, 'Javier School Cuttack', 'JSC', 'SCHOOL', 'CBSE',
 'principal@jsc.edu.in', '9777222200', '9437222200', 'https://jsc.edu.in',
 'Plot 7, Sector 5', 'Near New Bus Stand', 'Cuttack', 'Odisha', 'India', '753014',
 'Asia/Kolkata', 'INR', 'en-IN', 'https://cdn.thinkerscave.local/org/jsc.png', 'ACTIVE', TRUE, TRUE,
 'City campus in Cuttack', 'system', 'system', 0),
(3, 'ORG000003', 2, 'ABC School Puri', 'ABCP', 'SCHOOL', 'ICSE',
 'principal@abcpuri.edu.in', '9777333300', '9437333300', 'https://abcpuri.edu.in',
 'Marine Drive Road', 'Sipasurubali', 'Puri', 'Odisha', 'India', '752001',
 'Asia/Kolkata', 'INR', 'en-IN', 'https://cdn.thinkerscave.local/org/abc-puri.png', 'ACTIVE', TRUE, TRUE,
 'Coastal school campus in Puri', 'system', 'system', 0),
(4, 'ORG000004', 3, 'Kalinga College Cuttack', 'KCC', 'COLLEGE', 'UTKAL UNIVERSITY',
 'principal@kcc.edu.in', '9777444400', '9437444400', 'https://kcc.edu.in',
 'College Road', 'Badambadi', 'Cuttack', 'Odisha', 'India', '753012',
 'Asia/Kolkata', 'INR', 'en-IN', 'https://cdn.thinkerscave.local/org/kcc.png', 'ACTIVE', TRUE, TRUE,
 'College demo campus for validation', 'system', 'system', 0);

INSERT IGNORE INTO organization_subscriptions (
	id, organization_id, subscription_plan_id, promotion_id, start_date, end_date, trial_end_date,
	billing_cycle, plan_price, discount_amount, final_amount, student_limit_override, staff_limit_override,
	branch_limit_override, storage_limit_override, auto_renew, status, active, remarks, created_by, updated_by, version
)
VALUES
(1, 1, 2, NULL, '2026-01-01', '2026-12-31', NULL, 'YEARLY', 29990.00, 5000.00, 24990.00, 1200, 200, 5, 500, TRUE, 'ACTIVE', TRUE, 'Javier Bhubaneswar yearly subscription', 'system', 'system', 0),
(2, 2, 1, NULL, '2026-01-01', '2026-12-31', NULL, 'YEARLY', 9990.00, 0.00, 9990.00, 600, 80, 3, 200, TRUE, 'ACTIVE', TRUE, 'Javier Cuttack yearly subscription', 'system', 'system', 0),
(3, 3, 1, NULL, '2026-01-01', '2026-12-31', NULL, 'YEARLY', 9990.00, 0.00, 9990.00, 500, 60, 2, 100, TRUE, 'ACTIVE', TRUE, 'ABC Puri yearly subscription', 'system', 'system', 0),
(4, 4, 3, NULL, '2026-01-01', '2026-12-31', NULL, 'YEARLY', 99990.00, 10000.00, 89990.00, 5000, 400, 10, 1000, TRUE, 'ACTIVE', TRUE, 'Kalinga College Cuttack enterprise subscription', 'system', 'system', 0);

INSERT IGNORE INTO tenant_registry (
	id, tenant_identifier, organization_id, schema_name, database_version, migration_version, template_version,
	provision_status, database_size_mb, storage_used_mb, created_by, updated_by, version
)
VALUES
(1, 'jsb-bhubaneswar', 1, 'tenant_jsb_bhubaneswar', '1.0', '1.0', '1.0', 'COMPLETED', 128, 45, 'system', 'system', 0),
(2, 'jsc-cuttack', 2, 'tenant_jsc_cuttack', '1.0', '1.0', '1.0', 'COMPLETED', 96, 32, 'system', 'system', 0),
(3, 'abc-puri', 3, 'tenant_abc_puri', '1.0', '1.0', '1.0', 'COMPLETED', 88, 24, 'system', 'system', 0),
(4, 'kcc-cuttack', 4, 'tenant_kcc_cuttack', '1.0', '1.0', '1.0', 'COMPLETED', 140, 50, 'system', 'system', 0);

INSERT IGNORE INTO organization_domains (
	id, organization_id, sub_domain, domain, custom_domain, ssl_enabled, dns_verified,
	default_domain, status, created_by, updated_by, version
)
VALUES
(1, 1, 'jsb', 'jsb.thinkerscave.local', NULL, FALSE, TRUE, TRUE, 'ACTIVE', 'system', 'system', 0),
(2, 2, 'jsc', 'jsc.thinkerscave.local', NULL, FALSE, TRUE, TRUE, 'ACTIVE', 'system', 'system', 0),
(3, 3, 'abcpuri', 'abcpuri.thinkerscave.local', NULL, FALSE, TRUE, TRUE, 'ACTIVE', 'system', 'system', 0),
(4, 4, 'kcc', 'kcc.thinkerscave.local', NULL, FALSE, TRUE, TRUE, 'ACTIVE', 'system', 'system', 0);

INSERT IGNORE INTO organization_configurations (
	id, organization_id, default_academic_year, academic_year_start_month, student_code_pattern,
	employee_code_pattern, admission_number_pattern, receipt_number_pattern, invoice_number_pattern,
	currency, time_zone, language, date_format, created_by, updated_by, version
)
VALUES
(1, 1, '2026-27', 4, 'JSB/STU/{YY}/{SEQ}', 'JSB/EMP/{YY}/{SEQ}', 'JSB/ADM/{YY}/{SEQ}', 'JSB/REC/{YY}/{SEQ}', 'JSB/INV/{YY}/{SEQ}', 'INR', 'Asia/Kolkata', 'en-IN', 'dd-MM-yyyy', 'system', 'system', 0),
(2, 2, '2026-27', 4, 'JSC/STU/{YY}/{SEQ}', 'JSC/EMP/{YY}/{SEQ}', 'JSC/ADM/{YY}/{SEQ}', 'JSC/REC/{YY}/{SEQ}', 'JSC/INV/{YY}/{SEQ}', 'INR', 'Asia/Kolkata', 'en-IN', 'dd-MM-yyyy', 'system', 'system', 0),
(3, 3, '2026-27', 4, 'ABCP/STU/{YY}/{SEQ}', 'ABCP/EMP/{YY}/{SEQ}', 'ABCP/ADM/{YY}/{SEQ}', 'ABCP/REC/{YY}/{SEQ}', 'ABCP/INV/{YY}/{SEQ}', 'INR', 'Asia/Kolkata', 'en-IN', 'dd-MM-yyyy', 'system', 'system', 0),
(4, 4, '2026-27', 4, 'KCC/STU/{YY}/{SEQ}', 'KCC/EMP/{YY}/{SEQ}', 'KCC/ADM/{YY}/{SEQ}', 'KCC/REC/{YY}/{SEQ}', 'KCC/INV/{YY}/{SEQ}', 'INR', 'Asia/Kolkata', 'en-IN', 'dd-MM-yyyy', 'system', 'system', 0);

INSERT IGNORE INTO subscription_plans (
	id, plan_code, plan_name, description, monthly_price, quarterly_price, half_yearly_price, yearly_price,
	student_limit, staff_limit, branch_limit, storage_limit_gb, api_request_limit, trial_days, display_order,
	recommended, custom_plan, visible, active, remarks, created_by, updated_by, version
)
VALUES
(1, 'STARTER', 'Starter Plan', 'Starter plan for a single school', 1499.00, 3999.00, 6999.00, 12999.00, 300, 50, 1, 50, 100000, 14, 1, FALSE, FALSE, TRUE, TRUE, 'Good for a single campus', 'system', 'system', 0),
(2, 'GROWTH', 'Growth Plan', 'For growing school groups', 2999.00, 7999.00, 13999.00, 25999.00, 1200, 200, 5, 200, 500000, 14, 2, TRUE, FALSE, TRUE, TRUE, 'Recommended for Javier group', 'system', 'system', 0),
(3, 'ENTERPRISE', 'Enterprise Plan', 'Large scale multi-campus plan', 9999.00, 24999.00, 44999.00, 89999.00, 5000, 400, 20, 1000, 2000000, 30, 3, FALSE, FALSE, TRUE, TRUE, 'For large institutions', 'system', 'system', 0);

INSERT IGNORE INTO features (id, feature_code, feature_name, display_name, module, category, parent_feature_id, feature_key, description, icon, active, created_by, updated_by, version)
VALUES
(1, 'DASHBOARD', 'Dashboard', 'Dashboard', 'platform', 'core', NULL, 'platform.dashboard', 'Platform dashboard', 'dashboard', TRUE, 'system', 'system', 0),
(2, 'AUTH', 'Authentication', 'Authentication', 'security', 'core', NULL, 'security.auth', 'Login and session management', 'lock', TRUE, 'system', 'system', 0),
(3, 'STUDENT_MGMT', 'Student Management', 'Student Management', 'student', 'core', NULL, 'student.management', 'Student profile management', 'school', TRUE, 'system', 'system', 0),
(4, 'STAFF_MGMT', 'Staff Management', 'Staff Management', 'staff', 'core', NULL, 'staff.management', 'Staff profile management', 'badge', TRUE, 'system', 'system', 0),
(5, 'ATTENDANCE', 'Attendance', 'Attendance', 'attendance', 'core', NULL, 'attendance.management', 'Attendance tracking', 'check-circle', TRUE, 'system', 'system', 0);

INSERT IGNORE INTO subscription_plan_features (id, subscription_plan_id, feature_id, enabled, mandatory, display_order, notes, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 1, TRUE, TRUE, 1, 'Starter dashboard access', TRUE, 'system', 'system', 0),
(2, 1, 3, TRUE, TRUE, 2, 'Student module enabled', TRUE, 'system', 'system', 0),
(3, 1, 4, TRUE, TRUE, 3, 'Staff module enabled', TRUE, 'system', 'system', 0),
(4, 2, 1, TRUE, TRUE, 1, 'Growth dashboard access', TRUE, 'system', 'system', 0),
(5, 2, 2, TRUE, TRUE, 2, 'Authentication enabled', TRUE, 'system', 'system', 0),
(6, 2, 3, TRUE, TRUE, 3, 'Student module enabled', TRUE, 'system', 'system', 0),
(7, 2, 4, TRUE, TRUE, 4, 'Staff module enabled', TRUE, 'system', 'system', 0),
(8, 2, 5, TRUE, TRUE, 5, 'Attendance enabled', TRUE, 'system', 'system', 0),
(9, 3, 1, TRUE, TRUE, 1, 'Enterprise dashboard access', TRUE, 'system', 'system', 0),
(10, 3, 2, TRUE, TRUE, 2, 'Authentication enabled', TRUE, 'system', 'system', 0),
(11, 3, 3, TRUE, TRUE, 3, 'Student module enabled', TRUE, 'system', 'system', 0),
(12, 3, 4, TRUE, TRUE, 4, 'Staff module enabled', TRUE, 'system', 'system', 0),
(13, 3, 5, TRUE, TRUE, 5, 'Attendance enabled', TRUE, 'system', 'system', 0);

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(1, 'DASHBOARD', 'Dashboard', 'Main dashboard', '/dashboard', 'dashboard', 'MODULE', NULL, 1, TRUE, TRUE, TRUE, 'system', 'system', 0),
(2, 'ACCESS', 'Access', 'Access management', NULL, 'security', 'MODULE', NULL, 2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(3, 'STUDENTS', 'Students', 'Student module', '/students', 'school', 'MODULE', NULL, 3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(4, 'STAFF', 'Staff', 'Staff module', '/staff', 'badge', 'MODULE', NULL, 4, TRUE, TRUE, FALSE, 'system', 'system', 0),
(5, 'ATTENDANCE', 'Attendance', 'Attendance module', '/attendance', 'event_available', 'MODULE', NULL, 5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(6, 'ACADEMICS', 'Academics', 'Academics module', '/academics', 'book', 'MODULE', NULL, 6, TRUE, TRUE, FALSE, 'system', 'system', 0),
(7, 'ADMISSION', 'Admission', 'Admission module', '/admission', 'assignment', 'MODULE', NULL, 7, TRUE, TRUE, FALSE, 'system', 'system', 0);

INSERT IGNORE INTO roles (id, role_code, role_name, description, role_type, dashboard_code, system_role, active, display_order, created_by, updated_by, version)
VALUES
(6, 'ROLE_SUPER_ADMIN', 'ThinkersCave Super Admin', 'Platform control tower and tenant administration', 'SUPER_ADMIN', 'PLATFORM', TRUE, TRUE, 0, 'system', 'system', 0),
(1, 'ROLE_OWNER', 'Organization Owner', 'Campus owner with full access', 'ORGANIZATION_OWNER', 'ADMIN', TRUE, TRUE, 1, 'system', 'system', 0),
(2, 'ROLE_ADMIN', 'Organization Admin', 'Campus administrator', 'ORGANIZATION_ADMIN', 'ADMIN', TRUE, TRUE, 2, 'system', 'system', 0),
(3, 'ROLE_STAFF', 'Staff', 'Teaching staff', 'STAFF', 'STAFF', TRUE, TRUE, 3, 'system', 'system', 0),
(4, 'ROLE_STUDENT', 'Student', 'Student portal access', 'STUDENT', 'STUDENT', TRUE, TRUE, 4, 'system', 'system', 0),
(5, 'ROLE_PARENT', 'Parent', 'Parent portal access', 'PARENT', 'PARENT', TRUE, TRUE, 5, 'system', 'system', 0);

INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(1, 1, 1, 1, TRUE, TRUE, TRUE, 'system', 'system', 0),
(2, 1, 1, 3, TRUE, TRUE, TRUE, 'system', 'system', 0),
(3, 1, 1, 4, TRUE, TRUE, TRUE, 'system', 'system', 0),
(4, 1, 1, 5, TRUE, TRUE, TRUE, 'system', 'system', 0),
(5, 2, 1, 1, TRUE, TRUE, TRUE, 'system', 'system', 0),
(6, 2, 1, 3, TRUE, TRUE, TRUE, 'system', 'system', 0),
(7, 3, 1, 1, TRUE, TRUE, TRUE, 'system', 'system', 0),
(8, 4, 1, 1, TRUE, TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO users (
	id, organization_id, user_code, username, email, mobile_number, password, first_name, last_name,
	display_name, profile_image_url, status, email_verified, mobile_verified, first_time_login,
	failed_login_attempts, account_locked, last_login_at, password_changed_at, locked_at, lock_expiry_at,
	created_by, updated_by, version
)
VALUES
(1, 1, 'USR000001', 'superadmin', 'superadmin@thinkerscave.com', '9777000001', 'PLACEHOLDER', 'Super', 'Admin', 'Super Admin', 'https://cdn.thinkerscave.local/users/superadmin.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(2, 1, 'USR000002', 'javier.owner', 'owner@jsb.edu.in', '9777111101', 'PLACEHOLDER', 'Sanjay', 'Mohanty', 'Sanjay Mohanty', 'https://cdn.thinkerscave.local/users/jsb-owner.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(3, 1, 'USR000003', 'javier.admin', 'admin@jsb.edu.in', '9777111102', 'PLACEHOLDER', 'Ananya', 'Dash', 'Ananya Dash', 'https://cdn.thinkerscave.local/users/jsb-admin.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(4, 1, 'USR000004', 'javier.teacher1', 'teacher1@jsb.edu.in', '9777111201', 'PLACEHOLDER', 'Rupesh', 'Pati', 'Rupesh Pati', 'https://cdn.thinkerscave.local/users/jsb-teacher1.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(5, 1, 'USR000005', 'javier.teacher2', 'teacher2@jsb.edu.in', '9777111202', 'PLACEHOLDER', 'Saswati', 'Senapati', 'Saswati Senapati', 'https://cdn.thinkerscave.local/users/jsb-teacher2.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(6, 1, 'USR000006', 'javier.student1', 'student1@jsb.edu.in', '9777111301', 'PLACEHOLDER', 'Aarav', 'Mohanty', 'Aarav Mohanty', 'https://cdn.thinkerscave.local/users/student1.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(7, 1, 'USR000007', 'javier.parent1', 'parent1@jsb.edu.in', '9777111401', 'PLACEHOLDER', 'Ranjit', 'Mohanty', 'Ranjit Mohanty', 'https://cdn.thinkerscave.local/users/parent1.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(8, 2, 'USR000008', 'abc.owner', 'owner@abcpuri.edu.in', '9777333301', 'PLACEHOLDER', 'Dr. Madhumita', 'Das', 'Dr. Madhumita Das', 'https://cdn.thinkerscave.local/users/abc-owner.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(9, 2, 'USR000009', 'abc.admin', 'admin@abcpuri.edu.in', '9777333302', 'PLACEHOLDER', 'Priya', 'Patnaik', 'Priya Patnaik', 'https://cdn.thinkerscave.local/users/abc-admin.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(10, 2, 'USR000010', 'abc.teacher1', 'teacher1@abcpuri.edu.in', '9777333401', 'PLACEHOLDER', 'Suman', 'Nayak', 'Suman Nayak', 'https://cdn.thinkerscave.local/users/abc-teacher1.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(11, 2, 'USR000011', 'abc.student1', 'student1@abcpuri.edu.in', '9777333501', 'PLACEHOLDER', 'Ishita', 'Das', 'Ishita Das', 'https://cdn.thinkerscave.local/users/abc-student1.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(12, 3, 'USR000012', 'kalinga.owner', 'owner@kcc.edu.in', '9777444401', 'PLACEHOLDER', 'Dr. Namita', 'Sahoo', 'Dr. Namita Sahoo', 'https://cdn.thinkerscave.local/users/kcc-owner.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(13, 3, 'USR000013', 'kalinga.admin', 'admin@kcc.edu.in', '9777444402', 'PLACEHOLDER', 'Sourav', 'Swain', 'Sourav Swain', 'https://cdn.thinkerscave.local/users/kcc-admin.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(14, 3, 'USR000014', 'kalinga.teacher1', 'teacher1@kcc.edu.in', '9777444501', 'PLACEHOLDER', 'Madhuri', 'Tripathy', 'Madhuri Tripathy', 'https://cdn.thinkerscave.local/users/kcc-teacher1.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(15, 3, 'USR000015', 'kalinga.student1', 'student1@kcc.edu.in', '9777444601', 'PLACEHOLDER', 'Ritika', 'Panda', 'Ritika Panda', 'https://cdn.thinkerscave.local/users/kcc-student1.png', 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0);

INSERT IGNORE INTO user_roles (id, user_id, role_id, primary_role, active, created_by, updated_by, version)
VALUES
(1, 1, 6, TRUE, TRUE, 'system', 'system', 0),
(2, 2, 1, TRUE, TRUE, 'system', 'system', 0),
(3, 3, 2, TRUE, TRUE, 'system', 'system', 0),
(4, 4, 3, TRUE, TRUE, 'system', 'system', 0),
(5, 5, 3, TRUE, TRUE, 'system', 'system', 0),
(6, 6, 4, TRUE, TRUE, 'system', 'system', 0),
(7, 7, 5, TRUE, TRUE, 'system', 'system', 0),
(8, 8, 1, TRUE, TRUE, 'system', 'system', 0),
(9, 9, 2, TRUE, TRUE, 'system', 'system', 0),
(10, 10, 3, TRUE, TRUE, 'system', 'system', 0),
(11, 11, 4, TRUE, TRUE, 'system', 'system', 0),
(12, 12, 1, TRUE, TRUE, 'system', 'system', 0),
(13, 13, 2, TRUE, TRUE, 'system', 'system', 0),
(14, 14, 3, TRUE, TRUE, 'system', 'system', 0),
(15, 15, 4, TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO security_policies (
	id, organization_id, min_password_length, require_uppercase, require_lowercase, require_numbers,
	require_special_chars, password_expiry_days, password_history_count, max_failed_attempts,
	lockout_duration_minutes, session_timeout_minutes, max_concurrent_sessions, allow_remember_me,
	require_two_factor, active, created_by, updated_by, version
)
VALUES
(1, 1, 8, TRUE, TRUE, TRUE, FALSE, 90, 5, 5, 30, 60, 3, TRUE, FALSE, TRUE, 'system', 'system', 0),
(2, 2, 8, TRUE, TRUE, TRUE, FALSE, 90, 5, 5, 30, 60, 3, TRUE, FALSE, TRUE, 'system', 'system', 0),
(3, 3, 8, TRUE, TRUE, TRUE, FALSE, 90, 5, 5, 30, 60, 3, TRUE, FALSE, TRUE, 'system', 'system', 0),
(4, 4, 8, TRUE, TRUE, TRUE, FALSE, 90, 5, 5, 30, 60, 3, TRUE, FALSE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO login_history (id, user_id, status, login_time, logout_time, ip_address, browser, operating_system, failure_reason, created_by, updated_by, version)
VALUES
(1, 1, 'SUCCESS', '2026-06-29 08:00:00', NULL, '127.0.0.1', 'Chrome', 'Windows 11', NULL, 'system', 'system', 0),
(2, 2, 'SUCCESS', '2026-06-29 08:05:00', NULL, '127.0.0.1', 'Chrome', 'Windows 11', NULL, 'system', 'system', 0),
(3, 3, 'SUCCESS', '2026-06-29 08:10:00', NULL, '127.0.0.1', 'Edge', 'macOS', NULL, 'system', 'system', 0),
(4, 6, 'FAILED', '2026-06-29 08:15:00', NULL, '127.0.0.1', 'Chrome', 'Windows 11', 'Wrong password', 'system', 'system', 0);

INSERT IGNORE INTO user_sessions (id, user_id, refresh_token, device_name, browser, operating_system, ip_address, login_at, logout_at, status, created_by, updated_by, version)
VALUES
(1, 1, 'seed-refresh-token-superadmin', 'Lenovo ThinkPad', 'Chrome', 'Windows 11', '127.0.0.1', '2026-06-30 08:30:00', NULL, 'ACTIVE', 'system', 'system', 0),
(2, 3, 'seed-refresh-token-jsb-admin', 'MacBook Air', 'Edge', 'macOS', '127.0.0.1', '2026-06-30 08:35:00', NULL, 'ACTIVE', 'system', 'system', 0),
(3, 8, 'seed-refresh-token-abc-owner', 'HP ProBook', 'Chrome', 'Windows 11', '127.0.0.1', '2026-06-30 08:40:00', NULL, 'ACTIVE', 'system', 'system', 0);

INSERT IGNORE INTO password_reset_token (id, token, expiration_date, user_id, created_by, updated_by, version)
VALUES
(1, '483920', '2026-07-01 10:00:00', 6, 'system', 'system', 0);

-- =========================================================
-- ACADEMICS MODULE
-- =========================================================

INSERT IGNORE INTO academic_year (academic_year_id, year_code, year_name, start_date, end_date, current_year, active, remarks, created_by, updated_by, version)
VALUES
(1, 'AY2026', 'Academic Year 2026-27', '2026-04-01', '2027-03-31', TRUE, TRUE, 'Current academic year', 'system', 'system', 0),
(2, 'AY2025', 'Academic Year 2025-26', '2025-04-01', '2026-03-31', FALSE, TRUE, 'Previous academic year', 'system', 'system', 0);

INSERT IGNORE INTO academic_class (class_id, academic_year_id, class_code, class_name, academic_stage, display_order, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 'NURSERY', 'Nursery', 'PRE_PRIMARY', 1, TRUE, 'Pre primary group', 'system', 'system', 0),
(2, 1, 'LKG', 'Lower KG', 'PRE_PRIMARY', 2, TRUE, 'Lower kindergarten', 'system', 'system', 0),
(3, 1, 'UKG', 'Upper KG', 'PRE_PRIMARY', 3, TRUE, 'Upper kindergarten', 'system', 'system', 0),
(4, 1, 'I', 'Class I', 'PRIMARY', 4, TRUE, 'Primary class', 'system', 'system', 0),
(5, 1, 'II', 'Class II', 'PRIMARY', 5, TRUE, 'Primary class', 'system', 'system', 0),
(6, 1, 'III', 'Class III', 'PRIMARY', 6, TRUE, 'Primary class', 'system', 'system', 0),
(7, 1, 'XI-COM', 'Class XI Commerce', 'SENIOR_SECONDARY', 7, TRUE, 'College validation batch', 'system', 'system', 0);

INSERT IGNORE INTO academic_section (section_id, class_id, section_name, capacity, active, remarks, created_by, updated_by, version)
VALUES
(1, 4, 'A', 40, TRUE, 'Class I Section A', 'system', 'system', 0),
(2, 4, 'B', 40, TRUE, 'Class I Section B', 'system', 'system', 0),
(3, 5, 'A', 40, TRUE, 'Class II Section A', 'system', 'system', 0),
(4, 6, 'A', 40, TRUE, 'Class III Section A', 'system', 'system', 0),
(5, 7, 'A', 60, TRUE, 'Commerce Section A', 'system', 'system', 0);

INSERT IGNORE INTO subject (subject_id, subject_code, subject_name, subject_type, active, remarks, created_by, updated_by, version)
VALUES
(1, 'ENG', 'English', 'CORE', TRUE, 'Core language subject', 'system', 'system', 0),
(2, 'MTH', 'Mathematics', 'CORE', TRUE, 'Core mathematics subject', 'system', 'system', 0),
(3, 'SCI', 'Science', 'CORE', TRUE, 'Core science subject', 'system', 'system', 0),
(4, 'SST', 'Social Studies', 'CORE', TRUE, 'Core social science subject', 'system', 'system', 0),
(5, 'ORI', 'Odia', 'LANGUAGE', TRUE, 'Odisha regional language', 'system', 'system', 0),
(6, 'COM', 'Computer', 'SKILL', TRUE, 'Computer literacy subject', 'system', 'system', 0),
(7, 'HIN', 'Hindi', 'LANGUAGE', TRUE, 'Second language subject', 'system', 'system', 0),
(8, 'GKC', 'General Knowledge', 'SKILL', TRUE, 'General awareness', 'system', 'system', 0),
(9, 'ACC', 'Accountancy', 'CORE', TRUE, 'College commerce subject', 'system', 'system', 0),
(10, 'BUS', 'Business Studies', 'CORE', TRUE, 'College commerce subject', 'system', 'system', 0);

INSERT IGNORE INTO academic_setting (setting_id, setting_key, setting_value, category, active, remarks, created_by, updated_by, version)
VALUES
(1, 'DEFAULT_CURRENCY', 'INR', 'GENERAL', TRUE, 'Default currency for all campuses', 'system', 'system', 0),
(2, 'DEFAULT_TIME_ZONE', 'Asia/Kolkata', 'GENERAL', TRUE, 'Indian timezone', 'system', 'system', 0),
(3, 'ACADEMIC_YEAR_START_MONTH', '4', 'GENERAL', TRUE, 'April academic year start', 'system', 'system', 0),
(4, 'ATTENDANCE_MODE', 'DAILY', 'ATTENDANCE', TRUE, 'Daily attendance by default', 'system', 'system', 0);

INSERT IGNORE INTO academic_schedule (schedule_id, academic_year_id, schedule_name, start_date, end_date, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 'Regular Schedule 2026-27', '2026-04-01', '2027-03-31', TRUE, 'Regular campus schedule', 'system', 'system', 0);

INSERT IGNORE INTO timetable_template (template_id, schedule_id, template_name, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 'Primary School Timetable', TRUE, 'Default primary timetable', 'system', 'system', 0);

INSERT IGNORE INTO period_template (period_template_id, template_id, period_number, period_name, start_time, end_time, period_type, display_order, active, created_by, updated_by, version)
VALUES
(1, 1, 1, 'Period 1', '08:00:00', '08:45:00', 'CLASS', 1, TRUE, 'system', 'system', 0),
(2, 1, 2, 'Period 2', '08:45:00', '09:30:00', 'CLASS', 2, TRUE, 'system', 'system', 0),
(3, 1, 3, 'Break', '09:30:00', '09:45:00', 'BREAK', 3, TRUE, 'system', 'system', 0),
(4, 1, 4, 'Period 3', '09:45:00', '10:30:00', 'CLASS', 4, TRUE, 'system', 'system', 0),
(5, 1, 5, 'Lunch', '12:30:00', '13:00:00', 'LUNCH', 5, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO subject_assignment (subject_assignment_id, academic_year_id, class_id, section_id, subject_id, teacher_id, periods_per_week, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 4, 1, 1, 4, 5, TRUE, 'English for Class I A', 'system', 'system', 0),
(2, 1, 4, 1, 2, 5, 5, TRUE, 'Math for Class I A', 'system', 'system', 0),
(3, 1, 4, 1, 5, 4, 3, TRUE, 'Odia for Class I A', 'system', 'system', 0),
(4, 1, 5, 3, 1, 10, 5, TRUE, 'English for Class II A', 'system', 'system', 0),
(5, 1, 6, 4, 2, 14, 5, TRUE, 'Math for Class III A', 'system', 'system', 0),
(6, 1, 7, 5, 9, 14, 4, TRUE, 'Accountancy for Class XI Commerce', 'system', 'system', 0),
(7, 1, 7, 5, 10, 14, 4, TRUE, 'Business Studies for Class XI Commerce', 'system', 'system', 0);

INSERT IGNORE INTO timetable_slot (slot_id, academic_year_id, class_id, section_id, subject_assignment_id, period_template_id, day_of_week, active, created_by, updated_by, version)
VALUES
(1, 1, 4, 1, 1, 1, 'MONDAY', TRUE, 'system', 'system', 0),
(2, 1, 4, 1, 2, 2, 'MONDAY', TRUE, 'system', 'system', 0),
(3, 1, 4, 1, 3, 4, 'MONDAY', TRUE, 'system', 'system', 0),
(4, 1, 5, 3, 4, 1, 'TUESDAY', TRUE, 'system', 'system', 0),
(5, 1, 6, 4, 5, 2, 'WEDNESDAY', TRUE, 'system', 'system', 0),
(6, 1, 7, 5, 6, 1, 'THURSDAY', TRUE, 'system', 'system', 0),
(7, 1, 7, 5, 7, 2, 'FRIDAY', TRUE, 'system', 'system', 0);

INSERT IGNORE INTO teacher_arrangement (arrangement_id, slot_id, absent_teacher_id, substitute_teacher_id, arrangement_date, status, reason, approved_by, active, created_by, updated_by, version)
VALUES
(1, 1, 4, 5, '2026-06-24', 'APPROVED', 'Teacher on training at Bhubaneswar cluster office', 3, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO syllabus (syllabus_id, academic_year_id, class_id, subject_id, title, version_no, published, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 4, 1, 'Class I English Syllabus', 'v1', TRUE, TRUE, 'Bhubaneswar curriculum', 'system', 'system', 0),
(2, 1, 4, 2, 'Class I Maths Syllabus', 'v1', TRUE, TRUE, 'Bhubaneswar curriculum', 'system', 'system', 0),
(3, 1, 5, 1, 'Class II English Syllabus', 'v1', TRUE, TRUE, 'Cuttack curriculum', 'system', 'system', 0),
(4, 1, 7, 9, 'Class XI Accountancy Syllabus', 'v1', TRUE, TRUE, 'College syllabus', 'system', 'system', 0);

INSERT IGNORE INTO syllabus_unit (unit_id, syllabus_id, unit_number, unit_name, estimated_hours, display_order, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 1, 'My Family', 8, 1, TRUE, 'Intro unit', 'system', 'system', 0),
(2, 1, 2, 'My School', 10, 2, TRUE, 'School life unit', 'system', 'system', 0),
(3, 2, 1, 'Numbers 1 to 20', 12, 1, TRUE, 'Math basics', 'system', 'system', 0),
(4, 4, 1, 'Introduction to Accountancy', 10, 1, TRUE, 'College chapter planning', 'system', 'system', 0);

INSERT IGNORE INTO syllabus_chapter (chapter_id, unit_id, chapter_number, chapter_name, estimated_hours, display_order, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 1, 'Introduction to Family', 4, 1, TRUE, 'Chapter 1', 'system', 'system', 0),
(2, 1, 2, 'Family Members', 4, 2, TRUE, 'Chapter 2', 'system', 'system', 0),
(3, 3, 1, 'Counting Numbers', 6, 1, TRUE, 'Counting chapter', 'system', 'system', 0),
(4, 4, 1, 'Basic Concepts of Accountancy', 5, 1, TRUE, 'College chapter', 'system', 'system', 0);

INSERT IGNORE INTO syllabus_topic (topic_id, chapter_id, topic_number, topic_name, estimated_hours, display_order, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 1, 'Introduction to Family', 2, 1, TRUE, 'Family topic', 'system', 'system', 0),
(2, 2, 1, 'Father, Mother, Sibling', 2, 1, TRUE, 'Family members', 'system', 'system', 0),
(3, 3, 1, 'Counting 1 to 10', 3, 1, TRUE, 'Math topic', 'system', 'system', 0),
(4, 4, 1, 'Nature of Accountancy', 3, 1, TRUE, 'College topic', 'system', 'system', 0);

INSERT IGNORE INTO syllabus_coverage (coverage_id, topic_id, teacher_id, status, completion_date, remarks, created_by, updated_by, version)
VALUES
(1, 1, 4, 'COMPLETED', '2026-06-20', 'Completed in time', 'system', 'system', 0),
(2, 2, 4, 'IN_PROGRESS', NULL, 'Will finish this week', 'system', 'system', 0),
(3, 3, 5, 'NOT_STARTED', NULL, 'Pending after revision', 'system', 'system', 0),
(4, 4, 14, 'IN_PROGRESS', NULL, 'College syllabus in progress', 'system', 'system', 0);

INSERT IGNORE INTO academic_calendar_event (event_id, academic_year_id, title, event_type, start_date, end_date, all_day, description, active, created_by, updated_by, version)
VALUES
(1, 1, 'Utkal Divas Celebration', 'SPECIAL_DAY', '2026-04-01', '2026-04-01', TRUE, 'Odisha state day celebration', TRUE, 'system', 'system', 0),
(2, 1, 'Mid Term Examination', 'EXAM', '2026-09-15', '2026-09-22', TRUE, 'Mid term exams', TRUE, 'system', 'system', 0),
(3, 1, 'Parent Meeting', 'PARENT_MEETING', '2026-08-10', '2026-08-10', TRUE, 'Parent teacher interaction', TRUE, 'system', 'system', 0),
(4, 1, 'College Orientation Week', 'TERM_START', '2026-07-01', '2026-07-07', TRUE, 'Orientation for Kalinga college', TRUE, 'system', 'system', 0);

-- =========================================================
-- STAFF MODULE
-- =========================================================

INSERT IGNORE INTO responsibility (responsibility_id, responsibility_code, responsibility_name, description, system_defined, display_order, active, remarks, created_by, updated_by, version)
VALUES
(1, 'CLASS_TEACHER', 'Class Teacher', 'Handles class coordination', TRUE, 1, TRUE, 'System responsibility', 'system', 'system', 0),
(2, 'SUBJECT_HEAD', 'Subject Head', 'Leads a subject department', TRUE, 2, TRUE, 'System responsibility', 'system', 'system', 0),
(3, 'EXAM_COORDINATOR', 'Exam Coordinator', 'Coordinates exams', TRUE, 3, TRUE, 'System responsibility', 'system', 'system', 0),
(4, 'DISCIPLINE_INCHARGE', 'Discipline Incharge', 'Handles discipline', TRUE, 4, TRUE, 'System responsibility', 'system', 'system', 0);

INSERT IGNORE INTO staff (
	staff_id, user_id, staff_code, first_name, middle_name, last_name, gender, date_of_birth, blood_group,
	religion, nationality, mobile_number, email, staff_type, designation, employment_category, employment_status,
	joining_date, contract_start_date, contract_end_date, highest_qualification, experience_years,
	emergency_contact_name, emergency_contact_relation, emergency_contact_number, photo_url, active, created_by, updated_by, version
)
VALUES
(1, 4, 'JSB-TCHR-001', 'Rupesh', NULL, 'Pati', 'Male', '1990-03-14', 'B+', 'Hindu', 'Indian', '9777111201', 'teacher1@jsb.edu.in', 'TEACHING', 'English Teacher', 'PERMANENT', 'ACTIVE', '2022-06-01', NULL, NULL, 'M.A. English, B.Ed.', 8, 'Suresh Pati', 'Father', '9777000201', 'https://cdn.thinkerscave.local/staff/jsb-rupesh.png', TRUE, 'system', 'system', 0),
(2, 5, 'JSB-TCHR-002', 'Saswati', NULL, 'Senapati', 'Female', '1992-07-09', 'A+', 'Hindu', 'Indian', '9777111202', 'teacher2@jsb.edu.in', 'TEACHING', 'Mathematics Teacher', 'PERMANENT', 'ACTIVE', '2023-02-15', NULL, NULL, 'M.Sc. Mathematics, B.Ed.', 6, 'Anand Senapati', 'Husband', '9777000202', 'https://cdn.thinkerscave.local/staff/jsb-saswati.png', TRUE, 'system', 'system', 0),
(3, 10, 'ABCP-TCHR-001', 'Suman', NULL, 'Nayak', 'Male', '1989-11-21', 'O+', 'Hindu', 'Indian', '9777333401', 'teacher1@abcpuri.edu.in', 'TEACHING', 'Science Teacher', 'PERMANENT', 'ACTIVE', '2021-07-10', NULL, NULL, 'M.Sc. Physics, B.Ed.', 9, 'Rina Nayak', 'Wife', '9777000301', 'https://cdn.thinkerscave.local/staff/abc-suman.png', TRUE, 'system', 'system', 0),
(4, 14, 'KCC-TCHR-001', 'Madhuri', NULL, 'Tripathy', 'Female', '1991-01-18', 'AB+', 'Hindu', 'Indian', '9777444501', 'teacher1@kcc.edu.in', 'TEACHING', 'Commerce Lecturer', 'PERMANENT', 'ACTIVE', '2020-08-01', NULL, NULL, 'M.Com, NET', 10, 'Bikash Tripathy', 'Brother', '9777000401', 'https://cdn.thinkerscave.local/staff/kcc-madhuri.png', TRUE, 'system', 'system', 0),
(5, 3, 'JSB-ADM-001', 'Ananya', NULL, 'Dash', 'Female', '1987-05-10', 'O+', 'Hindu', 'Indian', '9777111102', 'admin@jsb.edu.in', 'NON_TEACHING', 'Office Administrator', 'PERMANENT', 'ACTIVE', '2020-01-15', NULL, NULL, 'B.Com, MBA', 11, 'Sandip Dash', 'Brother', '9777000203', 'https://cdn.thinkerscave.local/staff/jsb-ananya.png', TRUE, 'system', 'system', 0),
(6, 9, 'ABCP-ADM-001', 'Priya', NULL, 'Patnaik', 'Female', '1988-09-03', 'A+', 'Hindu', 'Indian', '9777333402', 'admin@abcpuri.edu.in', 'NON_TEACHING', 'Campus Admin', 'PERMANENT', 'ACTIVE', '2019-11-01', NULL, NULL, 'B.A., DCA', 12, 'Sanjay Patnaik', 'Brother', '9777000302', 'https://cdn.thinkerscave.local/staff/abc-priya.png', TRUE, 'system', 'system', 0);

INSERT IGNORE INTO responsibility_assignment (assignment_id, staff_id, responsibility_id, scope, effective_from, effective_to, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 1, 'Class I A', '2026-04-01', NULL, TRUE, 'Class teacher responsibility', 'system', 'system', 0),
(2, 2, 2, 'Mathematics Department', '2026-04-01', NULL, TRUE, 'Math lead', 'system', 'system', 0),
(3, 5, 3, 'Javier Bhubaneswar', '2026-04-01', NULL, TRUE, 'Exam coordination', 'system', 'system', 0),
(4, 6, 4, 'ABC Puri', '2026-04-01', NULL, TRUE, 'Discipline handling', 'system', 'system', 0);

INSERT IGNORE INTO staff_salary_structure (
	salary_structure_id, staff_id, salary_type, basic_pay, hra, da, special_allowance, transport_allowance,
	other_allowance, gross_salary, bank_name, account_holder_name, account_number, ifsc_code, effective_from,
	effective_to, active, remarks, created_by, updated_by, version
)
VALUES
(1, 1, 'MONTHLY', 28000.00, 8000.00, 3000.00, 2000.00, 1500.00, 500.00, 43000.00, 'State Bank of India', 'Rupesh Pati', '321100111222', 'SBIN0001234', '2026-04-01', NULL, TRUE, 'Primary school teacher salary', 'system', 'system', 0),
(2, 2, 'MONTHLY', 30000.00, 8500.00, 3200.00, 2200.00, 1500.00, 500.00, 45900.00, 'HDFC Bank', 'Saswati Senapati', '321100111223', 'HDFC0001235', '2026-04-01', NULL, TRUE, 'Math teacher salary', 'system', 'system', 0),
(3, 5, 'MONTHLY', 26000.00, 7000.00, 2500.00, 1800.00, 1000.00, 500.00, 38800.00, 'Bank of Baroda', 'Ananya Dash', '321100111224', 'BARB0JSB001', '2026-04-01', NULL, TRUE, 'Admin staff salary', 'system', 'system', 0),
(4, 6, 'MONTHLY', 27000.00, 7500.00, 2600.00, 1900.00, 1000.00, 500.00, 40500.00, 'Canara Bank', 'Priya Patnaik', '321100111225', 'CNRB0002345', '2026-04-01', NULL, TRUE, 'Campus admin salary', 'system', 'system', 0);

INSERT IGNORE INTO payroll (
	payroll_id, staff_id, payroll_year, payroll_month, working_days, present_days, leave_without_pay_days,
	gross_salary, total_deductions, net_salary, generated_on, paid_on, status, remarks, created_by, updated_by, version
)
VALUES
(1, 1, 2026, 6, 26, 25, 1, 43000.00, 1500.00, 41500.00, '2026-06-25', '2026-06-30', 'PAID', 'June payroll processed', 'system', 'system', 0),
(2, 2, 2026, 6, 26, 26, 0, 45900.00, 1200.00, 44700.00, '2026-06-25', '2026-06-30', 'PAID', 'June payroll processed', 'system', 'system', 0),
(3, 5, 2026, 6, 26, 24, 2, 38800.00, 2200.00, 36600.00, '2026-06-25', '2026-06-30', 'PAID', 'June payroll processed', 'system', 'system', 0),
(4, 6, 2026, 6, 26, 26, 0, 40500.00, 800.00, 39700.00, '2026-06-25', '2026-06-30', 'PAID', 'June payroll processed', 'system', 'system', 0);

-- =========================================================
-- STUDENT MODULE
-- =========================================================

INSERT IGNORE INTO parent (
	parent_id, parent_code, first_name, middle_name, last_name, gender, mobile_number, alternate_mobile,
	email, occupation, organization_name, qualification, annual_income, photo_url, active, remarks, user_id,
	created_by, updated_by, version
)
VALUES
(1, 'PAR000001', 'Ranjit', NULL, 'Mohanty', 'Male', '9777111401', '9437111401', 'parent1@jsb.edu.in', 'Business', 'Mohanty Traders', 'Graduate', 950000.00, 'https://cdn.thinkerscave.local/parents/ranjit.png', TRUE, 'Father of Aarav Mohanty', 7, 'system', 'system', 0),
(2, 'PAR000002', 'Madhumita', NULL, 'Das', 'Female', '9777334401', NULL, 'mother@abcpuri.edu.in', 'Teacher', 'Government High School', 'Post Graduate', 720000.00, 'https://cdn.thinkerscave.local/parents/madhumita.png', TRUE, 'Mother of Ishita Das', NULL, 'system', 'system', 0),
(3, 'PAR000003', 'Sangeeta', NULL, 'Sahoo', 'Female', '9777445401', NULL, 'mother@kcc.edu.in', 'Accountant', 'Private Firm', 'Graduate', 680000.00, 'https://cdn.thinkerscave.local/parents/sangeeta.png', TRUE, 'Mother of Ritika Panda', NULL, 'system', 'system', 0);

INSERT IGNORE INTO student (
	student_id, student_code, admission_number, roll_number, first_name, middle_name, last_name, gender,
	date_of_birth, religion, nationality, mother_tongue, mobile_number, email, photo_url, admission_date,
	status, transport_required, hostel_required, same_address, user_id, remarks, created_by, updated_by, version
)
VALUES
(1, 'JSB-STU-0001', 'JSB-ADM-26001', '1', 'Aarav', NULL, 'Mohanty', 'Male', '2019-05-18', 'Hindu', 'Indian', 'Odia', 9777111301, 'student1@jsb.edu.in', 'https://cdn.thinkerscave.local/students/aarav.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, 6, 'Class I student from Bhubaneswar', 'system', 'system', 0),
(2, 'JSB-STU-0002', 'JSB-ADM-26002', '2', 'Ishani', NULL, 'Mishra', 'Female', '2019-08-24', 'Hindu', 'Indian', 'Odia', 9777111302, 'ishani@jsb.edu.in', 'https://cdn.thinkerscave.local/students/ishani.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class I student from Bhubaneswar', 'system', 'system', 0),
(3, 'JSB-STU-0003', 'JSB-ADM-26003', '3', 'Aditya', NULL, 'Behera', 'Male', '2018-12-03', 'Hindu', 'Indian', 'Odia', 9777111303, 'aditya@jsb.edu.in', 'https://cdn.thinkerscave.local/students/aditya.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II student from Bhubaneswar', 'system', 'system', 0),
(4, 'JSB-STU-0004', 'JSB-ADM-26004', '4', 'Tanvi', NULL, 'Nayak', 'Female', '2018-07-13', 'Hindu', 'Indian', 'Odia', 9777111304, 'tanvi@jsb.edu.in', 'https://cdn.thinkerscave.local/students/tanvi.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II student from Bhubaneswar', 'system', 'system', 0),
(5, 'JSC-STU-0001', 'JSC-ADM-26001', '1', 'Arjun', NULL, 'Pani', 'Male', '2019-02-11', 'Hindu', 'Indian', 'Odia', 9777222301, 'arjun@jsc.edu.in', 'https://cdn.thinkerscave.local/students/arjun.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class I student from Cuttack', 'system', 'system', 0),
(6, 'JSC-STU-0002', 'JSC-ADM-26002', '2', 'Manya', NULL, 'Swain', 'Female', '2018-10-02', 'Hindu', 'Indian', 'Odia', 9777222302, 'manya@jsc.edu.in', 'https://cdn.thinkerscave.local/students/manya.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class I student from Cuttack', 'system', 'system', 0),
(7, 'JSC-STU-0003', 'JSC-ADM-26003', '3', 'Kunal', NULL, 'Das', 'Male', '2017-04-19', 'Hindu', 'Indian', 'Odia', 9777222303, 'kunal@jsc.edu.in', 'https://cdn.thinkerscave.local/students/kunal.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II student from Cuttack', 'system', 'system', 0),
(8, 'JSC-STU-0004', 'JSC-ADM-26004', '4', 'Riya', NULL, 'Mohanty', 'Female', '2017-11-28', 'Hindu', 'Indian', 'Odia', 9777222304, 'riya@jsc.edu.in', 'https://cdn.thinkerscave.local/students/riya.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II student from Cuttack', 'system', 'system', 0),
(9, 'ABCP-STU-0001', 'ABCP-ADM-26001', '1', 'Ishita', NULL, 'Das', 'Female', '2019-01-14', 'Hindu', 'Indian', 'Odia', 9777333501, 'student1@abcpuri.edu.in', 'https://cdn.thinkerscave.local/students/ishita.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, 11, 'Class I student from Puri', 'system', 'system', 0),
(10, 'ABCP-STU-0002', 'ABCP-ADM-26002', '2', 'Sai', NULL, 'Panda', 'Male', '2018-06-20', 'Hindu', 'Indian', 'Odia', 9777333502, 'sai@abcpuri.edu.in', 'https://cdn.thinkerscave.local/students/sai.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class I student from Puri', 'system', 'system', 0),
(11, 'ABCP-STU-0003', 'ABCP-ADM-26003', '3', 'Anushka', NULL, 'Jena', 'Female', '2017-09-09', 'Hindu', 'Indian', 'Odia', 9777333503, 'anushka@abcpuri.edu.in', 'https://cdn.thinkerscave.local/students/anushka.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II student from Puri', 'system', 'system', 0),
(12, 'ABCP-STU-0004', 'ABCP-ADM-26004', '4', 'Kabir', NULL, 'Pradhan', 'Male', '2017-03-08', 'Hindu', 'Indian', 'Odia', 9777333504, 'kabir@abcpuri.edu.in', 'https://cdn.thinkerscave.local/students/kabir.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II student from Puri', 'system', 'system', 0),
(13, 'KCC-STU-0001', 'KCC-ADM-26001', '1', 'Ritika', NULL, 'Panda', 'Female', '2006-05-22', 'Hindu', 'Indian', 'Odia', 9777444601, 'student1@kcc.edu.in', 'https://cdn.thinkerscave.local/students/ritika.png', '2026-04-05', 'ACTIVE', FALSE, TRUE, TRUE, 15, 'Degree student in Cuttack', 'system', 'system', 0),
(14, 'KCC-STU-0002', 'KCC-ADM-26002', '2', 'Debasish', NULL, 'Barik', 'Male', '2005-12-11', 'Hindu', 'Indian', 'Odia', 9777444602, 'debasish@kcc.edu.in', 'https://cdn.thinkerscave.local/students/debasish.png', '2026-04-05', 'ACTIVE', FALSE, TRUE, TRUE, NULL, 'College student from Cuttack', 'system', 'system', 0),
(15, 'KCC-STU-0003', 'KCC-ADM-26003', '3', 'Sreeja', NULL, 'Sahu', 'Female', '2006-08-15', 'Hindu', 'Indian', 'Odia', 9777444603, 'sreeja@kcc.edu.in', 'https://cdn.thinkerscave.local/students/sreeja.png', '2026-04-05', 'ACTIVE', FALSE, TRUE, TRUE, NULL, 'College student from Cuttack', 'system', 'system', 0),
(16, 'KCC-STU-0004', 'KCC-ADM-26004', '4', 'Aniket', NULL, 'Mahapatra', 'Male', '2005-10-03', 'Hindu', 'Indian', 'Odia', 9777444604, 'aniket@kcc.edu.in', 'https://cdn.thinkerscave.local/students/aniket.png', '2026-04-05', 'ACTIVE', FALSE, TRUE, TRUE, NULL, 'College student from Cuttack', 'system', 'system', 0),
(17, 'JSB-STU-0005', 'JSB-ADM-26005', '5', 'Sambit', NULL, 'Behera', 'Male', '2018-04-02', 'Hindu', 'Indian', 'Odia', 9777111305, 'sambit@jsb.edu.in', 'https://cdn.thinkerscave.local/students/sambit.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Another Bhubaneswar student', 'system', 'system', 0),
(18, 'JSC-STU-0005', 'JSC-ADM-26005', '5', 'Nitya', NULL, 'Mohanty', 'Female', '2018-12-27', 'Hindu', 'Indian', 'Odia', 9777222305, 'nitya@jsc.edu.in', 'https://cdn.thinkerscave.local/students/nitya.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Another Cuttack student', 'system', 'system', 0),
(19, 'ABCP-STU-0005', 'ABCP-ADM-26005', '5', 'Tushar', NULL, 'Nayak', 'Male', '2018-01-17', 'Hindu', 'Indian', 'Odia', 9777333505, 'tushar@abcpuri.edu.in', 'https://cdn.thinkerscave.local/students/tushar.png', '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Another Puri student', 'system', 'system', 0),
(20, 'KCC-STU-0005', 'KCC-ADM-26005', '5', 'Pallavi', NULL, 'Sahoo', 'Female', '2006-02-12', 'Hindu', 'Indian', 'Odia', 9777444605, 'pallavi@kcc.edu.in', 'https://cdn.thinkerscave.local/students/pallavi.png', '2026-04-05', 'ACTIVE', FALSE, TRUE, TRUE, NULL, 'Another college student', 'system', 'system', 0);

INSERT IGNORE INTO student_enrollment (
	enrollment_id, student_id, academic_year_id, class_id, section_id, roll_number, status, active, remarks, created_by, updated_by, version
)
VALUES
(1, 1, 1, 4, 1, '1', 'ACTIVE', TRUE, 'Enrolled in Class I A', 'system', 'system', 0),
(2, 2, 1, 4, 1, '2', 'ACTIVE', TRUE, 'Enrolled in Class I A', 'system', 'system', 0),
(3, 3, 1, 5, 3, '1', 'ACTIVE', TRUE, 'Enrolled in Class II A', 'system', 'system', 0),
(4, 4, 1, 5, 3, '2', 'ACTIVE', TRUE, 'Enrolled in Class II A', 'system', 'system', 0),
(5, 5, 1, 4, 2, '1', 'ACTIVE', TRUE, 'Enrolled in Class I B', 'system', 'system', 0),
(6, 6, 1, 4, 2, '2', 'ACTIVE', TRUE, 'Enrolled in Class I B', 'system', 'system', 0),
(7, 7, 1, 5, 4, '1', 'ACTIVE', TRUE, 'Enrolled in Class II B', 'system', 'system', 0),
(8, 8, 1, 5, 4, '2', 'ACTIVE', TRUE, 'Enrolled in Class II B', 'system', 'system', 0),
(9, 9, 1, 4, 1, '3', 'ACTIVE', TRUE, 'Enrolled in Class I A', 'system', 'system', 0),
(10, 10, 1, 4, 1, '4', 'ACTIVE', TRUE, 'Enrolled in Class I A', 'system', 'system', 0),
(11, 11, 1, 5, 3, '3', 'ACTIVE', TRUE, 'Enrolled in Class II A', 'system', 'system', 0),
(12, 12, 1, 5, 3, '4', 'ACTIVE', TRUE, 'Enrolled in Class II A', 'system', 'system', 0),
(13, 13, 1, 7, 5, '1', 'ACTIVE', TRUE, 'College student section A', 'system', 'system', 0),
(14, 14, 1, 7, 5, '2', 'ACTIVE', TRUE, 'College student section A', 'system', 'system', 0),
(15, 15, 1, 7, 5, '3', 'ACTIVE', TRUE, 'College student section A', 'system', 'system', 0),
(16, 16, 1, 7, 5, '4', 'ACTIVE', TRUE, 'College student section A', 'system', 'system', 0),
(17, 17, 1, 4, 2, '3', 'ACTIVE', TRUE, 'Class I B', 'system', 'system', 0),
(18, 18, 1, 4, 2, '4', 'ACTIVE', TRUE, 'Class I B', 'system', 'system', 0),
(19, 19, 1, 5, 4, '3', 'ACTIVE', TRUE, 'Class II B', 'system', 'system', 0),
(20, 20, 1, 7, 5, '5', 'ACTIVE', TRUE, 'College student section A', 'system', 'system', 0);

INSERT IGNORE INTO student_medical (
	medical_id, student_id, blood_group, allergies, medical_conditions, medications, doctor_name, doctor_contact,
	emergency_notes, active, created_by, updated_by, version
)
VALUES
(1, 1, 'B+', 'Pollen allergy', 'Mild seasonal cold sensitivity', 'Vitamin D syrup in winter', 'Dr. Anupama Das', '9777005101', 'Avoid dust during sports', TRUE, 'system', 'system', 0),
(2, 5, 'A+', 'Peanut allergy', 'None', 'None', 'Dr. R. K. Mishra', '9777005102', 'Carry epipen info in file', TRUE, 'system', 'system', 0),
(3, 9, 'O+', 'None', 'Mild asthma', 'Inhaler if needed', 'Dr. Priya Mohanty', '9777005103', 'Inform PE teacher on exertion', TRUE, 'system', 'system', 0),
(4, 13, 'AB+', 'None', 'Iron deficiency history', 'Iron supplement monthly', 'Dr. S. Das', '9777005104', 'Monitor diet and hydration', TRUE, 'system', 'system', 0);

INSERT IGNORE INTO student_parent (
	student_parent_id, student_id, parent_id, relationship, primary_contact, receive_sms, receive_email,
	pickup_authorized, active, created_by, updated_by, version
)
VALUES
(1, 1, 1, 'FATHER', TRUE, TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(2, 2, 1, 'FATHER', FALSE, TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(3, 9, 2, 'MOTHER', TRUE, TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(4, 13, 3, 'MOTHER', TRUE, TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO student_timeline (
	timeline_id, student_id, event_type, title, description, created_by, updated_by, version
)
VALUES
(1, 1, 'ENROLLMENT_CREATED', 'Joined Class I A', 'Aarav admitted to Javier School Bhubaneswar', 'system', 'system', 0),
(2, 1, 'PROMOTED', 'Promoted to Class II', 'Promotion planned for next year', 'system', 'system', 0),
(3, 9, 'ENROLLMENT_CREATED', 'Joined Class I A', 'Ishita admitted to ABC School Puri', 'system', 'system', 0),
(4, 13, 'ENROLLMENT_CREATED', 'Joined College', 'Ritika admitted to Kalinga College Cuttack', 'system', 'system', 0);

INSERT IGNORE INTO student_achievement (
	achievement_id, student_id, title, category, achievement_date, issuer, rank_position, description,
	certificate_document_id, active, created_by, updated_by, version
)
VALUES
(1, 1, 'Essay Competition Winner', 'ACADEMIC', '2026-06-15', 'Javier School Bhubaneswar', '1st', 'Won school-level essay competition', NULL, TRUE, 'system', 'system', 0),
(2, 9, 'Drawing Competition Runner Up', 'CULTURAL', '2026-06-18', 'ABC School Puri', '2nd', 'Secured second place in drawing contest', NULL, TRUE, 'system', 'system', 0),
(3, 13, 'College Quiz Finalist', 'ACADEMIC', '2026-06-19', 'Kalinga College Cuttack', 'Finalist', 'Reached final round in quiz competition', NULL, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO student_promotion (
	promotion_id, student_id, academic_year_id, from_class_id, to_class_id, from_section_id, to_section_id,
	promotion_date, remarks, created_by, updated_by, version
)
VALUES
(1, 1, 1, 4, 5, 1, 3, '2027-03-20', 'Planned yearly promotion', 'system', 'system', 0),
(2, 9, 1, 4, 5, 1, 3, '2027-03-20', 'Planned yearly promotion', 'system', 'system', 0);

INSERT IGNORE INTO transfer_request (
	id, organization_id, request_number, student_id, enrollment_id, requested_on, reason, destination_school,
	status, approved_by_user_id, approved_on, certificate_number, certificate_issued_on, remarks, created_by, updated_by, version
)
VALUES
(1, 1, 'TRF-2026-0001', 1, 1, '2026-06-25', 'Family relocating to Nayagarh', 'JNV Nayagarh', 'REQUESTED', NULL, NULL, NULL, NULL, 'Requested by parent', 'system', 'system', 0);

-- =========================================================
-- ATTENDANCE MODULE
-- =========================================================

INSERT IGNORE INTO attendance_setting (
	setting_id, organization_id, attendance_mode, late_after_time, window_start_time, window_end_time,
	allow_copy_previous, min_student_attendance_percent, student_alert_threshold_percent, send_sms_on_absent,
	send_email_on_absent, min_staff_working_hours, staff_late_grace_minutes, freeze_after_days, active,
	created_by, updated_by, version
)
VALUES
(1, 1, 'DAILY', '08:15:00', '08:00:00', '08:30:00', TRUE, 75, 80, TRUE, TRUE, 6, 15, 7, TRUE, 'system', 'system', 0),
(2, 2, 'DAILY', '08:20:00', '08:00:00', '08:30:00', TRUE, 75, 80, TRUE, TRUE, 6, 15, 7, TRUE, 'system', 'system', 0),
(3, 3, 'DAILY', '08:10:00', '08:00:00', '08:25:00', TRUE, 75, 80, TRUE, TRUE, 6, 15, 7, TRUE, 'system', 'system', 0),
(4, 4, 'DAILY', '09:00:00', '08:30:00', '09:15:00', TRUE, 80, 85, TRUE, TRUE, 6, 10, 7, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO student_attendance (
	attendance_id, organization_id, student_id, student_name, roll_number, admission_number, academic_year_id,
	class_id, class_name, section_id, section_name, attendance_date, status, remarks, marked_by, created_by,
	updated_by, version
)
VALUES
(1, 1, 1, 'Aarav Mohanty', '1', 'JSB-ADM-26001', 1, 4, 'Class I', 1, 'A', '2026-06-30', 'PRESENT', 'On time', 'Ananya Dash', 'system', 'system', 0),
(2, 1, 2, 'Ishani Mishra', '2', 'JSB-ADM-26002', 1, 4, 'Class I', 1, 'A', '2026-06-30', 'PRESENT', 'On time', 'Ananya Dash', 'system', 'system', 0),
(3, 1, 9, 'Ishita Das', '3', 'ABCP-ADM-26001', 1, 4, 'Class I', 1, 'A', '2026-06-30', 'ABSENT', 'Sick leave', 'Ananya Dash', 'system', 'system', 0),
(4, 2, 5, 'Arjun Pani', '1', 'JSC-ADM-26001', 1, 4, 'Class I', 2, 'B', '2026-06-30', 'LATE', 'Arrived late due to rain', 'Priya Patnaik', 'system', 'system', 0),
(5, 3, 13, 'Ritika Panda', '1', 'KCC-ADM-26001', 1, 7, 'Class XI Commerce', 5, 'A', '2026-06-30', 'PRESENT', 'Present for all classes', 'Priya Patnaik', 'system', 'system', 0);

INSERT IGNORE INTO staff_attendance (
	attendance_id, organization_id, staff_id, staff_name, staff_code, department, designation, attendance_date,
	sign_in_time, sign_out_time, working_minutes, shift, status, remarks, marked_by, created_by, updated_by, version
)
VALUES
(1, 1, 1, 'Rupesh Pati', 'JSB-TCHR-001', 'English', 'English Teacher', '2026-06-30', '2026-06-30 07:58:00', '2026-06-30 15:30:00', 452, 'Morning', 'PRESENT', 'On time', 'Ananya Dash', 'system', 'system', 0),
(2, 1, 2, 'Saswati Senapati', 'JSB-TCHR-002', 'Mathematics', 'Mathematics Teacher', '2026-06-30', '2026-06-30 08:06:00', '2026-06-30 15:25:00', 439, 'Morning', 'PRESENT', 'Regular attendance', 'Ananya Dash', 'system', 'system', 0),
(3, 2, 3, 'Suman Nayak', 'ABCP-TCHR-001', 'Science', 'Science Teacher', '2026-06-30', '2026-06-30 08:20:00', '2026-06-30 15:20:00', 420, 'Morning', 'LATE', 'Traffic delay near Cuttack', 'Priya Patnaik', 'system', 'system', 0),
(4, 3, 4, 'Madhuri Tripathy', 'KCC-TCHR-001', 'Commerce', 'Commerce Lecturer', '2026-06-30', '2026-06-30 08:00:00', '2026-06-30 15:40:00', 460, 'Morning', 'PRESENT', 'Present', 'Priya Patnaik', 'system', 'system', 0);

INSERT IGNORE INTO student_period_attendance (
	period_attendance_id, organization_id, student_id, student_name, roll_number, academic_year_id, class_id,
	class_name, section_id, section_name, period_id, period_number, period_name, subject_id, subject_name,
	attendance_date, status, remarks, created_by, updated_by, version
)
VALUES
(1, 1, 1, 'Aarav Mohanty', '1', 1, 4, 'Class I', 1, 'A', 1, 1, 'Period 1', 1, 'English', '2026-06-30', 'PRESENT', 'Active in class', 'system', 'system', 0),
(2, 1, 1, 'Aarav Mohanty', '1', 1, 4, 'Class I', 1, 'A', 2, 2, 'Period 2', 2, 'Mathematics', '2026-06-30', 'PRESENT', 'Answered questions', 'system', 'system', 0),
(3, 1, 2, 'Ishani Mishra', '2', 1, 4, 'Class I', 1, 'A', 1, 1, 'Period 1', 1, 'English', '2026-06-30', 'ABSENT', 'Absent due to fever', 'system', 'system', 0),
(4, 2, 5, 'Arjun Pani', '1', 1, 4, 'Class I', 2, 'B', 1, 1, 'Period 1', 1, 'English', '2026-06-30', 'LATE', 'Joined after bell', 'system', 'system', 0);

INSERT IGNORE INTO attendance_freeze (
	freeze_id, organization_id, freeze_from_date, freeze_to_date, reason, active, created_by, updated_by, version
)
VALUES
(1, 1, '2026-07-10', '2026-07-12', 'Term assessment freeze', TRUE, 'system', 'system', 0);

-- =========================================================
-- ADMISSION MODULE
-- =========================================================

INSERT IGNORE INTO inquiry (
	inquiry_id, organization_id, name, mobile_number, email, class_interested_in, address, inquiry_source,
	referred_by, comments, assigned_counselor_id, status, is_deleted, last_follow_up_date, last_follow_up_type,
	next_follow_up_date, created_by, updated_by, version
)
VALUES
(1, 1, 'Rahul Nayak', '9777111501', 'rahul@example.com', 'Class I', 'Patia, Bhubaneswar', 'Walk-In', NULL, 'Interested in morning batch', 3, 'INTERESTED', FALSE, '2026-06-28 11:00:00', 'CALL', '2026-07-02', 'system', 'system', 0),
(2, 2, 'Seema Sahu', '9777222501', 'seema@example.com', 'Class II', 'Madhupatna, Cuttack', 'Referral', 'Old parent', 'Wants transport facility', 9, 'CONTACTED', FALSE, '2026-06-29 10:15:00', 'WALK_IN', '2026-07-03', 'system', 'system', 0),
(3, 3, 'Niranjan Das', '9777334501', 'niranjan@example.com', 'Class I', 'Puri Town', 'Website', NULL, 'Looking for disciplined campus', 6, 'NEW', FALSE, NULL, NULL, '2026-07-04', 'system', 'system', 0),
(4, 4, 'Debasis Mohapatra', '9777445501', 'debasis@example.com', 'Degree 1st Year', 'Buxi Bazar, Cuttack', 'Phone', NULL, 'Interested in commerce stream', 6, 'INTERESTED', FALSE, '2026-06-27 09:30:00', 'OTHER', '2026-07-01', 'system', 'system', 0);

INSERT IGNORE INTO inquiry_follow_up (
	follow_up_id, inquiry_id, follow_up_type, remarks, status_after, follow_up_date, next_follow_up_date, created_by, updated_by, version
)
VALUES
(1, 1, 'CALL', 'Parent is positive about admission', 'INTERESTED', '2026-06-28 11:00:00', '2026-07-02', 'system', 'system', 0),
(2, 2, 'WALK_IN', 'Campus visit scheduled for Saturday', 'CONTACTED', '2026-06-29 10:15:00', '2026-07-03', 'system', 'system', 0),
(3, 4, 'OTHER', 'Discussed commerce stream options', 'INTERESTED', '2026-06-27 09:30:00', '2026-07-01', 'system', 'system', 0);

INSERT IGNORE INTO counseling_note (
	note_id, inquiry_id, student_requirements, parent_concerns, campus_visit_info, recommendations, notes,
	created_by, updated_by, version
)
VALUES
(1, 1, 'Strong English base, sports participation', 'Needs transport clarity', 'Visited Bhubaneswar campus', 'Offer transport and scholarship info', 'Very likely to convert', 'system', 'system', 0),
(2, 2, 'Good academic support', 'Wants safe pickup/drop', 'To be visited next week', 'Show transport and hostel options', 'Warm lead', 'system', 'system', 0);

INSERT IGNORE INTO application_admission (
	application_id, application_number, organization_id, inquiry_id, applicant_name, date_of_birth, gender,
	applying_for_class, email, contact_number, address, parent_name, parent_contact, parent_email, status,
	reviewed_by_user_id, reviewed_on, internal_comments, created_by, updated_by, version
)
VALUES
(1, 'APP-JSB-0001', 1, 1, 'Aarav Mohanty', '2019-05-18', 'Male', 'Class I', 'arav@example.com', '9777111301', 'Patia, Bhubaneswar', 'Ranjit Mohanty', '9777111401', 'parent1@jsb.edu.in', 'APPROVED', 3, '2026-06-30', 'Documents verified', 'system', 'system', 0),
(2, 'APP-ABC-0001', 3, 3, 'Ishita Das', '2019-01-14', 'Female', 'Class I', 'ishita@example.com', '9777333501', 'Puri Town', 'Madhumita Das', '9777334401', 'mother@abcpuri.edu.in', 'SUBMITTED', NULL, NULL, 'Awaiting document upload', 'system', 'system', 0),
(3, 'APP-KCC-0001', 4, 4, 'Ritika Panda', '2006-05-22', 'Female', 'Degree 1st Year', 'ritika@example.com', '9777444601', 'Cuttack', 'Sangeeta Sahoo', '9777445401', 'mother@kcc.edu.in', 'APPROVED', 6, '2026-06-29', 'Admission approved for commerce', 'system', 'system', 0);

-- =========================================================
-- COMMUNICATION MODULE
-- =========================================================

INSERT IGNORE INTO notice (
	notice_id, organization_id, title, content, category, pinned, publish_date, expiry_date, status,
	attachment_url, published_by_user_id, created_by, updated_by, version
)
VALUES
(1, 1, 'Odisha Foundation Day Celebration', 'The Bhubaneswar campus will celebrate Utkal Divas with cultural events.', 'EVENT', TRUE, '2026-04-01', '2026-04-05', 'PUBLISHED', NULL, 3, 'system', 'system', 0),
(2, 2, 'Rainy Season Advisory', 'Parents are advised to send umbrellas and ensure students carry raincoats.', 'ADVISORY', FALSE, '2026-06-20', '2026-07-10', 'PUBLISHED', NULL, 9, 'system', 'system', 0),
(3, 4, 'College Orientation Week', 'Orientation schedule for new first-year students at Kalinga College Cuttack.', 'ACADEMIC', TRUE, '2026-06-25', '2026-07-05', 'PUBLISHED', NULL, 12, 'system', 'system', 0);

INSERT IGNORE INTO notice_audience (audience_id, notice_id, audience_type, ref_id, created_by, updated_by, version)
VALUES
(1, 1, 'STAFF', NULL, 'system', 'system', 0),
(2, 1, 'STUDENTS', NULL, 'system', 'system', 0),
(3, 2, 'PARENTS', NULL, 'system', 'system', 0),
(4, 3, 'INDIVIDUAL', 12, 'system', 'system', 0);

INSERT IGNORE INTO notification (
	notification_id, organization_id, subject, body, channels_csv, category, scheduled_at, sent_at, status,
	triggered_by_user_id, total_recipients, delivered_count, failed_count, created_by, updated_by, version
)
VALUES
(1, 1, 'Attendance Alert', 'Aarav was marked present today.', 'SMS,EMAIL,IN_APP', 'ATTENDANCE', '2026-06-30 08:35:00', '2026-06-30 08:35:10', 'SENT', 3, 2, 2, 0, 'system', 'system', 0),
(2, 2, 'Admission Follow-up', 'Please visit the Cuttack campus with your documents.', 'SMS,EMAIL', 'ADMISSION', '2026-06-30 10:00:00', NULL, 'PENDING', 9, 1, 0, 0, 'system', 'system', 0);

INSERT IGNORE INTO notification_recipient (
	recipient_id, notification_id, user_id, address, channel, status, sent_at, delivered_at, read_at, failure_reason,
	attempt_count, created_by, updated_by, version
)
VALUES
(1, 1, 6, 'student1@jsb.edu.in', 'EMAIL', 'DELIVERED', '2026-06-30 08:35:10', '2026-06-30 08:35:20', '2026-06-30 08:36:00', NULL, 1, 'system', 'system', 0),
(2, 1, 7, 'parent1@jsb.edu.in', 'SMS', 'DELIVERED', '2026-06-30 08:35:10', '2026-06-30 08:35:18', NULL, NULL, 1, 'system', 'system', 0),
(3, 2, 11, 'student1@abcpuri.edu.in', 'EMAIL', 'PENDING', NULL, NULL, NULL, NULL, 0, 'system', 'system', 0);

INSERT IGNORE INTO message_thread (
	thread_id, organization_id, subject, participant_user_ids_csv, last_message_at, closed, created_by, updated_by, version
)
VALUES
(1, 1, 'Class I Parents', '3,6,7', '2026-06-30 09:15:00', FALSE, 'system', 'system', 0),
(2, 2, 'Admission Desk', '9,11', '2026-06-30 09:30:00', FALSE, 'system', 'system', 0);

INSERT IGNORE INTO message (
	message_id, message_thread_id, sender_user_id, body, attachment_url, sent_at, deleted, created_by, updated_by, version
)
VALUES
(1, 1, 3, 'Good morning parents. Please ensure your children carry water bottles.', NULL, '2026-06-30 09:10:00', FALSE, 'system', 'system', 0),
(2, 1, 7, 'Noted, thank you.', NULL, '2026-06-30 09:12:00', FALSE, 'system', 'system', 0),
(3, 2, 9, 'Please bring original certificates tomorrow.', NULL, '2026-06-30 09:30:00', FALSE, 'system', 'system', 0);

-- =========================================================
-- DOCUMENT MODULE
-- =========================================================

INSERT IGNORE INTO document (
	document_id, owner_type, owner_id, document_type, document_name, file_name, file_path, file_extension,
	file_size, mime_type, remarks, active, created_by, updated_by, version
)
VALUES
(1, 'STUDENT', 1, 'PHOTO', 'Aarav Profile Photo', 'aarav.png', '/uploads/students/aarav.png', 'png', 245678, 'image/png', 'Student photo', TRUE, 'system', 'system', 0),
(2, 'STUDENT', 9, 'PHOTO', 'Ishita Profile Photo', 'ishita.png', '/uploads/students/ishita.png', 'png', 236411, 'image/png', 'Student photo', TRUE, 'system', 'system', 0),
(3, 'STAFF', 1, 'PHOTO', 'Rupesh Profile Photo', 'rupesh.png', '/uploads/staff/rupesh.png', 'png', 198765, 'image/png', 'Staff photo', TRUE, 'system', 'system', 0),
(4, 'PARENT', 1, 'IDENTITY', 'Ranjit Aadhaar', 'ranjit-aadhaar.pdf', '/uploads/parents/ranjit-aadhaar.pdf', 'pdf', 512340, 'application/pdf', 'Parent identity document', TRUE, 'system', 'system', 0),
(5, 'ORGANIZATION', 1, 'LOGO', 'JSB Logo', 'jsb-logo.png', '/uploads/org/jsb-logo.png', 'png', 145678, 'image/png', 'Campus logo', TRUE, 'system', 'system', 0);

-- =========================================================
-- AUDIT MODULE
-- =========================================================

INSERT IGNORE INTO audit_log (
	id, organization_id, tenant_code, correlation_id, event_type, action, entity_type, entity_id, actor_user_id,
	actor_username, source_ip, user_agent, changes, summary, occurred_at, created_by, updated_by, version
)
VALUES
(1, 1, 'jsb-bhubaneswar', 'corr-001', 'CREATE', 'Create Student', 'Student', '1', 3, 'javier.admin', '127.0.0.1', 'Chrome', '{"studentCode":"JSB-STU-0001"}', 'Created Aarav Mohanty', '2026-06-30 08:00:00', 'system', 'system', 0),
(2, 2, 'jsc-cuttack', 'corr-002', 'UPDATE', 'Mark Attendance', 'StudentAttendance', '4', 9, 'abc.admin', '127.0.0.1', 'Edge', '{"status":"LATE"}', 'Marked Arjun Pani late', '2026-06-30 08:20:00', 'system', 'system', 0),
(3, 4, 'kcc-cuttack', 'corr-003', 'LOGIN', 'Login', 'User', '12', 12, 'kalinga.owner', '127.0.0.1', 'Chrome', NULL, 'Owner login', '2026-06-30 08:30:00', 'system', 'system', 0);

INSERT IGNORE INTO security_audit_log (
	id, event_code, username, tenant_code, source_ip, user_agent, success, severity, message, correlation_id,
	occurred_at, created_by, updated_by, version
)
VALUES
(1, 'LOGIN_SUCCESS', 'superadmin', 'jsb-bhubaneswar', '127.0.0.1', 'Chrome', TRUE, 'INFO', 'Successful login', 'corr-100', '2026-06-30 08:30:00', 'system', 'system', 0),
(2, 'LOGIN_FAILED', 'student1', 'jsb-bhubaneswar', '127.0.0.1', 'Chrome', FALSE, 'MEDIUM', 'Invalid password', 'corr-101', '2026-06-30 08:32:00', 'system', 'system', 0);

-- =========================================================
-- SHARED MODULE
-- =========================================================

INSERT IGNORE INTO address (
	address_id, address_line1, address_line2, landmark, city, district, state, country, postal_code, active,
	created_by, updated_by, version
)
VALUES
(1, 'Plot 12, Patia', 'Near Infocity', 'Infocity Square', 'Bhubaneswar', 'Khordha', 'Odisha', 'India', '751024', TRUE, 'system', 'system', 0),
(2, 'Plot 7, Sector 5', 'Near New Bus Stand', 'Bus Stand', 'Cuttack', 'Cuttack', 'Odisha', 'India', '753014', TRUE, 'system', 'system', 0),
(3, 'Marine Drive Road', 'Sipasurubali', 'Sea Beach Road', 'Puri', 'Puri', 'Odisha', 'India', '752001', TRUE, 'system', 'system', 0),
(4, 'College Road', 'Badambadi', 'Badambadi Square', 'Cuttack', 'Cuttack', 'Odisha', 'India', '753012', TRUE, 'system', 'system', 0);

-- =========================================================
-- PLATFORM / OPTIONAL MANAGEMENT MODULES
-- =========================================================

INSERT IGNORE INTO promotions (
	id, promotion_code, promotion_name, description, discount_type, discount_value, maximum_discount,
	valid_from, valid_to, maximum_usage, used_count, allow_custom_plan, stackable, auto_apply, status, active,
	remarks, created_by, updated_by, version
)
VALUES
(1, 'SUMMER2026', 'Summer Enrollment Offer', '10% off yearly plans for new campuses', 'PERCENTAGE', 10.00, 15000.00,
 '2026-04-01', '2026-08-31', 100, 12, FALSE, FALSE, TRUE, 'ACTIVE', TRUE, 'Seasonal campaign', 'system', 'system', 0),
(2, 'ODISHA_LAUNCH', 'Odisha Launch Discount', 'Flat discount for Odisha pilot schools', 'FLAT_AMOUNT', 5000.00, 5000.00,
 '2026-01-01', '2026-12-31', 50, 3, FALSE, TRUE, FALSE, 'ACTIVE', TRUE, 'Regional launch offer', 'system', 'system', 0);

INSERT IGNORE INTO maintenance_schedules (
	id, organization_id, title, description, reason, start_time, end_time, planned, notification_sent, completed,
	created_by, updated_by, version
)
VALUES
(1, NULL, 'June Database Check', 'Short platform maintenance window', 'Routine database validation', '2026-06-30 23:00:00', '2026-07-01 00:00:00', TRUE, FALSE, FALSE, 'system', 'system', 0);

-- Ensure platform super-admin role is assigned (idempotent for existing dev databases)
UPDATE user_roles ur
INNER JOIN users u ON u.id = ur.user_id
SET ur.role_id = 6
WHERE u.username = 'superadmin' AND ur.primary_role = TRUE;

UPDATE promotions SET discount_type = 'FLAT_AMOUNT' WHERE discount_type = 'FLAT';

-- =============================================================================
-- PHASE 2: COMPREHENSIVE MULTI-ORG ENHANCEMENT
-- Fixes data bugs + adds rich Odisha-specific demo data for 4 organizations
-- Run: idempotent INSERT IGNORE + targeted UPDATE statements
-- =============================================================================

-- =========================================================
-- STEP 1: FIX USER ORGANIZATION ASSIGNMENTS (CRITICAL BUG FIX)
-- Users 8-11 belong to ABC School Puri (org 3), not JSC (org 2)
-- Users 12-15 belong to Kalinga College Cuttack (org 4), not ABC (org 3)
-- =========================================================
UPDATE users SET organization_id = 3 WHERE id IN (8, 9, 10, 11);
UPDATE users SET organization_id = 4 WHERE id IN (12, 13, 14, 15);

-- =========================================================
-- STEP 2: FIX ATTENDANCE ORGANIZATION IDs
-- =========================================================
UPDATE student_attendance SET organization_id = 3 WHERE student_id = 9 AND organization_id = 1;
UPDATE student_attendance SET organization_id = 4 WHERE student_id = 13 AND organization_id = 3;
UPDATE staff_attendance   SET organization_id = 3 WHERE staff_id = 3  AND organization_id = 2;
UPDATE staff_attendance   SET organization_id = 4 WHERE staff_id = 4  AND organization_id = 3;

-- =========================================================
-- STEP 3: FIX STUDENT ENROLLMENT SECTION ASSIGNMENTS
-- Students 7,8 (JSC Class II) were wrongly placed in Section 4 (Class III A)
-- Add Class II Section B first, then fix
-- =========================================================
INSERT IGNORE INTO academic_section (section_id, class_id, section_name, capacity, active, remarks, created_by, updated_by, version)
VALUES (6, 5, 'B', 40, TRUE, 'Class II Section B', 'system', 'system', 0);

UPDATE student_enrollment SET section_id = 6 WHERE enrollment_id IN (7, 8) AND section_id = 4;
-- Fix student 19 (Tushar, ABC Class II) also wrongly in section 4
UPDATE student_enrollment SET section_id = 3 WHERE enrollment_id = 19 AND section_id = 4;

-- =========================================================
-- STEP 4: MORE ACADEMIC CLASSES (IV through XII)
-- =========================================================
INSERT IGNORE INTO academic_class (class_id, academic_year_id, class_code, class_name, academic_stage, display_order, active, remarks, created_by, updated_by, version)
VALUES
(8,  1, 'IV',      'Class IV',             'PRIMARY',           8,  TRUE, 'Primary class',              'system', 'system', 0),
(9,  1, 'V',       'Class V',              'PRIMARY',           9,  TRUE, 'Upper primary',              'system', 'system', 0),
(10, 1, 'VI',      'Class VI',             'MIDDLE',            10, TRUE, 'Middle school',              'system', 'system', 0),
(11, 1, 'VII',     'Class VII',            'MIDDLE',            11, TRUE, 'Middle school',              'system', 'system', 0),
(12, 1, 'VIII',    'Class VIII',           'MIDDLE',            12, TRUE, 'Middle school',              'system', 'system', 0),
(13, 1, 'IX',      'Class IX',             'SECONDARY',         13, TRUE, 'High school',                'system', 'system', 0),
(14, 1, 'X',       'Class X',              'SECONDARY',         14, TRUE, 'Board class',                'system', 'system', 0),
(15, 1, 'XI-SCI',  'Class XI Science',     'SENIOR_SECONDARY',  15, TRUE, 'Senior secondary science',   'system', 'system', 0),
(16, 1, 'XII-COM', 'Class XII Commerce',   'SENIOR_SECONDARY',  16, TRUE, 'Final year commerce',        'system', 'system', 0),
(17, 1, 'XII-SCI', 'Class XII Science',    'SENIOR_SECONDARY',  17, TRUE, 'Final year science',         'system', 'system', 0);

-- =========================================================
-- STEP 5: MORE SECTIONS (for Classes IV-X, XII)
-- =========================================================
INSERT IGNORE INTO academic_section (section_id, class_id, section_name, capacity, active, remarks, created_by, updated_by, version)
VALUES
(7,  8,  'A', 40, TRUE, 'Class IV Section A',          'system', 'system', 0),
(8,  9,  'A', 40, TRUE, 'Class V Section A',           'system', 'system', 0),
(9,  10, 'A', 45, TRUE, 'Class VI Section A',          'system', 'system', 0),
(10, 11, 'A', 45, TRUE, 'Class VII Section A',         'system', 'system', 0),
(11, 12, 'A', 45, TRUE, 'Class VIII Section A',        'system', 'system', 0),
(12, 13, 'A', 50, TRUE, 'Class IX Section A',          'system', 'system', 0),
(13, 14, 'A', 50, TRUE, 'Class X Section A',           'system', 'system', 0),
(14, 15, 'A', 60, TRUE, 'Class XI Science Section A',  'system', 'system', 0),
(15, 16, 'A', 60, TRUE, 'Class XII Commerce Section A','system', 'system', 0),
(16, 3,  'A', 35, TRUE, 'UKG Section A',               'system', 'system', 0),
(17, 2,  'A', 35, TRUE, 'LKG Section A',               'system', 'system', 0),
(18, 1,  'A', 30, TRUE, 'Nursery Section A',           'system', 'system', 0);

-- =========================================================
-- STEP 6: MORE SUBJECTS
-- =========================================================
INSERT IGNORE INTO subject (subject_id, subject_code, subject_name, subject_type, active, remarks, created_by, updated_by, version)
VALUES
(11, 'PHY', 'Physics',               'CORE',     TRUE, 'Science stream', 'system', 'system', 0),
(12, 'CHM', 'Chemistry',             'CORE',     TRUE, 'Science stream', 'system', 'system', 0),
(13, 'BIO', 'Biology',               'CORE',     TRUE, 'Science stream', 'system', 'system', 0),
(14, 'ECO', 'Economics',             'CORE',     TRUE, 'Commerce stream','system', 'system', 0),
(15, 'EVS', 'Environmental Science', 'CORE',     TRUE, 'Primary EVS',   'system', 'system', 0),
(16, 'ART', 'Art & Craft',           'SKILL',    TRUE, 'Creative arts',  'system', 'system', 0),
(17, 'SPT', 'Sports & PE',           'SKILL',    TRUE, 'Physical education','system','system', 0),
(18, 'MOR', 'Moral Science',         'SKILL',    TRUE, 'Value education','system', 'system', 0),
(19, 'SNS', 'Sanskrit',              'LANGUAGE', TRUE, 'Optional lang',  'system', 'system', 0),
(20, 'GEO', 'Geography',             'CORE',     TRUE, 'Social science', 'system', 'system', 0);

-- =========================================================
-- STEP 7: JSC USERS (Javier School Cuttack, org_id = 2)
-- =========================================================
INSERT IGNORE INTO users (
    id, organization_id, user_code, username, email, mobile_number, password,
    first_name, last_name, display_name, profile_image_url, status,
    email_verified, mobile_verified, first_time_login, failed_login_attempts, account_locked,
    last_login_at, password_changed_at, locked_at, lock_expiry_at, created_by, updated_by, version)
VALUES
(16, 2, 'USR000016', 'jsc.owner',    'owner@jsc.edu.in',    '9777222201', 'PLACEHOLDER', 'Bijaya',    'Patnaik',   'Bijaya Patnaik',   NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(17, 2, 'USR000017', 'jsc.admin',    'admin@jsc.edu.in',    '9777222202', 'PLACEHOLDER', 'Sulochana', 'Babu',      'Sulochana Babu',   NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(18, 2, 'USR000018', 'jsc.teacher1', 'teacher1@jsc.edu.in', '9777222401', 'PLACEHOLDER', 'Debendra',  'Barik',     'Debendra Barik',   NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(19, 2, 'USR000019', 'jsc.teacher2', 'teacher2@jsc.edu.in', '9777222402', 'PLACEHOLDER', 'Geetanjali','Rath',      'Geetanjali Rath',  NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(20, 2, 'USR000020', 'jsc.student1', 'student1@jsc.edu.in', '9777222501', 'PLACEHOLDER', 'Bikash',    'Nanda',     'Bikash Nanda',     NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0),
(21, 2, 'USR000021', 'jsc.parent1',  'parent1@jsc.edu.in',  '9777222601', 'PLACEHOLDER', 'Prasanta',  'Nanda',     'Prasanta Nanda',   NULL, 'ACTIVE', TRUE, TRUE, FALSE, 0, FALSE, NULL, NULL, NULL, NULL, 'system', 'system', 0);

INSERT IGNORE INTO user_roles (id, user_id, role_id, primary_role, active, created_by, updated_by, version)
VALUES
(16, 16, 1, TRUE, TRUE, 'system', 'system', 0),
(17, 17, 2, TRUE, TRUE, 'system', 'system', 0),
(18, 18, 3, TRUE, TRUE, 'system', 'system', 0),
(19, 19, 3, TRUE, TRUE, 'system', 'system', 0),
(20, 20, 4, TRUE, TRUE, 'system', 'system', 0),
(21, 21, 5, TRUE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO security_policies (
    id, organization_id, min_password_length, require_uppercase, require_lowercase, require_numbers,
    require_special_chars, password_expiry_days, password_history_count, max_failed_attempts,
    lockout_duration_minutes, session_timeout_minutes, max_concurrent_sessions, allow_remember_me,
    require_two_factor, active, created_by, updated_by, version)
VALUES
(5, 2, 8, TRUE, TRUE, TRUE, FALSE, 90, 5, 5, 30, 60, 3, TRUE, FALSE, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
-- Org 2 (JSC) - OWNER full access
(9,  2, 1, 1, TRUE, TRUE, TRUE, 'system', 'system', 0),
(10, 2, 1, 3, TRUE, TRUE, TRUE, 'system', 'system', 0),
(11, 2, 1, 4, TRUE, TRUE, TRUE, 'system', 'system', 0),
(12, 2, 1, 5, TRUE, TRUE, TRUE, 'system', 'system', 0),
(13, 2, 1, 6, TRUE, TRUE, TRUE, 'system', 'system', 0),
(14, 2, 1, 7, TRUE, TRUE, TRUE, 'system', 'system', 0),
-- Org 2 (JSC) - ADMIN manage
(15, 2, 2, 1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(16, 2, 2, 3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(17, 2, 2, 4, TRUE, TRUE, FALSE, 'system', 'system', 0),
(18, 2, 2, 5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(19, 2, 2, 6, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Org 3 (ABC) - OWNER full access
(20, 3, 1, 1, TRUE, TRUE, TRUE, 'system', 'system', 0),
(21, 3, 1, 3, TRUE, TRUE, TRUE, 'system', 'system', 0),
(22, 3, 1, 5, TRUE, TRUE, TRUE, 'system', 'system', 0),
(23, 3, 1, 6, TRUE, TRUE, TRUE, 'system', 'system', 0),
-- Org 3 (ABC) - ADMIN
(24, 3, 2, 1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(25, 3, 2, 3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(26, 3, 2, 5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(27, 3, 2, 6, TRUE, TRUE, FALSE, 'system', 'system', 0),
-- Org 4 (Kalinga) - OWNER
(28, 4, 1, 1, TRUE, TRUE, TRUE, 'system', 'system', 0),
(29, 4, 1, 3, TRUE, TRUE, TRUE, 'system', 'system', 0),
(30, 4, 1, 5, TRUE, TRUE, TRUE, 'system', 'system', 0),
(31, 4, 1, 6, TRUE, TRUE, TRUE, 'system', 'system', 0),
-- Org 4 (Kalinga) - ADMIN
(32, 4, 2, 1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(33, 4, 2, 3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(34, 4, 2, 5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(35, 4, 2, 6, TRUE, TRUE, FALSE, 'system', 'system', 0);

-- =========================================================
-- STEP 8: JSC STAFF (Javier School Cuttack)
-- =========================================================
INSERT IGNORE INTO staff (
    staff_id, user_id, staff_code, first_name, middle_name, last_name, gender, date_of_birth,
    blood_group, religion, nationality, mobile_number, email, staff_type, designation,
    employment_category, employment_status, joining_date, contract_start_date, contract_end_date,
    highest_qualification, experience_years, emergency_contact_name, emergency_contact_relation,
    emergency_contact_number, photo_url, active, created_by, updated_by, version)
VALUES
(7, 18, 'JSC-TCHR-001', 'Debendra',  NULL, 'Barik',  'Male',   '1991-04-12', 'O+', 'Hindu', 'Indian', '9777222401', 'teacher1@jsc.edu.in', 'TEACHING',     'English Teacher',      'PERMANENT', 'ACTIVE', '2022-07-01', NULL, NULL, 'M.A. English, B.Ed.',       7, 'Sushil Barik',   'Father',  '9777000601', NULL, TRUE, 'system', 'system', 0),
(8, 19, 'JSC-TCHR-002', 'Geetanjali',NULL, 'Rath',   'Female', '1993-08-20', 'A+', 'Hindu', 'Indian', '9777222402', 'teacher2@jsc.edu.in', 'TEACHING',     'Mathematics Teacher',  'PERMANENT', 'ACTIVE', '2023-05-10', NULL, NULL, 'M.Sc. Mathematics, B.Ed.',  5, 'Pradeep Rath',   'Husband', '9777000602', NULL, TRUE, 'system', 'system', 0),
(9, 17, 'JSC-ADM-001',  'Sulochana', NULL, 'Babu',   'Female', '1986-02-14', 'B+', 'Hindu', 'Indian', '9777222202', 'admin@jsc.edu.in',    'NON_TEACHING', 'Office Administrator', 'PERMANENT', 'ACTIVE', '2021-04-01', NULL, NULL, 'B.Com, MBA',               10, 'Ramesh Babu',    'Brother', '9777000603', NULL, TRUE, 'system', 'system', 0),
(10,13, 'KCC-ADM-001',  'Sourav',    NULL, 'Swain',  'Male',   '1985-06-18', 'O+', 'Hindu', 'Indian', '9777444402', 'admin@kcc.edu.in',    'NON_TEACHING', 'College Administrator','PERMANENT', 'ACTIVE', '2019-09-01', NULL, NULL, 'MBA, B.Com',               12, 'Bijay Swain',    'Brother', '9777000701', NULL, TRUE, 'system', 'system', 0),
(11, 8, 'ABCP-ADM-002', 'Dr. Madhumita',NULL,'Das',  'Female', '1975-03-22', 'A+', 'Hindu', 'Indian', '9777333301', 'owner@abcpuri.edu.in','NON_TEACHING', 'Managing Trustee',     'PERMANENT', 'ACTIVE', '2018-06-01', NULL, NULL, 'Ph.D. Education',          18, 'Ramesh Das',     'Husband', '9777000801', NULL, TRUE, 'system', 'system', 0);

INSERT IGNORE INTO staff_salary_structure (
    salary_structure_id, staff_id, salary_type, basic_pay, hra, da, special_allowance,
    transport_allowance, other_allowance, gross_salary, bank_name, account_holder_name,
    account_number, ifsc_code, effective_from, effective_to, active, remarks, created_by, updated_by, version)
VALUES
(5, 7,  'MONTHLY', 27000.00, 7500.00, 2800.00, 1900.00, 1200.00, 500.00, 40900.00, 'Bank of India Cuttack',  'Debendra Barik',    '3211002CUTK01', 'BKID0JSC001', '2026-04-01', NULL, TRUE, 'JSC English teacher salary',  'system', 'system', 0),
(6, 8,  'MONTHLY', 29000.00, 8000.00, 3000.00, 2100.00, 1200.00, 500.00, 43800.00, 'SBI Cuttack Main',       'Geetanjali Rath',   '3211002CUTK02', 'SBIN0CUTAK', '2026-04-01', NULL, TRUE, 'JSC Math teacher salary',     'system', 'system', 0),
(7, 9,  'MONTHLY', 25000.00, 6500.00, 2400.00, 1600.00, 1000.00, 500.00, 37000.00, 'Axis Bank Cuttack',      'Sulochana Babu',    '3211002CUTK03', 'UTIB0CUTAK', '2026-04-01', NULL, TRUE, 'JSC admin salary',            'system', 'system', 0),
(8, 10, 'MONTHLY', 32000.00, 9000.00, 3400.00, 2400.00, 1500.00, 700.00, 49000.00, 'Canara Bank Cuttack',    'Sourav Swain',      '3211002CUTK04', 'CNRB0KCC01', '2026-04-01', NULL, TRUE, 'Kalinga college admin salary','system', 'system', 0),
(9, 4,  'MONTHLY', 38000.00,11000.00, 4000.00, 3000.00, 1500.00, 800.00, 58300.00, 'UCO Bank Cuttack',       'Madhuri Tripathy',  '3211002CUTK05', 'UCBA0KCC01', '2026-04-01', NULL, TRUE, 'Kalinga commerce salary',     'system', 'system', 0);

INSERT IGNORE INTO payroll (
    payroll_id, staff_id, payroll_year, payroll_month, working_days, present_days, leave_without_pay_days,
    gross_salary, total_deductions, net_salary, generated_on, paid_on, status, remarks, created_by, updated_by, version)
VALUES
(5, 7,  2026, 6, 26, 26, 0, 40900.00, 1100.00, 39800.00, '2026-06-25', '2026-06-30', 'PAID', 'JSC teacher June payroll',          'system', 'system', 0),
(6, 8,  2026, 6, 26, 25, 1, 43800.00, 1600.00, 42200.00, '2026-06-25', '2026-06-30', 'PAID', 'JSC math teacher June payroll',     'system', 'system', 0),
(7, 9,  2026, 6, 26, 26, 0, 37000.00,  900.00, 36100.00, '2026-06-25', '2026-06-30', 'PAID', 'JSC admin June payroll',            'system', 'system', 0),
(8, 3,  2026, 6, 26, 26, 0, 45000.00, 1200.00, 43800.00, '2026-06-25', '2026-06-30', 'PAID', 'ABC science teacher June payroll',  'system', 'system', 0),
(9, 4,  2026, 6, 26, 25, 1, 58300.00, 2000.00, 56300.00, '2026-06-25', '2026-06-30', 'PAID', 'KCC commerce teacher June payroll', 'system', 'system', 0);

INSERT IGNORE INTO responsibility_assignment (assignment_id, staff_id, responsibility_id, scope, effective_from, effective_to, active, remarks, created_by, updated_by, version)
VALUES
(5, 7, 1, 'Class I B (JSC)',              '2026-04-01', NULL, TRUE, 'JSC Class I B teacher',     'system', 'system', 0),
(6, 8, 2, 'Mathematics Department (JSC)', '2026-04-01', NULL, TRUE, 'JSC Math department head',  'system', 'system', 0),
(7, 9, 3, 'Javier Cuttack',              '2026-04-01', NULL, TRUE, 'JSC exam coordinator',      'system', 'system', 0),
(8,10, 3, 'Kalinga College',             '2026-04-01', NULL, TRUE, 'KCC exam coordinator',      'system', 'system', 0);

-- =========================================================
-- STEP 9: CLASS TEACHER ASSIGNMENTS
-- =========================================================
INSERT IGNORE INTO class_teacher_assignment (
    assignment_id, academic_year_id, class_id, section_id, teacher_id,
    effective_from, effective_to, active, remarks, created_by, updated_by, version)
VALUES
(1, 1, 4, 1, 4,  '2026-04-01', NULL, TRUE, 'JSB Class I-A class teacher (Rupesh)',     'system', 'system', 0),
(2, 1, 4, 2, 18, '2026-04-01', NULL, TRUE, 'JSC Class I-B class teacher (Debendra)',   'system', 'system', 0),
(3, 1, 5, 3, 5,  '2026-04-01', NULL, TRUE, 'Class II-A class teacher (Saswati)',       'system', 'system', 0),
(4, 1, 5, 6, 19, '2026-04-01', NULL, TRUE, 'JSC Class II-B class teacher (Geetanjali)','system', 'system', 0),
(5, 1, 7, 5, 14, '2026-04-01', NULL, TRUE, 'XI Commerce class teacher (Madhuri)',      'system', 'system', 0);

-- =========================================================
-- STEP 10: MORE SUBJECT ASSIGNMENTS (JSC teachers, ABC)
-- =========================================================
INSERT IGNORE INTO subject_assignment (
    subject_assignment_id, academic_year_id, class_id, section_id, subject_id,
    teacher_id, periods_per_week, active, remarks, created_by, updated_by, version)
VALUES
(8,  1, 4, 2, 1,  18, 5, TRUE, 'English for Class I B (JSC)',           'system', 'system', 0),
(9,  1, 4, 2, 2,  19, 5, TRUE, 'Math for Class I B (JSC)',              'system', 'system', 0),
(10, 1, 4, 2, 5,  18, 3, TRUE, 'Odia for Class I B (JSC)',              'system', 'system', 0),
(11, 1, 5, 6, 1,  18, 5, TRUE, 'English for Class II B (JSC)',          'system', 'system', 0),
(12, 1, 5, 6, 2,  19, 5, TRUE, 'Math for Class II B (JSC)',             'system', 'system', 0),
(13, 1, 5, 3, 3,  10, 4, TRUE, 'Science for Class II A (ABC teacher)',  'system', 'system', 0),
(14, 1, 7, 5, 14, 14, 3, TRUE, 'Economics for Class XI Commerce',       'system', 'system', 0);

-- Timetable slots for JSC
INSERT IGNORE INTO timetable_slot (slot_id, academic_year_id, class_id, section_id, subject_assignment_id, period_template_id, day_of_week, active, created_by, updated_by, version)
VALUES
(8,  1, 4, 2, 8,  1, 'MONDAY',    TRUE, 'system', 'system', 0),
(9,  1, 4, 2, 9,  2, 'MONDAY',    TRUE, 'system', 'system', 0),
(10, 1, 4, 2, 10, 4, 'MONDAY',    TRUE, 'system', 'system', 0),
(11, 1, 5, 6, 11, 1, 'TUESDAY',   TRUE, 'system', 'system', 0),
(12, 1, 5, 6, 12, 2, 'WEDNESDAY', TRUE, 'system', 'system', 0),
(13, 1, 5, 3, 13, 1, 'THURSDAY',  TRUE, 'system', 'system', 0);

-- =========================================================
-- STEP 11: ADDITIONAL PARENTS (for new students)
-- =========================================================
INSERT IGNORE INTO parent (
    parent_id, parent_code, first_name, middle_name, last_name, gender, mobile_number,
    alternate_mobile, email, occupation, organization_name, qualification, annual_income,
    photo_url, active, remarks, user_id, created_by, updated_by, version)
VALUES
(4,  'PAR000004', 'Prasanta',     NULL, 'Nanda',   'Male',   '9777222601', NULL,         'parent1@jsc.edu.in',     'Government Employee',   'Odisha Secretariat',         'Post Graduate',  780000.00, NULL, TRUE, 'Father of Bikash (JSC)',  21,   'system', 'system', 0),
(5,  'PAR000005', 'Bimalendu',    NULL, 'Sahoo',   'Male',   '9777111402', NULL,         'parent2@jsb.edu.in',     'Private Business',      'Sahoo Enterprises',          'Graduate',      1100000.00, NULL, TRUE, 'Father of Ishani',        NULL, 'system', 'system', 0),
(6,  'PAR000006', 'Sarojini',     NULL, 'Behera',  'Female', '9777111403', NULL,         'parent3@jsb.edu.in',     'Homemaker',             NULL,                         'Intermediate',       0.00, NULL, TRUE, 'Mother of Aditya',        NULL, 'system', 'system', 0),
(7,  'PAR000007', 'Rajendra',     NULL, 'Nayak',   'Male',   '9777111404', NULL,         'parent4@jsb.edu.in',     'Bank Employee',         'UCO Bank Bhubaneswar',       'Post Graduate',  950000.00, NULL, TRUE, 'Father of Tanvi',         NULL, 'system', 'system', 0),
(8,  'PAR000008', 'Sumanta',      NULL, 'Pani',    'Male',   '9777222502', NULL,         'parent5@jsc.edu.in',     'School Teacher',        'Govt. HS Cuttack',           'Post Graduate',  720000.00, NULL, TRUE, 'Father of Arjun (JSC)',   NULL, 'system', 'system', 0),
(9,  'PAR000009', 'Laxmipriya',   NULL, 'Swain',   'Female', '9777222503', NULL,         'parent6@jsc.edu.in',     'Nurse',                 'SCB Medical College',        'Diploma',        680000.00, NULL, TRUE, 'Mother of Manya (JSC)',   NULL, 'system', 'system', 0),
(10, 'PAR000010', 'Narayan',      NULL, 'Das',     'Male',   '9777222504', NULL,         'parent7@jsc.edu.in',     'Lawyer',                'Orissa High Court',          'Post Graduate', 1800000.00, NULL, TRUE, 'Father of Kunal (JSC)',   NULL, 'system', 'system', 0),
(11, 'PAR000011', 'Hemanta',      NULL, 'Mohanty', 'Male',   '9777222505', NULL,         'parent8@jsc.edu.in',     'Police Officer',        'Odisha Police',              'Graduate',       750000.00, NULL, TRUE, 'Father of Riya (JSC)',    NULL, 'system', 'system', 0),
(12, 'PAR000012', 'Rajashree',    NULL, 'Panda',   'Female', '9777334402', NULL,         'parent9@abcpuri.edu.in', 'School Teacher',        'Govt. UP School Puri',       'Post Graduate',  700000.00, NULL, TRUE, 'Mother of Sai (ABC)',     NULL, 'system', 'system', 0),
(13, 'PAR000013', 'Duryodhan',    NULL, 'Jena',    'Male',   '9777334403', NULL,         'parent10@abcpuri.edu.in','Fisherman',             NULL,                         'Intermediate',   420000.00, NULL, TRUE, 'Father of Anushka (ABC)', NULL, 'system', 'system', 0),
(14, 'PAR000014', 'Satyabrata',   NULL, 'Pradhan', 'Male',   '9777334404', '9437334404', 'parent11@abcpuri.edu.in','Hotel Owner',           'Sea View Hotel Puri',        'Graduate',      1500000.00, NULL, TRUE, 'Father of Kabir (ABC)',   NULL, 'system', 'system', 0),
(15, 'PAR000015', 'Binodini',     NULL, 'Barik',   'Female', '9777445402', NULL,         'parent12@kcc.edu.in',    'Bank Employee',         'Axis Bank Cuttack',          'Post Graduate',  880000.00, NULL, TRUE, 'Mother of Debasish (KCC)',NULL, 'system', 'system', 0),
(16, 'PAR000016', 'Subash',       NULL, 'Sahu',    'Male',   '9777445403', NULL,         'parent13@kcc.edu.in',    'Pharmacist',            'Jan Aushadhi Kendra',        'Graduate',       620000.00, NULL, TRUE, 'Father of Sreeja (KCC)', NULL, 'system', 'system', 0),
(17, 'PAR000017', 'Mamata',       NULL, 'Mahapatra','Female','9777445404', NULL,         'parent14@kcc.edu.in',    'Homemaker',             NULL,                         'Graduate',           0.00, NULL, TRUE, 'Mother of Aniket (KCC)', NULL, 'system', 'system', 0);

INSERT IGNORE INTO student_parent (
    student_parent_id, student_id, parent_id, relationship, primary_contact,
    receive_sms, receive_email, pickup_authorized, active, created_by, updated_by, version)
VALUES
(5,  2,  5,  'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(6,  3,  6,  'MOTHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(7,  4,  7,  'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(8,  5,  8,  'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(9,  6,  9,  'MOTHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(10, 7,  10, 'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(11, 8,  11, 'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(12, 10, 12, 'MOTHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(13, 11, 13, 'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(14, 12, 14, 'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(15, 14, 15, 'MOTHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(16, 15, 16, 'FATHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0),
(17, 16, 17, 'MOTHER', TRUE,  TRUE, TRUE, TRUE, TRUE, 'system', 'system', 0);

-- =========================================================
-- STEP 12: ADDITIONAL STUDENTS (5 JSB, 3 JSC, 3 ABC, 3 KCC)
-- =========================================================
INSERT IGNORE INTO student (
    student_id, student_code, admission_number, roll_number, first_name, middle_name, last_name, gender,
    date_of_birth, religion, nationality, mother_tongue, mobile_number, email, photo_url, admission_date,
    status, transport_required, hostel_required, same_address, user_id, remarks, created_by, updated_by, version)
VALUES
-- JSB additional (21-25)
(21, 'JSB-STU-0006', 'JSB-ADM-26006', '6',  'Priyanka',   NULL, 'Sahoo',    'Female', '2018-03-07', 'Hindu', 'Indian', 'Odia', 9777111306, 'priyanka@jsb.edu.in',  NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II JSB',    'system', 'system', 0),
(22, 'JSB-STU-0007', 'JSB-ADM-26007', '7',  'Rohit',      NULL, 'Panda',    'Male',   '2017-11-15', 'Hindu', 'Indian', 'Odia', 9777111307, 'rohit@jsb.edu.in',     NULL, '2026-04-05', 'ACTIVE', TRUE,  FALSE, TRUE, NULL, 'Class III JSB',   'system', 'system', 0),
(23, 'JSB-STU-0008', 'JSB-ADM-26008', '8',  'Lipsa',      NULL, 'Swain',    'Female', '2017-06-22', 'Hindu', 'Indian', 'Odia', 9777111308, 'lipsa@jsb.edu.in',     NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class III JSB',   'system', 'system', 0),
(24, 'JSB-STU-0009', 'JSB-ADM-26009', '9',  'Shreyansh',  NULL, 'Patnaik',  'Male',   '2016-09-30', 'Hindu', 'Indian', 'Odia', 9777111309, 'shreyansh@jsb.edu.in', NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class IV JSB',    'system', 'system', 0),
(25, 'JSB-STU-0010', 'JSB-ADM-26010', '10', 'Susmita',    NULL, 'Choudhury','Female', '2016-01-04', 'Hindu', 'Indian', 'Odia', 9777111310, 'susmita@jsb.edu.in',   NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class IV JSB',    'system', 'system', 0),
-- JSC additional (26-28)
(26, 'JSC-STU-0006', 'JSC-ADM-26006', '6',  'Bikash',     NULL, 'Nanda',    'Male',   '2019-07-08', 'Hindu', 'Indian', 'Odia', 9777222501, 'student1@jsc.edu.in',  NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, 20,   'Class I JSC',     'system', 'system', 0),
(27, 'JSC-STU-0007', 'JSC-ADM-26007', '7',  'Pragnya',    NULL, 'Mishra',   'Female', '2018-11-25', 'Hindu', 'Indian', 'Odia', 9777222506, 'pragnya@jsc.edu.in',   NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II JSC',    'system', 'system', 0),
(28, 'JSC-STU-0008', 'JSC-ADM-26008', '8',  'Subhankar',  NULL, 'Sahu',     'Male',   '2018-05-14', 'Hindu', 'Indian', 'Odia', 9777222507, 'subhankar@jsc.edu.in', NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II JSC',    'system', 'system', 0),
-- ABC additional (29-31)
(29, 'ABCP-STU-0006', 'ABCP-ADM-26006', '6', 'Sudha',     NULL, 'Kar',      'Female', '2019-10-12', 'Hindu', 'Indian', 'Odia', 9777333506, 'sudha@abcpuri.edu.in',      NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class I ABC',    'system', 'system', 0),
(30, 'ABCP-STU-0007', 'ABCP-ADM-26007', '7', 'Manoranjan',NULL, 'Behera',   'Male',   '2018-02-19', 'Hindu', 'Indian', 'Odia', 9777333507, 'manoranjan@abcpuri.edu.in', NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class II ABC',   'system', 'system', 0),
(31, 'ABCP-STU-0008', 'ABCP-ADM-26008', '8', 'Deepika',   NULL, 'Naik',     'Female', '2019-04-03', 'Hindu', 'Indian', 'Odia', 9777333508, 'deepika@abcpuri.edu.in',    NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Class I ABC',    'system', 'system', 0),
-- KCC additional (32-34)
(32, 'KCC-STU-0006', 'KCC-ADM-26006', '6', 'Deepak',     NULL, 'Rout',     'Male',   '2006-04-18', 'Hindu', 'Indian', 'Odia', 9777444606, 'deepak@kcc.edu.in',    NULL, '2026-04-05', 'ACTIVE', FALSE, TRUE,  TRUE, NULL, 'Degree student KCC', 'system', 'system', 0),
(33, 'KCC-STU-0007', 'KCC-ADM-26007', '7', 'Monalisa',   NULL, 'Pati',     'Female', '2005-07-29', 'Hindu', 'Indian', 'Odia', 9777444607, 'monalisa@kcc.edu.in',  NULL, '2026-04-05', 'ACTIVE', FALSE, TRUE,  TRUE, NULL, 'Degree student KCC', 'system', 'system', 0),
(34, 'KCC-STU-0008', 'KCC-ADM-26008', '8', 'Gouranga',   NULL, 'Behera',   'Male',   '2006-09-05', 'Hindu', 'Indian', 'Odia', 9777444608, 'gouranga@kcc.edu.in',  NULL, '2026-04-05', 'ACTIVE', FALSE, FALSE, TRUE, NULL, 'Degree student KCC', 'system', 'system', 0);

INSERT IGNORE INTO student_enrollment (
    enrollment_id, student_id, academic_year_id, class_id, section_id, roll_number,
    status, active, remarks, created_by, updated_by, version)
VALUES
(21, 21, 1, 5, 3, '5', 'ACTIVE', TRUE, 'JSB Class II-A',    'system', 'system', 0),
(22, 22, 1, 6, 4, '5', 'ACTIVE', TRUE, 'JSB Class III-A',   'system', 'system', 0),
(23, 23, 1, 6, 4, '6', 'ACTIVE', TRUE, 'JSB Class III-A',   'system', 'system', 0),
(24, 24, 1, 8, 7, '1', 'ACTIVE', TRUE, 'JSB Class IV-A',    'system', 'system', 0),
(25, 25, 1, 8, 7, '2', 'ACTIVE', TRUE, 'JSB Class IV-A',    'system', 'system', 0),
(26, 26, 1, 4, 2, '5', 'ACTIVE', TRUE, 'JSC Class I-B',     'system', 'system', 0),
(27, 27, 1, 5, 6, '5', 'ACTIVE', TRUE, 'JSC Class II-B',    'system', 'system', 0),
(28, 28, 1, 5, 6, '6', 'ACTIVE', TRUE, 'JSC Class II-B',    'system', 'system', 0),
(29, 29, 1, 4, 1, '5', 'ACTIVE', TRUE, 'ABC Class I-A',     'system', 'system', 0),
(30, 30, 1, 5, 3, '5', 'ACTIVE', TRUE, 'ABC Class II-A',    'system', 'system', 0),
(31, 31, 1, 4, 1, '6', 'ACTIVE', TRUE, 'ABC Class I-A',     'system', 'system', 0),
(32, 32, 1, 7, 5, '6', 'ACTIVE', TRUE, 'KCC XI Commerce',   'system', 'system', 0),
(33, 33, 1, 7, 5, '7', 'ACTIVE', TRUE, 'KCC XI Commerce',   'system', 'system', 0),
(34, 34, 1, 7, 5, '8', 'ACTIVE', TRUE, 'KCC XI Commerce',   'system', 'system', 0);

INSERT IGNORE INTO student_medical (
    medical_id, student_id, blood_group, allergies, medical_conditions, medications,
    doctor_name, doctor_contact, emergency_notes, active, created_by, updated_by, version)
VALUES
(5,  26, 'B+', 'Dust allergy',    'None',                     'Antihistamine',      'Dr. B. Kumar Cuttack',  '9777005301', 'Inform PE teacher',                   TRUE, 'system', 'system', 0),
(6,  5,  'A+', 'Peanut allergy',  'None',                     'None',               'Dr. R. K. Mishra',      '9777005102', 'Carry epipen form',                   TRUE, 'system', 'system', 0),
(7,  14, 'O+', 'None',            'Myopia - wears glasses',   'None',               'Dr. Vision Care KCC',   '9777005401', 'Front row seating preferred',         TRUE, 'system', 'system', 0),
(8,  32, 'AB+','None',            'Mild hypertension history','Monitor, no meds',   'Dr. Sasmita Panda',     '9777005402', 'Avoid excessive stress',              TRUE, 'system', 'system', 0);

INSERT IGNORE INTO student_timeline (
    timeline_id, student_id, event_type, title, description, created_by, updated_by, version)
VALUES
(5,  5,  'ENROLLMENT_CREATED', 'Joined Class I B',    'Arjun Pani admitted to Javier School Cuttack',  'system', 'system', 0),
(6,  14, 'ENROLLMENT_CREATED', 'Joined College',      'Debasish Barik admitted to Kalinga College',    'system', 'system', 0),
(7,  26, 'ENROLLMENT_CREATED', 'Joined Class I B',    'Bikash Nanda admitted to Javier School Cuttack','system', 'system', 0),
(8,  32, 'ENROLLMENT_CREATED', 'Joined XI Commerce',  'Deepak Rout admitted to Kalinga College',       'system', 'system', 0);

INSERT IGNORE INTO student_achievement (
    achievement_id, student_id, title, category, achievement_date, issuer, rank_position, description,
    certificate_document_id, active, created_by, updated_by, version)
VALUES
(4,  5,  'Cuttack Inter-School Drawing',  'CULTURAL',  '2026-06-20', 'Javier School Cuttack', '1st',      'Won drawing competition among 5 schools',   NULL, TRUE, 'system', 'system', 0),
(5,  14, 'Commerce Quiz Winner',          'ACADEMIC',  '2026-06-22', 'Kalinga College',        '1st',      'First place in KCC inter-department quiz',   NULL, TRUE, 'system', 'system', 0),
(6,  29, 'Puri Dance Festival Finalist',  'CULTURAL',  '2026-06-25', 'ABC School Puri',        'Finalist', 'Reached finals in annual dance competition', NULL, TRUE, 'system', 'system', 0);

-- =========================================================
-- STEP 13: MORE ACADEMIC CALENDAR EVENTS
-- =========================================================
INSERT IGNORE INTO academic_calendar_event (
    event_id, academic_year_id, title, event_type, start_date, end_date, all_day,
    description, active, created_by, updated_by, version)
VALUES
(5,  1, 'Rath Yatra Celebration',      'SPECIAL_DAY',      '2026-07-04', '2026-07-04', TRUE, 'Puri Rath Yatra - special school assembly',          TRUE, 'system', 'system', 0),
(6,  1, 'First Term End',              'TERM_END',          '2026-10-31', '2026-10-31', TRUE, 'End of first academic term',                          TRUE, 'system', 'system', 0),
(7,  1, 'Puja Vacation',               'HOLIDAY',           '2026-10-01', '2026-10-20', TRUE, 'Durga Puja / Dussehra / Diwali holidays',            TRUE, 'system', 'system', 0),
(8,  1, 'Annual Examination',          'EXAM',              '2027-02-10', '2027-02-28', TRUE, 'Annual final examination for all classes',            TRUE, 'system', 'system', 0),
(9,  1, 'Sports Day',                  'SPECIAL_DAY',       '2026-11-15', '2026-11-15', TRUE, 'Annual sports day celebration',                       TRUE, 'system', 'system', 0),
(10, 1, 'Winter Vacation',             'HOLIDAY',           '2026-12-26', '2027-01-03', TRUE, 'Christmas and New Year holidays',                     TRUE, 'system', 'system', 0),
(11, 1, 'Science Exhibition',          'SPECIAL_DAY',       '2026-12-12', '2026-12-13', TRUE, 'Inter-school science exhibition at Bhubaneswar',      TRUE, 'system', 'system', 0),
(12, 1, 'Republic Day',                'HOLIDAY',           '2027-01-26', '2027-01-26', TRUE, 'National Republic Day flag hoisting ceremony',        TRUE, 'system', 'system', 0),
(13, 1, 'Teacher Training Workshop',   'PROFESSIONAL_DAY',  '2026-07-18', '2026-07-18', TRUE, 'Campus closed for teacher capacity building',          TRUE, 'system', 'system', 0),
(14, 1, 'Annual Day',                  'SPECIAL_DAY',       '2027-01-10', '2027-01-10', TRUE, 'Annual day with cultural programs and prize giving',  TRUE, 'system', 'system', 0),
(15, 1, 'Independence Day',            'HOLIDAY',           '2026-08-15', '2026-08-15', TRUE, 'National Independence Day',                            TRUE, 'system', 'system', 0),
(16, 1, 'Gandhi Jayanti',              'HOLIDAY',           '2026-10-02', '2026-10-02', TRUE, 'Mahatma Gandhi Birthday - National Holiday',          TRUE, 'system', 'system', 0);

-- =========================================================
-- STEP 14: MORE SYLLABUS DATA (JSC and ABC curriculum)
-- =========================================================
INSERT IGNORE INTO syllabus (syllabus_id, academic_year_id, class_id, subject_id, title, version_no, published, active, remarks, created_by, updated_by, version)
VALUES
(5, 1, 4, 1, 'Class I English - Javier Cuttack',       'v1', TRUE, TRUE, 'JSC curriculum', 'system', 'system', 0),
(6, 1, 5, 1, 'Class II English Syllabus',              'v1', TRUE, TRUE, 'CBSE primary',  'system', 'system', 0),
(7, 1, 5, 2, 'Class II Mathematics Syllabus',          'v1', TRUE, TRUE, 'CBSE primary',  'system', 'system', 0),
(8, 1, 6, 3, 'Class III Science Syllabus',             'v1', TRUE, TRUE, 'Primary sci',   'system', 'system', 0),
(9, 1, 7, 10,'Class XI Business Studies Syllabus',     'v1', TRUE, TRUE, 'CBSE XI',       'system', 'system', 0);

INSERT IGNORE INTO syllabus_unit (unit_id, syllabus_id, unit_number, unit_name, estimated_hours, display_order, active, remarks, created_by, updated_by, version)
VALUES
(5, 5, 1, 'Friends and Animals',          8,  1, TRUE, 'Class I Eng Unit 1', 'system', 'system', 0),
(6, 5, 2, 'Fun at School',               10,  2, TRUE, 'Class I Eng Unit 2', 'system', 'system', 0),
(7, 6, 1, 'Reading and Writing',         12,  1, TRUE, 'Class II Eng Unit 1','system', 'system', 0),
(8, 7, 1, 'Numbers to 100',             12,  1, TRUE, 'Class II Math Unit 1','system', 'system', 0),
(9, 8, 1, 'Plants Around Us',           10,  1, TRUE, 'Class III Sci',       'system', 'system', 0),
(10,9, 1, 'Nature and Purpose of Business',12,1, TRUE, 'Business Studies 1',  'system', 'system', 0);

INSERT IGNORE INTO syllabus_chapter (chapter_id, unit_id, chapter_number, chapter_name, estimated_hours, display_order, active, remarks, created_by, updated_by, version)
VALUES
(5,  5, 1, 'My Pet Dog',          4, 1, TRUE, 'Story chapter',      'system', 'system', 0),
(6,  6, 1, 'My Classroom',        5, 1, TRUE, 'Classroom vocab',    'system', 'system', 0),
(7,  7, 1, 'Letters and Words',   6, 1, TRUE, 'Language chapter',   'system', 'system', 0),
(8,  8, 1, 'Counting by 10s',     6, 1, TRUE, 'Math chapter',       'system', 'system', 0),
(9,  9, 1, 'Types of Plants',     5, 1, TRUE, 'Botany chapter',     'system', 'system', 0),
(10,10, 1, 'Concept of Business', 6, 1, TRUE, 'Business chapter',   'system', 'system', 0);

INSERT IGNORE INTO syllabus_topic (topic_id, chapter_id, topic_number, topic_name, estimated_hours, display_order, active, remarks, created_by, updated_by, version)
VALUES
(5,  5, 1, 'My Favourite Animal',    2, 1, TRUE, 'Animal topic',      'system', 'system', 0),
(6,  6, 1, 'Things in My Classroom', 3, 1, TRUE, 'Classroom topic',   'system', 'system', 0),
(7,  7, 1, 'Vowels and Consonants',  3, 1, TRUE, 'Phonics topic',     'system', 'system', 0),
(8,  8, 1, 'Counting 10, 20, 30...',  3, 1, TRUE, 'Skip counting',    'system', 'system', 0),
(9,  9, 1, 'Trees and Shrubs',       3, 1, TRUE, 'Plant types',       'system', 'system', 0),
(10,10, 1, 'Meaning of Business',    3, 1, TRUE, 'Business concept',  'system', 'system', 0);

INSERT IGNORE INTO syllabus_coverage (coverage_id, topic_id, teacher_id, status, completion_date, remarks, created_by, updated_by, version)
VALUES
(5,  5, 18, 'COMPLETED',   '2026-06-28', 'Completed with activity',          'system', 'system', 0),
(6,  6, 18, 'IN_PROGRESS', NULL,         'Started, 1 more period needed',    'system', 'system', 0),
(7,  7, 18, 'NOT_STARTED', NULL,         'Scheduled for next week',          'system', 'system', 0),
(8,  8, 19, 'COMPLETED',   '2026-06-25', 'Completed with worksheet',         'system', 'system', 0),
(9,  9, 10, 'NOT_STARTED', NULL,         'Starting week of July 7',          'system', 'system', 0),
(10,10, 14, 'COMPLETED',   '2026-06-22', 'Completed with case study',        'system', 'system', 0);

-- Teacher arrangement for JSC
INSERT IGNORE INTO teacher_arrangement (arrangement_id, slot_id, absent_teacher_id, substitute_teacher_id, arrangement_date, status, reason, approved_by, active, created_by, updated_by, version)
VALUES
(2, 8, 18, 19, '2026-06-29', 'APPROVED', 'JSC teacher attended district education workshop', 17, TRUE, 'system', 'system', 0);

-- =========================================================
-- STEP 15: MORE STUDENT ATTENDANCE (3 orgs, 3 days each)
-- =========================================================
INSERT IGNORE INTO student_attendance (
    attendance_id, organization_id, student_id, student_name, roll_number, admission_number,
    academic_year_id, class_id, class_name, section_id, section_name, attendance_date,
    status, remarks, marked_by, created_by, updated_by, version)
VALUES
-- June 27 - JSB
(6,  1, 1,  'Aarav Mohanty',  '1', 'JSB-ADM-26001', 1, 4, 'Class I',   1, 'A', '2026-06-27', 'PRESENT', 'On time',       'javier.admin', 'system', 'system', 0),
(7,  1, 2,  'Ishani Mishra',  '2', 'JSB-ADM-26002', 1, 4, 'Class I',   1, 'A', '2026-06-27', 'PRESENT', 'On time',       'javier.admin', 'system', 'system', 0),
(8,  1, 17, 'Sambit Behera',  '5', 'JSB-ADM-26005', 1, 4, 'Class I',   2, 'B', '2026-06-27', 'ABSENT',  'Holiday trip',  'javier.admin', 'system', 'system', 0),
-- June 27 - JSC
(9,  2, 5,  'Arjun Pani',     '1', 'JSC-ADM-26001', 1, 4, 'Class I',   2, 'B', '2026-06-27', 'PRESENT', 'On time',       'jsc.admin',    'system', 'system', 0),
(10, 2, 6,  'Manya Swain',    '2', 'JSC-ADM-26002', 1, 4, 'Class I',   2, 'B', '2026-06-27', 'PRESENT', 'On time',       'jsc.admin',    'system', 'system', 0),
(11, 2, 7,  'Kunal Das',      '1', 'JSC-ADM-26003', 1, 5, 'Class II',  6, 'B', '2026-06-27', 'LATE',    'Bus delay',     'jsc.admin',    'system', 'system', 0),
-- June 27 - ABC
(12, 3, 9,  'Ishita Das',     '1', 'ABCP-ADM-26001',1, 4, 'Class I',   1, 'A', '2026-06-27', 'PRESENT', 'On time',       'abc.admin',    'system', 'system', 0),
(13, 3, 10, 'Sai Panda',      '2', 'ABCP-ADM-26002',1, 4, 'Class I',   1, 'A', '2026-06-27', 'ABSENT',  'Medical leave', 'abc.admin',    'system', 'system', 0),
-- June 27 - Kalinga
(14, 4, 13, 'Ritika Panda',   '1', 'KCC-ADM-26001', 1, 7, 'XI Com',    5, 'A', '2026-06-27', 'PRESENT', 'Present',       'kalinga.admin','system', 'system', 0),
(15, 4, 14, 'Debasish Barik', '2', 'KCC-ADM-26002', 1, 7, 'XI Com',    5, 'A', '2026-06-27', 'LATE',    'Bus delay',     'kalinga.admin','system', 'system', 0),
-- June 28 - JSB
(16, 1, 1,  'Aarav Mohanty',  '1', 'JSB-ADM-26001', 1, 4, 'Class I',   1, 'A', '2026-06-28', 'PRESENT', 'Saturday duty','javier.admin', 'system', 'system', 0),
(17, 1, 2,  'Ishani Mishra',  '2', 'JSB-ADM-26002', 1, 4, 'Class I',   1, 'A', '2026-06-28', 'PRESENT', 'Saturday duty','javier.admin', 'system', 'system', 0),
-- June 28 - JSC
(18, 2, 5,  'Arjun Pani',     '1', 'JSC-ADM-26001', 1, 4, 'Class I',   2, 'B', '2026-06-28', 'PRESENT', 'Regular',       'jsc.admin',    'system', 'system', 0),
(19, 2, 26, 'Bikash Nanda',   '5', 'JSC-ADM-26006', 1, 4, 'Class I',   2, 'B', '2026-06-28', 'PRESENT', 'Regular',       'jsc.admin',    'system', 'system', 0),
-- June 28 - ABC
(20, 3, 9,  'Ishita Das',     '1', 'ABCP-ADM-26001',1, 4, 'Class I',   1, 'A', '2026-06-28', 'PRESENT', 'Regular',       'abc.admin',    'system', 'system', 0),
-- June 30 - JSC additional
(21, 2, 26, 'Bikash Nanda',   '5', 'JSC-ADM-26006', 1, 4, 'Class I',   2, 'B', '2026-06-30', 'PRESENT', 'Regular',       'jsc.admin',    'system', 'system', 0),
(22, 2, 27, 'Pragnya Mishra', '5', 'JSC-ADM-26007', 1, 5, 'Class II',  6, 'B', '2026-06-30', 'ABSENT',  'Sick leave',    'jsc.admin',    'system', 'system', 0),
-- June 30 - ABC additional
(23, 3, 10, 'Sai Panda',      '2', 'ABCP-ADM-26002',1, 4, 'Class I',   1, 'A', '2026-06-30', 'PRESENT', 'Recovered',     'abc.admin',    'system', 'system', 0),
(24, 3, 11, 'Anushka Jena',   '1', 'ABCP-ADM-26003',1, 5, 'Class II',  3, 'A', '2026-06-30', 'PRESENT', 'On time',       'abc.admin',    'system', 'system', 0),
-- June 30 - Kalinga additional
(25, 4, 14, 'Debasish Barik', '2', 'KCC-ADM-26002', 1, 7, 'XI Com',    5, 'A', '2026-06-30', 'PRESENT', 'On time',       'kalinga.admin','system', 'system', 0),
(26, 4, 15, 'Sreeja Sahu',    '3', 'KCC-ADM-26003', 1, 7, 'XI Com',    5, 'A', '2026-06-30', 'PRESENT', 'Regular',       'kalinga.admin','system', 'system', 0);

-- =========================================================
-- STEP 16: MORE STAFF ATTENDANCE (all 4 orgs, 3 days)
-- =========================================================
INSERT IGNORE INTO staff_attendance (
    attendance_id, organization_id, staff_id, staff_name, staff_code, department, designation,
    attendance_date, sign_in_time, sign_out_time, working_minutes, shift, status, remarks,
    marked_by, created_by, updated_by, version)
VALUES
-- June 27 - JSB
(5,  1, 1, 'Rupesh Pati',       'JSB-TCHR-001', 'English',       'English Teacher',     '2026-06-27', '2026-06-27 08:00:00', '2026-06-27 15:25:00', 445, 'Morning', 'PRESENT', 'Regular',       'javier.admin',  'system', 'system', 0),
(6,  1, 2, 'Saswati Senapati',  'JSB-TCHR-002', 'Mathematics',   'Mathematics Teacher', '2026-06-27', '2026-06-27 08:02:00', '2026-06-27 15:20:00', 438, 'Morning', 'PRESENT', 'Regular',       'javier.admin',  'system', 'system', 0),
-- June 27 - JSC
(7,  2, 7, 'Debendra Barik',    'JSC-TCHR-001', 'English',       'English Teacher',     '2026-06-27', '2026-06-27 08:00:00', '2026-06-27 15:30:00', 450, 'Morning', 'PRESENT', 'Regular',       'jsc.admin',     'system', 'system', 0),
(8,  2, 8, 'Geetanjali Rath',   'JSC-TCHR-002', 'Mathematics',   'Mathematics Teacher', '2026-06-27', '2026-06-27 08:05:00', '2026-06-27 15:28:00', 443, 'Morning', 'PRESENT', 'Regular',       'jsc.admin',     'system', 'system', 0),
-- June 27 - ABC
(9,  3, 3, 'Suman Nayak',       'ABCP-TCHR-001','Science',       'Science Teacher',     '2026-06-27', '2026-06-27 08:00:00', '2026-06-27 15:20:00', 440, 'Morning', 'PRESENT', 'Regular',       'abc.admin',     'system', 'system', 0),
-- June 27 - Kalinga
(10, 4, 4, 'Madhuri Tripathy',  'KCC-TCHR-001', 'Commerce',      'Commerce Lecturer',   '2026-06-27', '2026-06-27 08:10:00', '2026-06-27 15:40:00', 450, 'Morning', 'PRESENT', 'Regular',       'kalinga.admin', 'system', 'system', 0),
-- June 28 - JSB (Saturday)
(11, 1, 1, 'Rupesh Pati',       'JSB-TCHR-001', 'English',       'English Teacher',     '2026-06-28', '2026-06-28 08:00:00', '2026-06-28 12:30:00', 270, 'Morning', 'PRESENT', 'Half day Sat',  'javier.admin',  'system', 'system', 0),
(12, 1, 5, 'Ananya Dash',       'JSB-ADM-001',  'Administration','Office Administrator', '2026-06-28', '2026-06-28 08:05:00', '2026-06-28 12:35:00', 270, 'Morning', 'PRESENT', 'Saturday duty', 'javier.admin',  'system', 'system', 0),
-- June 28 - JSC
(13, 2, 7, 'Debendra Barik',    'JSC-TCHR-001', 'English',       'English Teacher',     '2026-06-28', '2026-06-28 08:00:00', '2026-06-28 12:30:00', 270, 'Morning', 'PRESENT', 'Saturday',      'jsc.admin',     'system', 'system', 0),
-- June 28 - Kalinga
(14, 4, 4, 'Madhuri Tripathy',  'KCC-TCHR-001', 'Commerce',      'Commerce Lecturer',   '2026-06-28', '2026-06-28 09:00:00', '2026-06-28 14:00:00', 300, 'Morning', 'PRESENT', 'College half day','kalinga.admin','system', 'system', 0),
-- June 30 - JSC new staff
(15, 2, 9, 'Sulochana Babu',    'JSC-ADM-001',  'Administration','Office Administrator', '2026-06-30', '2026-06-30 08:00:00', '2026-06-30 15:30:00', 450, 'Morning', 'PRESENT', 'Regular',       'jsc.admin',     'system', 'system', 0),
-- June 30 - Kalinga admin
(16, 4,10, 'Sourav Swain',      'KCC-ADM-001',  'Administration','College Administrator','2026-06-30', '2026-06-30 08:00:00', '2026-06-30 15:45:00', 465, 'Morning', 'PRESENT', 'Regular',       'kalinga.admin', 'system', 'system', 0);

-- =========================================================
-- STEP 17: MORE INQUIRY DATA (richer per org)
-- =========================================================
INSERT IGNORE INTO inquiry (
    inquiry_id, organization_id, name, mobile_number, email, class_interested_in, address,
    inquiry_source, referred_by, comments, assigned_counselor_id, status, is_deleted,
    last_follow_up_date, last_follow_up_type, next_follow_up_date, created_by, updated_by, version)
VALUES
(5,  1, 'Pramod Biswal',    '9777115601', 'pramod@example.com',   'Class III',       'Nayapalli, Bhubaneswar',     'Walk-In', NULL,           'CBSE, wants sports facilities',        3,  'CONTACTED',  FALSE, '2026-06-30 14:00:00', 'WALK_IN', '2026-07-05', 'system', 'system', 0),
(6,  1, 'Minakshi Nanda',   '9777115602', 'minakshi@example.com', 'LKG',             'Khandagiri, Bhubaneswar',    'Phone',   NULL,           'Enquiring in advance for next year',   3,  'NEW',        FALSE, NULL,                  NULL,      '2026-07-08', 'system', 'system', 0),
(7,  2, 'Ramakanta Sahu',   '9777225601', 'ramakanta@example.com','Class II',        'College Square, Cuttack',    'Referral','jsc.parent1',  'Wants scholarship, referred by parent', 17, 'INTERESTED', FALSE, '2026-06-29 11:30:00', 'CALL',    '2026-07-03', 'system', 'system', 0),
(8,  2, 'Suresh Mohanty',   '9777225602', 'suresh@example.com',   'Class I',         'Madhupatna, Cuttack',        'Walk-In', NULL,           'Elder son already in this school',      17, 'CONTACTED',  FALSE, '2026-06-28 10:00:00', 'WALK_IN', '2026-07-04', 'system', 'system', 0),
(9,  3, 'Hemanta Rath',     '9777336601', 'hemanta@example.com',  'Class IV',        'Baliapanda, Puri',           'Phone',   NULL,           'Wants ICSE, transferring from govt school',9,'INTERESTED',FALSE, '2026-06-30 15:00:00', 'CALL',    '2026-07-06', 'system', 'system', 0),
(10, 3, 'Geeta Pradhan',    '9777336602', 'geeta@example.com',    'Class I',         'Grand Road, Puri',           'Website', NULL,           'Single parent, needs hostel clarity',   9,  'NEW',        FALSE, NULL,                  NULL,      '2026-07-09', 'system', 'system', 0),
(11, 4, 'Arun Patnaik',     '9777447601', 'arun@example.com',     '1st Year Degree', 'Dolamundai, Cuttack',        'Phone',   NULL,           'B.Com Hons, Utkal Univ affiliation?',  13, 'CONTACTED',  FALSE, '2026-06-30 12:00:00', 'CALL',    '2026-07-05', 'system', 'system', 0),
(12, 4, 'Pradipta Singh',   '9777447602', 'pradipta@example.com', '1st Year Degree', 'Tulsipur, Cuttack',          'Walk-In', NULL,           'Wants hostel + affordable fee',         13, 'INTERESTED', FALSE, '2026-06-29 15:00:00', 'WALK_IN', '2026-07-02', 'system', 'system', 0);

INSERT IGNORE INTO inquiry_follow_up (
    follow_up_id, inquiry_id, follow_up_type, remarks, status_after,
    follow_up_date, next_follow_up_date, created_by, updated_by, version)
VALUES
(4, 5,  'WALK_IN', 'Visited campus, liked library and labs',              'CONTACTED',  '2026-06-30 14:00:00', '2026-07-05', 'system', 'system', 0),
(5, 7,  'CALL',    'Discussed scholarship structure, sent prospectus',    'INTERESTED', '2026-06-29 11:30:00', '2026-07-03', 'system', 'system', 0),
(6, 8,  'WALK_IN', 'Parent came with elder child, saw classrooms',        'CONTACTED',  '2026-06-28 10:00:00', '2026-07-04', 'system', 'system', 0),
(7, 9,  'CALL',    'Explained CBSE vs ICSE difference and fees',          'INTERESTED', '2026-06-30 15:00:00', '2026-07-06', 'system', 'system', 0),
(8, 11, 'CALL',    'Shared college prospectus, discussed fee structure',  'CONTACTED',  '2026-06-30 12:00:00', '2026-07-05', 'system', 'system', 0),
(9, 12, 'WALK_IN', 'Showed hostel and campus, parent very interested',    'INTERESTED', '2026-06-29 15:00:00', '2026-07-02', 'system', 'system', 0);

INSERT IGNORE INTO counseling_note (
    note_id, inquiry_id, student_requirements, parent_concerns, campus_visit_info,
    recommendations, notes, created_by, updated_by, version)
VALUES
(3, 7,  'Good academic environment',     'Wants scholarship for meritorious student', 'Visited Cuttack campus', 'Send scholarship details',    'Good prospect for JSC',    'system', 'system', 0),
(4, 9,  'ICSE curriculum preferred',     'Transport facility from Baliapanda',        'Not yet visited',        'Schedule campus tour',        'ABC Puri is a good fit',   'system', 'system', 0),
(5, 11, 'B.Com Honours degree',          'Affordability is key concern',              'Visited KCC campus',     'Discuss installment payment',  'Warm lead for KCC',        'system', 'system', 0);

-- =========================================================
-- STEP 18: MORE APPLICATION ADMISSIONS
-- =========================================================
INSERT IGNORE INTO application_admission (
    application_id, application_number, organization_id, inquiry_id, applicant_name, date_of_birth,
    gender, applying_for_class, email, contact_number, address, parent_name, parent_contact,
    parent_email, status, reviewed_by_user_id, reviewed_on, internal_comments, created_by, updated_by, version)
VALUES
(4, 'APP-JSC-0001', 2, 7, 'Ramakanta Sahu Jr',  '2018-08-10', 'Male',   'Class II',       'ramakanta.jr@example.com', '9777225601', 'College Square, Cuttack', 'Ramakanta Sahu',   '9777225601', 'ramakanta@example.com', 'SUBMITTED',    NULL, NULL,         'Awaiting documents',        'system', 'system', 0),
(5, 'APP-ABC-0002', 3, 9, 'Sudhir Rath',         '2016-04-01', 'Male',   'Class IV',       'sudhir@example.com',       '9777336601', 'Baliapanda, Puri',        'Hemanta Rath',     '9777336601', 'hemanta@example.com',   'UNDER_REVIEW', 9, '2026-07-01', 'TC from previous school pending','system','system',0),
(6, 'APP-KCC-0002', 4,12, 'Pradipta Singh',      '2007-03-18', 'Male',   '1st Year Degree','pradipta@example.com',     '9777447602', 'Tulsipur, Cuttack',       'Sangram Singh',    '9777447603', 'sangram.s@example.com', 'SUBMITTED',    NULL, NULL,         'Hostel preference noted',   'system', 'system', 0);

-- =========================================================
-- STEP 19: MORE NOTICES PER ORG
-- =========================================================
INSERT IGNORE INTO notice (
    notice_id, organization_id, title, content, category, pinned, publish_date, expiry_date,
    status, attachment_url, published_by_user_id, created_by, updated_by, version)
VALUES
(4, 2, 'Term Test Schedule - Cuttack',     'Term test for Class I-III is from July 1-5, 2026. Parents please sign test papers.', 'ACADEMIC', FALSE, '2026-06-25', '2026-07-07', 'PUBLISHED', NULL, 17, 'system', 'system', 0),
(5, 3, 'ICSE Board Registration Notice',   'All Class X students submit registration forms by July 31, 2026.', 'ACADEMIC', TRUE, '2026-06-28', '2026-07-31', 'PUBLISHED', NULL, 9,  'system', 'system', 0),
(6, 4, 'College Fee Payment Deadline',     'First semester fees must be paid by July 15, 2026. Late fee: 2% per month.', 'FINANCE', TRUE, '2026-06-25', '2026-07-15', 'PUBLISHED', NULL, 13, 'system', 'system', 0),
(7, 1, 'Bus Route Update - Route 3',       'Bus Route 3 (Patia-Infocity) has new stop at Nalco Square from July 1.', 'OPERATIONAL', FALSE, '2026-06-29', '2026-07-31', 'PUBLISHED', NULL, 3,  'system', 'system', 0),
(8, 2, 'Parent Teacher Meeting - July 12', 'PTA meeting on July 12, 2026 at 10 AM in Javier Cuttack campus hall.', 'PARENT_MEETING', TRUE, '2026-07-01', '2026-07-13', 'DRAFT', NULL, 17, 'system', 'system', 0);

-- =========================================================
-- STEP 20: MORE AUDIT LOG ENTRIES
-- =========================================================
INSERT IGNORE INTO audit_log (
    id, organization_id, tenant_code, correlation_id, event_type, action, entity_type, entity_id,
    actor_user_id, actor_username, source_ip, user_agent, changes, summary, occurred_at, created_by, updated_by, version)
VALUES
(4, 2, 'jsc-cuttack',     'corr-004', 'CREATE', 'Create Staff',       'Staff',       '7',  17, 'jsc.admin',     '127.0.0.1', 'Chrome', '{"staffCode":"JSC-TCHR-001"}',       'Added Debendra Barik',         '2026-06-30 09:10:00', 'system', 'system', 0),
(5, 3, 'abc-puri',        'corr-005', 'CREATE', 'Create Inquiry',     'Inquiry',     '9',  9,  'abc.admin',     '127.0.0.1', 'Chrome', '{"inquiryId":9}',                    'New inquiry from Hemanta Rath', '2026-06-30 15:00:00', 'system', 'system', 0),
(6, 4, 'kcc-cuttack',     'corr-006', 'UPDATE', 'Mark Attendance',    'Attendance',  '14', 13, 'kalinga.admin', '127.0.0.1', 'Chrome', '{"status":"LATE"}',                  'Marked Debasish late',         '2026-06-30 09:20:00', 'system', 'system', 0),
(7, 1, 'jsb-bhubaneswar', 'corr-007', 'LOGIN',  'User Login',         'User',        '3',  3,  'javier.admin',  '127.0.0.1', 'Chrome', NULL,                                 'Admin login - JSB',            '2026-06-30 08:00:00', 'system', 'system', 0),
(8, 2, 'jsc-cuttack',     'corr-008', 'CREATE', 'Create Application', 'Application', '4',  17, 'jsc.admin',     '127.0.0.1', 'Edge',   '{"applicationNumber":"APP-JSC-0001"}','Application submitted JSC',   '2026-06-30 14:30:00', 'system', 'system', 0);

INSERT IGNORE INTO security_audit_log (
    id, event_code, username, tenant_code, source_ip, user_agent, success, severity, message, correlation_id, occurred_at, created_by, updated_by, version)
VALUES
(3, 'LOGIN_SUCCESS', 'jsc.owner',     'jsc-cuttack', '127.0.0.1', 'Chrome', TRUE,  'INFO',   'Successful login',       'corr-110', '2026-06-30 09:00:00', 'system', 'system', 0),
(4, 'LOGIN_SUCCESS', 'abc.owner',     'abc-puri',    '127.0.0.1', 'Chrome', TRUE,  'INFO',   'Successful login',       'corr-111', '2026-06-30 09:05:00', 'system', 'system', 0),
(5, 'LOGIN_SUCCESS', 'kalinga.owner', 'kcc-cuttack', '127.0.0.1', 'Chrome', TRUE,  'INFO',   'Successful login',       'corr-112', '2026-06-30 09:10:00', 'system', 'system', 0),
(6, 'LOGIN_FAILED',  'jsc.admin',     'jsc-cuttack', '127.0.0.1', 'Chrome', FALSE, 'MEDIUM', 'Wrong password attempt', 'corr-113', '2026-06-30 08:55:00', 'system', 'system', 0);

INSERT IGNORE INTO login_history (id, user_id, status, login_time, logout_time, ip_address, browser, operating_system, failure_reason, created_by, updated_by, version)
VALUES
(5, 16, 'SUCCESS', '2026-06-30 09:00:00', NULL, '127.0.0.1', 'Chrome', 'Windows 11', NULL,            'system', 'system', 0),
(6, 17, 'SUCCESS', '2026-06-30 09:05:00', NULL, '127.0.0.1', 'Edge',   'Windows 11', NULL,            'system', 'system', 0),
(7,  8, 'SUCCESS', '2026-06-30 09:10:00', NULL, '127.0.0.1', 'Chrome', 'macOS',      NULL,            'system', 'system', 0),
(8, 12, 'SUCCESS', '2026-06-30 09:15:00', NULL, '127.0.0.1', 'Chrome', 'Windows 11', NULL,            'system', 'system', 0),
(9, 17, 'FAILED',  '2026-06-30 08:55:00', NULL, '127.0.0.1', 'Chrome', 'Windows 11', 'Wrong password','system', 'system', 0);

-- =========================================================
-- STEP 21: MORE USER SESSIONS (JSC, ABC, Kalinga)
-- =========================================================
INSERT IGNORE INTO user_sessions (id, user_id, refresh_token, device_name, browser, operating_system, ip_address, login_at, logout_at, status, created_by, updated_by, version)
VALUES
(4, 16, 'seed-refresh-jsc-owner',     'Lenovo IdeaPad', 'Chrome', 'Windows 11', '127.0.0.1', '2026-06-30 09:00:00', NULL, 'ACTIVE', 'system', 'system', 0),
(5, 12, 'seed-refresh-kalinga-owner', 'HP Pavilion',    'Chrome', 'Windows 11', '127.0.0.1', '2026-06-30 09:15:00', NULL, 'ACTIVE', 'system', 'system', 0),
(6,  8, 'seed-refresh-abc-owner',     'Dell Laptop',    'Chrome', 'macOS',      '127.0.0.1', '2026-06-30 09:10:00', NULL, 'ACTIVE', 'system', 'system', 0);

-- =========================================================
-- STEP 22: PERIOD TEMPLATE for College (different schedule)
-- =========================================================
INSERT IGNORE INTO timetable_template (template_id, schedule_id, template_name, active, remarks, created_by, updated_by, version)
VALUES (2, 1, 'College Timetable', TRUE, 'Kalinga College lecture schedule', 'system', 'system', 0);

INSERT IGNORE INTO period_template (period_template_id, template_id, period_number, period_name, start_time, end_time, period_type, display_order, active, created_by, updated_by, version)
VALUES
(6, 2, 1, 'Lecture 1', '09:00:00', '10:00:00', 'CLASS',  1, TRUE, 'system', 'system', 0),
(7, 2, 2, 'Lecture 2', '10:00:00', '11:00:00', 'CLASS',  2, TRUE, 'system', 'system', 0),
(8, 2, 3, 'Recess',    '11:00:00', '11:15:00', 'BREAK',  3, TRUE, 'system', 'system', 0),
(9, 2, 4, 'Lecture 3', '11:15:00', '12:15:00', 'CLASS',  4, TRUE, 'system', 'system', 0),
(10,2, 5, 'Lunch',     '13:00:00', '14:00:00', 'LUNCH',  5, TRUE, 'system', 'system', 0),
(11,2, 6, 'Lecture 4', '14:00:00', '15:00:00', 'CLASS',  6, TRUE, 'system', 'system', 0);

-- =========================================================
-- STEP 23: ADDITIONAL ADDRESSES
-- =========================================================
INSERT IGNORE INTO address (
    address_id, address_line1, address_line2, landmark, city, district, state, country, postal_code, active, created_by, updated_by, version)
VALUES
(5, 'Sector 6, Cuttack',     'Near Badambadi',   'Badambadi Square', 'Cuttack',      'Cuttack', 'Odisha', 'India', '753009', TRUE, 'system', 'system', 0),
(6, 'Dolamundai',            'Near Taladanda',   'Old Bus Stand',    'Cuttack',      'Cuttack', 'Odisha', 'India', '753001', TRUE, 'system', 'system', 0),
(7, 'Grand Road, Near Temple','Sipasurubali',     'Puri Temple',      'Puri',         'Puri',    'Odisha', 'India', '752001', TRUE, 'system', 'system', 0),
(8, 'Ranihat Colony',        'Near Ranihat',     'Ranihat Crossing', 'Cuttack',      'Cuttack', 'Odisha', 'India', '753006', TRUE, 'system', 'system', 0),
(9, 'Unit-4, Near Police Lines','Near BJB College','BJB Nagar',       'Bhubaneswar',  'Khordha', 'Odisha', 'India', '751009', TRUE, 'system', 'system', 0);

-- =========================================================
-- STEP 24: MORE NOTIFICATION (JSC, Kalinga)
-- =========================================================
INSERT IGNORE INTO notification (
    notification_id, organization_id, subject, body, channels_csv, category, scheduled_at, sent_at, status,
    triggered_by_user_id, total_recipients, delivered_count, failed_count, created_by, updated_by, version)
VALUES
(3, 2, 'Welcome to Javier Cuttack', 'Welcome to JSC AY 2026-27. Please check the academic calendar.', 'EMAIL,IN_APP', 'GENERAL',    '2026-06-27 10:00:00', '2026-06-27 10:00:15', 'SENT',    17, 5, 5, 0, 'system', 'system', 0),
(4, 4, 'Fee Reminder',              'Please clear pending fees by July 15, 2026.',                      'SMS,EMAIL',   'FINANCE',    '2026-06-30 11:00:00', NULL,                  'PENDING', 13, 8, 0, 0, 'system', 'system', 0),
(5, 3, 'Attendance Alert - ABC',    'Sai Panda was absent on June 27. Please inform.',                  'SMS,EMAIL',   'ATTENDANCE', '2026-06-27 15:00:00', '2026-06-27 15:00:10', 'SENT',    9,  1, 1, 0, 'system', 'system', 0);

-- =========================================================
-- FINAL: Ensure all new users get Password@123 BCrypt
-- (DevDataInitializer will handle this, but adding placeholder marker)
-- =========================================================
UPDATE users SET password = 'PLACEHOLDER' WHERE password IS NULL OR password = '';
