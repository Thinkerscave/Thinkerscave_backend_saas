-- =============================================================================
-- ThinkersCave SaaS — PostgreSQL PLATFORM MASTER SEED
-- =============================================================================
-- Purpose : Fresh production bootstrap so Super Admin can log in and see menus.
-- Assumes : All application tables already exist (empty or nearly empty).
-- Scope   : Master / platform data ONLY (no students, subjects, attendance, etc.)
--
-- Login (Thinkers Department / PLATFORM login — not institution login):
--   Username : superadmin
--   Password : admin@123  (only for a newly inserted row; existing passwords are never overwritten)
--
-- Run (example):
--   psql -h <host> -U <user> -d <database> -v ON_ERROR_STOP=1 -f 01_platform_master_seed.sql
--
-- If login fails on password only: Spring BCrypt may not match pgcrypto on some hosts.
-- Set a Spring-compatible hash once (do not re-run mass password updates):
--   UPDATE users SET password = '<spring-bcrypt-hash>' WHERE username = 'superadmin';
-- =============================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =============================================================================
-- 1) PLATFORM CUSTOMER + ORGANIZATION (host for Super Admin FK)
-- =============================================================================
-- Super Admin users.organization_id and role_permissions.organization_id = 1
-- This is NOT a school tenant — it is the ThinkersCave platform host org.

INSERT INTO customers (
    id, customer_code, customer_name, status,
    business_email, mobile_number, alternate_mobile_number,
    notes, active, owner_user_id,
    created_by, created_on, updated_by, updated_on, version
) VALUES (
    1, 'CUS000001', 'ThinkersCave Platform', 'ACTIVE',
    'platform@thinkerscave.com', '9000000001', NULL,
    'Internal platform account (host for Super Admin)', TRUE, NULL,
    'system', NOW(), 'system', NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO organizations (
    id, organization_code, customer_id, organization_name, short_name, institution_type, board_name,
    email, mobile_number, alternate_mobile_number, website,
    address_line_1, address_line_2, city, state, country, postal_code,
    time_zone, currency, language, logo_url, status, active, onboarding_completed,
    remarks, created_by, created_on, updated_by, updated_on, version
) VALUES (
    1, 'ORG000001', 1, 'ThinkersCave Platform', 'TCP', 'OTHER', NULL,
    'platform@thinkerscave.com', '9000000001', NULL, 'https://thinkerscave.com',
    'Platform HQ', NULL, 'Bhubaneswar', 'Odisha', 'India', '751001',
    'Asia/Kolkata', 'INR', 'en-IN', NULL, 'ACTIVE', TRUE, TRUE,
    'Platform host organization for Super Admin', 'system', NOW(), 'system', NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO organization_configurations (
    id, organization_id, default_academic_year, academic_year_start_month,
    student_code_pattern, employee_code_pattern, admission_number_pattern,
    receipt_number_pattern, invoice_number_pattern,
    currency, time_zone, language, date_format,
    created_by, created_on, updated_by, updated_on, version
) VALUES (
    1, 1, '2026-27', 4,
    'TCP/STU/{YY}/{SEQ}', 'TCP/EMP/{YY}/{SEQ}', 'TCP/ADM/{YY}/{SEQ}',
    'TCP/REC/{YY}/{SEQ}', 'TCP/INV/{YY}/{SEQ}',
    'INR', 'Asia/Kolkata', 'en-IN', 'dd-MM-yyyy',
    'system', NOW(), 'system', NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO security_policies (
    id, organization_id, min_password_length, require_uppercase, require_lowercase, require_numbers,
    require_special_chars, password_expiry_days, password_history_count, max_failed_attempts,
    lockout_duration_minutes, session_timeout_minutes, max_concurrent_sessions, allow_remember_me,
    require_two_factor, active, created_by, created_on, updated_by, updated_on, version
) VALUES (
    1, 1, 8, TRUE, TRUE, TRUE, FALSE, 90, 5, 5, 30, 60, 3, TRUE, FALSE, TRUE,
    'system', NOW(), 'system', NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 2) SYSTEM ROLES
-- =============================================================================

INSERT INTO roles (
    id, role_code, role_name, description, role_type, dashboard_code,
    system_role, active, display_order, created_by, created_on, updated_by, updated_on, version
) VALUES
(6, 'ROLE_SUPER_ADMIN', 'ThinkersCave Super Admin', 'Platform control tower and tenant administration', 'SUPER_ADMIN', 'PLATFORM', TRUE, TRUE, 0, 'system', NOW(), 'system', NOW(), 0),
(1, 'ROLE_OWNER',       'Organization Owner',       'Campus owner with full access',                    'ORGANIZATION_OWNER', 'ADMIN',   TRUE, TRUE, 1, 'system', NOW(), 'system', NOW(), 0),
(2, 'ROLE_ADMIN',       'Organization Admin',       'Campus administrator',                             'ORGANIZATION_ADMIN', 'ADMIN',   TRUE, TRUE, 2, 'system', NOW(), 'system', NOW(), 0),
(3, 'ROLE_STAFF',       'Staff',                    'Teaching / non-teaching staff',                    'STAFF',              'STAFF',   TRUE, TRUE, 3, 'system', NOW(), 'system', NOW(), 0),
(4, 'ROLE_STUDENT',     'Student',                  'Student portal access',                            'STUDENT',            'STUDENT', TRUE, TRUE, 4, 'system', NOW(), 'system', NOW(), 0),
(5, 'ROLE_PARENT',      'Parent',                   'Parent portal access',                             'PARENT',             'PARENT',  TRUE, TRUE, 5, 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 3) PRIVILEGE MASTER (reference catalog)
-- Sidebar authorization uses role_permissions flags; this table is the master list.
-- =============================================================================

INSERT INTO privileges (
    id, privilege_code, privilege_name, description, privilege_type, display_order, active,
    created_by, created_on, updated_by, updated_on, version
) VALUES
(1, 'VIEW',    'View',    'View / read access',           'VIEW',    1, TRUE, 'system', NOW(), 'system', NOW(), 0),
(2, 'MANAGE',  'Manage',  'Create / update / delete',     'MANAGE',  2, TRUE, 'system', NOW(), 'system', NOW(), 0),
(3, 'APPROVE', 'Approve', 'Approval / workflow actions',  'APPROVE', 3, TRUE, 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 4) PLATFORM MENUS (MODULE + PAGE tree used by Super Admin sidebar)
-- Insert parents first (self-FK on parent_menu_id).
-- =============================================================================

INSERT INTO menus (
    id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id,
    display_order, show_in_sidebar, active, default_page,
    created_by, created_on, updated_by, updated_on, version
) VALUES
(23, 'SUBSCRIPTIONS_GROUP',   'Subscriptions',            'Commercial subscription management',     NULL,                                           'credit_card',   'MODULE', NULL, 4, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(24, 'PLATFORM_GROUP',        'Platform',                 'Platform operations and infrastructure', NULL,                                           'server',        'MODULE', NULL, 5, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(14, 'PLATFORM_DASHBOARD',    'Dashboard',                'Platform overview',                      '/app/tenant-management/dashboard',             'dashboard',     'PAGE',   NULL, 1, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(15, 'CUSTOMERS',             'Customers',                'Commercial customer accounts',           '/app/tenant-management/customers',             'groups',        'PAGE',   NULL, 2, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(16, 'TM_ORGANIZATIONS',      'Organizations',            'Tenant organizations',                   '/app/tenant-management/organizations',         'business',      'PAGE',   NULL, 3, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(22, 'AUDIT_CENTER',          'Audit Center',             'Platform audit center',                  '/app/tenant-management/audit-center',          'history',       'PAGE',   NULL, 6, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(17, 'SUBSCRIPTION_PLANS',    'Subscription Plans',       'Platform subscription plans',            '/app/tenant-management/subscription-plans',    'credit_card',   'PAGE',   23,   1, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(18, 'PROMOTIONS',            'Promotions',               'Platform promotions',                    '/app/tenant-management/promotions',            'local_offer',   'PAGE',   23,   2, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(19, 'FEATURE_CATALOG',       'Feature Catalog',          'Platform feature catalogue',             '/app/tenant-management/feature-catalog',       'apps',          'PAGE',   24,   1, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(25, 'PROVISIONING_TEMPLATES','Provisioning Templates',   'Provisioning templates and onboarding',  '/app/tenant-management/organizations/create',  'sliders-h',     'PAGE',   24,   2, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(20, 'TENANT_HEALTH',         'Tenant Health',            'Tenant health monitoring',               '/app/tenant-management/tenant-health',         'monitor_heart', 'PAGE',   24,   3, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(21, 'MIGRATION_CENTER',      'Migration Center',         'Tenant migration jobs',                  '/app/tenant-management/migration-center',      'sync',          'PAGE',   24,   4, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- Keep labels/routes aligned if rows already existed
UPDATE menus SET menu_name = 'Dashboard',               route = '/app/tenant-management/dashboard',             icon = 'dashboard',     menu_type = 'PAGE',   parent_menu_id = NULL, display_order = 1, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'PLATFORM_DASHBOARD';
UPDATE menus SET menu_name = 'Customers',               route = '/app/tenant-management/customers',             icon = 'groups',        menu_type = 'PAGE',   parent_menu_id = NULL, display_order = 2, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'CUSTOMERS';
UPDATE menus SET menu_name = 'Organizations',           route = '/app/tenant-management/organizations',         icon = 'business',      menu_type = 'PAGE',   parent_menu_id = NULL, display_order = 3, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'TM_ORGANIZATIONS';
UPDATE menus SET menu_name = 'Subscriptions',           route = NULL,                                           icon = 'credit_card',   menu_type = 'MODULE', parent_menu_id = NULL, display_order = 4, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'SUBSCRIPTIONS_GROUP';
UPDATE menus SET menu_name = 'Subscription Plans',      route = '/app/tenant-management/subscription-plans',    icon = 'credit_card',   menu_type = 'PAGE',   parent_menu_id = 23,   display_order = 1, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'SUBSCRIPTION_PLANS';
UPDATE menus SET menu_name = 'Promotions',              route = '/app/tenant-management/promotions',            icon = 'local_offer',   menu_type = 'PAGE',   parent_menu_id = 23,   display_order = 2, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'PROMOTIONS';
UPDATE menus SET menu_name = 'Platform',                route = NULL,                                           icon = 'server',        menu_type = 'MODULE', parent_menu_id = NULL, display_order = 5, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'PLATFORM_GROUP';
UPDATE menus SET menu_name = 'Feature Catalog',         route = '/app/tenant-management/feature-catalog',       icon = 'apps',          menu_type = 'PAGE',   parent_menu_id = 24,   display_order = 1, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'FEATURE_CATALOG';
UPDATE menus SET menu_name = 'Provisioning Templates',  route = '/app/tenant-management/organizations/create',  icon = 'sliders-h',     menu_type = 'PAGE',   parent_menu_id = 24,   display_order = 2, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'PROVISIONING_TEMPLATES';
UPDATE menus SET menu_name = 'Tenant Health',           route = '/app/tenant-management/tenant-health',         icon = 'monitor_heart', menu_type = 'PAGE',   parent_menu_id = 24,   display_order = 3, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'TENANT_HEALTH';
UPDATE menus SET menu_name = 'Migration Center',        route = '/app/tenant-management/migration-center',      icon = 'sync',          menu_type = 'PAGE',   parent_menu_id = 24,   display_order = 4, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'MIGRATION_CENTER';
UPDATE menus SET menu_name = 'Audit Center',            route = '/app/tenant-management/audit-center',          icon = 'history',       menu_type = 'PAGE',   parent_menu_id = NULL, display_order = 6, show_in_sidebar = TRUE, active = TRUE WHERE menu_code = 'AUDIT_CENTER';

-- =============================================================================
-- 5) SUPER ADMIN USER + ROLE MAPPING
-- Creates superadmin only when id=1 is missing. Never overwrites an existing password.
-- Bootstrap password for a brand-new row only; align with PlatformBootstrapSeed (admin@123).
-- =============================================================================

INSERT INTO users (
    id, organization_id, user_code, username, email, mobile_number, password,
    first_name, last_name, display_name, profile_image_url, status,
    email_verified, mobile_verified, first_time_login,
    failed_login_attempts, account_locked, last_login_at, password_changed_at, locked_at, lock_expiry_at,
    created_by, created_on, updated_by, updated_on, version
) VALUES (
    1, 1, 'USR000001', 'superadmin', 'superadmin@thinkerscave.com', '9777000001',
    crypt('admin@123', gen_salt('bf', 12)),
    'Super', 'Admin', 'Super Admin', NULL, 'ACTIVE',
    TRUE, TRUE, FALSE,
    0, FALSE, NULL, NOW(), NULL, NULL,
    'system', NOW(), 'system', NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (
    id, user_id, role_id, primary_role, active,
    created_by, created_on, updated_by, updated_on, version
) VALUES (
    1, 1, 6, TRUE, TRUE, 'system', NOW(), 'system', NOW(), 0
)
ON CONFLICT (id) DO UPDATE
SET role_id = 6, primary_role = TRUE, active = TRUE, updated_by = 'system', updated_on = NOW();

-- =============================================================================
-- 6) SUPER_ADMIN MENU PRIVILEGE MAPPING (role_permissions)
-- This is what drives the Super Admin sidebar (can_view / can_manage / can_approve).
-- =============================================================================

DELETE FROM role_permissions WHERE role_id = 6;

INSERT INTO role_permissions (
    organization_id, role_id, menu_id, can_view, can_manage, can_approve,
    created_by, created_on, updated_by, updated_on, version
) VALUES
(1, 6, 14, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 15, TRUE, TRUE, TRUE,  'system', NOW(), 'system', NOW(), 0),
(1, 6, 16, TRUE, TRUE, TRUE,  'system', NOW(), 'system', NOW(), 0),
(1, 6, 23, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 17, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 18, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 24, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 19, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 25, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 20, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 21, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0),
(1, 6, 22, TRUE, TRUE, FALSE, 'system', NOW(), 'system', NOW(), 0);

-- =============================================================================
-- 7) SUBSCRIPTION PLANS + FEATURE CATALOG (needed when creating orgs)
-- =============================================================================

INSERT INTO subscription_plans (
    id, plan_code, plan_name, description,
    monthly_price, quarterly_price, half_yearly_price, yearly_price,
    student_limit, staff_limit, branch_limit, storage_limit_gb, api_request_limit, trial_days,
    display_order, recommended, custom_plan, visible, active, remarks,
    created_by, created_on, updated_by, updated_on, version
) VALUES
(1, 'STARTER',    'Starter Plan',    'Starter plan for a single school', 1499.00, 3999.00, 6999.00, 12999.00, 300, 50, 1, 50, 100000, 14, 1, FALSE, FALSE, TRUE, TRUE, 'Good for a single campus', 'system', NOW(), 'system', NOW(), 0),
(2, 'GROWTH',     'Growth Plan',     'For growing school groups',        2999.00, 7999.00, 13999.00, 25999.00, 1200, 200, 5, 200, 500000, 14, 2, TRUE, FALSE, TRUE, TRUE, 'Recommended for groups', 'system', NOW(), 'system', NOW(), 0),
(3, 'ENTERPRISE', 'Enterprise Plan', 'Large scale multi-campus plan',    9999.00, 24999.00, 44999.00, 89999.00, 5000, 400, 20, 1000, 2000000, 30, 3, FALSE, FALSE, TRUE, TRUE, 'For large institutions', 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO features (
    id, feature_code, feature_name, display_name, module, category, parent_feature_id,
    feature_key, description, icon, display_order, premium_feature, visible, default_enabled, active, remarks,
    created_by, created_on, updated_by, updated_on, version
) VALUES
(1, 'DASHBOARD',   'Dashboard',          'Dashboard',          'platform',   'core', NULL, 'platform.dashboard',      'Platform dashboard',           'dashboard',    1, FALSE, TRUE, TRUE, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(2, 'AUTH',        'Authentication',     'Authentication',     'security',   'core', NULL, 'security.auth',            'Login and session management', 'lock',         2, FALSE, TRUE, TRUE, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(3, 'STUDENT_MGMT','Student Management', 'Student Management', 'student',    'core', NULL, 'student.management',       'Student profile management',   'school',       3, FALSE, TRUE, TRUE, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(4, 'STAFF_MGMT',  'Staff Management',   'Staff Management',   'staff',      'core', NULL, 'staff.management',         'Staff profile management',     'badge',        4, FALSE, TRUE, TRUE, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(5, 'ATTENDANCE',  'Attendance',         'Attendance',         'attendance', 'core', NULL, 'attendance.management',    'Attendance tracking',          'check-circle', 5, FALSE, TRUE, TRUE, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO subscription_plan_features (
    id, subscription_plan_id, feature_id, enabled, mandatory, display_order, notes, active, remarks,
    created_by, created_on, updated_by, updated_on, version
) VALUES
(1,  1, 1, TRUE, TRUE, 1, 'Starter dashboard access', TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(2,  1, 3, TRUE, TRUE, 2, 'Student module enabled',   TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(3,  1, 4, TRUE, TRUE, 3, 'Staff module enabled',     TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(4,  2, 1, TRUE, TRUE, 1, 'Growth dashboard access',  TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(5,  2, 2, TRUE, TRUE, 2, 'Authentication enabled',   TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(6,  2, 3, TRUE, TRUE, 3, 'Student module enabled',   TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(7,  2, 4, TRUE, TRUE, 4, 'Staff module enabled',     TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(8,  2, 5, TRUE, TRUE, 5, 'Attendance enabled',       TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(9,  3, 1, TRUE, TRUE, 1, 'Enterprise dashboard',     TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(10, 3, 2, TRUE, TRUE, 2, 'Authentication enabled',   TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(11, 3, 3, TRUE, TRUE, 3, 'Student module enabled',   TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(12, 3, 4, TRUE, TRUE, 4, 'Staff module enabled',     TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(13, 3, 5, TRUE, TRUE, 5, 'Attendance enabled',       TRUE, NULL, 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 8) PROMOTIONS (optional commercial master)
-- =============================================================================

INSERT INTO promotions (
    id, promotion_code, promotion_name, description, discount_type, discount_value, maximum_discount,
    valid_from, valid_to, maximum_usage, used_count, allow_custom_plan, stackable, auto_apply,
    status, active, remarks, created_by, created_on, updated_by, updated_on, version
) VALUES
(1, 'SUMMER2026',    'Summer Enrollment Offer', '10% off yearly plans for new campuses', 'PERCENTAGE',  10.00, 15000.00, '2026-04-01', '2026-08-31', 100, 0, FALSE, FALSE, TRUE,  'ACTIVE', TRUE, 'Seasonal campaign', 'system', NOW(), 'system', NOW(), 0),
(2, 'ODISHA_LAUNCH', 'Odisha Launch Discount',  'Flat discount for Odisha pilot schools', 'FLAT_AMOUNT', 5000.00, 5000.00, '2026-01-01', '2026-12-31', 50,  0, FALSE, TRUE,  FALSE, 'ACTIVE', TRUE, 'Regional launch offer', 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 9) PROVISIONING TEMPLATES (used when creating organizations)
-- =============================================================================

INSERT INTO provisioning_templates (
    id, template_code, template_name, institution_type, template_version, description,
    academic_structure_enabled, roles_enabled, permissions_enabled, classes_enabled,
    sections_enabled, departments_enabled, designations_enabled, seed_master_data,
    active, remarks, created_by, created_on, updated_by, updated_on, version
) VALUES
(1, 'SCHOOL_DEFAULT',  'Default School Template',  'SCHOOL',  '1.0', 'Standard school onboarding template',  TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(2, 'COLLEGE_DEFAULT', 'Default College Template', 'COLLEGE', '1.0', 'Standard college onboarding template', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO provisioning_template_items (
    id, template_id, item_type, item_key, item_name, item_value, configuration_json,
    mandatory, enabled, display_order, active, remarks,
    created_by, created_on, updated_by, updated_on, version
) VALUES
(1, 1, 'MODULE', 'STUDENT',  'Student Module',  'ENABLED', NULL, TRUE, TRUE, 1, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(2, 1, 'MODULE', 'STAFF',    'Staff Module',    'ENABLED', NULL, TRUE, TRUE, 2, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(3, 1, 'MODULE', 'ATTENDANCE','Attendance Module','ENABLED', NULL, TRUE, TRUE, 3, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(4, 1, 'ROLE',   'ADMIN',    'Organization Admin','ENABLED', NULL, TRUE, TRUE, 4, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(5, 2, 'MODULE', 'STUDENT',  'Student Module',  'ENABLED', NULL, TRUE, TRUE, 1, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(6, 2, 'MODULE', 'STAFF',    'Staff Module',    'ENABLED', NULL, TRUE, TRUE, 2, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0),
(7, 2, 'ROLE',   'ADMIN',    'Organization Admin','ENABLED', NULL, TRUE, TRUE, 3, TRUE, NULL, 'system', NOW(), 'system', NOW(), 0)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 10) CODE SEQUENCE (align with seeded codes so next create does not collide)
-- Column name follows Hibernate @Column(name = "`last_value`") → "last_value"
-- If your DB column is named differently, adjust the quoted identifier below.
-- =============================================================================

INSERT INTO code_sequence (code_type, "last_value", version, created_by, created_on, updated_by, updated_on)
VALUES
  ('CUSTOMER',      1, 0, 'system', NOW(), 'system', NOW()),
  ('CONTACT',       0, 0, 'system', NOW(), 'system', NOW()),
  ('ORGANIZATION',  1, 0, 'system', NOW(), 'system', NOW()),
  ('USER',          1, 0, 'system', NOW(), 'system', NOW()),
  ('PROVISION_JOB', 0, 0, 'system', NOW(), 'system', NOW()),
  ('TENANT',        0, 0, 'system', NOW(), 'system', NOW()),
  ('PROMOTION',     2, 0, 'system', NOW(), 'system', NOW()),
  ('TEMPLATE',      2, 0, 'system', NOW(), 'system', NOW()),
  ('STUDENT',       0, 0, 'system', NOW(), 'system', NOW()),
  ('PARENT',        0, 0, 'system', NOW(), 'system', NOW()),
  ('STAFF',         0, 0, 'system', NOW(), 'system', NOW()),
  ('DOCUMENT',      0, 0, 'system', NOW(), 'system', NOW())
ON CONFLICT (code_type) DO UPDATE
SET "last_value" = GREATEST(code_sequence."last_value", EXCLUDED."last_value"),
    updated_by = 'system',
    updated_on = NOW();

-- =============================================================================
-- 11) RESET IDENTITY SEQUENCES (PostgreSQL)
-- =============================================================================

SELECT setval(pg_get_serial_sequence('customers', 'id'),                  COALESCE((SELECT MAX(id) FROM customers), 1), true);
SELECT setval(pg_get_serial_sequence('organizations', 'id'),              COALESCE((SELECT MAX(id) FROM organizations), 1), true);
SELECT setval(pg_get_serial_sequence('organization_configurations', 'id'),COALESCE((SELECT MAX(id) FROM organization_configurations), 1), true);
SELECT setval(pg_get_serial_sequence('security_policies', 'id'),          COALESCE((SELECT MAX(id) FROM security_policies), 1), true);
SELECT setval(pg_get_serial_sequence('roles', 'id'),                      COALESCE((SELECT MAX(id) FROM roles), 1), true);
SELECT setval(pg_get_serial_sequence('privileges', 'id'),                 COALESCE((SELECT MAX(id) FROM privileges), 1), true);
SELECT setval(pg_get_serial_sequence('menus', 'id'),                      COALESCE((SELECT MAX(id) FROM menus), 1), true);
SELECT setval(pg_get_serial_sequence('users', 'id'),                      COALESCE((SELECT MAX(id) FROM users), 1), true);
SELECT setval(pg_get_serial_sequence('user_roles', 'id'),                 COALESCE((SELECT MAX(id) FROM user_roles), 1), true);
SELECT setval(pg_get_serial_sequence('role_permissions', 'id'),           COALESCE((SELECT MAX(id) FROM role_permissions), 1), true);
SELECT setval(pg_get_serial_sequence('subscription_plans', 'id'),         COALESCE((SELECT MAX(id) FROM subscription_plans), 1), true);
SELECT setval(pg_get_serial_sequence('features', 'id'),                   COALESCE((SELECT MAX(id) FROM features), 1), true);
SELECT setval(pg_get_serial_sequence('subscription_plan_features', 'id'), COALESCE((SELECT MAX(id) FROM subscription_plan_features), 1), true);
SELECT setval(pg_get_serial_sequence('promotions', 'id'),                 COALESCE((SELECT MAX(id) FROM promotions), 1), true);
SELECT setval(pg_get_serial_sequence('provisioning_templates', 'id'),     COALESCE((SELECT MAX(id) FROM provisioning_templates), 1), true);
SELECT setval(pg_get_serial_sequence('provisioning_template_items', 'id'),COALESCE((SELECT MAX(id) FROM provisioning_template_items), 1), true);

COMMIT;

-- =============================================================================
-- VERIFY (optional — run after seed)
-- =============================================================================
-- SELECT username, email, status FROM users WHERE username = 'superadmin';
-- SELECT r.role_code, r.role_type FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = 1;
-- SELECT m.menu_code, m.menu_name, rp.can_view, rp.can_manage
--   FROM role_permissions rp JOIN menus m ON m.id = rp.menu_id
--  WHERE rp.role_id = 6 ORDER BY m.display_order, m.id;
