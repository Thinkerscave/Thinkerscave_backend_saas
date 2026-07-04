-- =============================================================================
-- PHASE 4: Customer Management module — dynamic platform menus for SUPER_ADMIN
-- Idempotent — safe to re-run via DevDataInitializer
-- =============================================================================

UPDATE menus SET menu_name = 'Platform Management', route = '/app/tenant-management/dashboard', icon = 'shield', menu_type = 'MODULE'
WHERE menu_code = 'PLATFORM';

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(14, 'PLATFORM_DASHBOARD',   'Dashboard',              'Platform overview',              '/app/tenant-management/dashboard',              'dashboard',       'PAGE',   8,  1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(15, 'CUSTOMERS',            'Customers',              'Commercial customer accounts',   '/app/tenant-management/customers',              'groups',          'PAGE',   8,  2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(16, 'TM_ORGANIZATIONS',     'Organizations',          'Tenant organizations',           '/app/tenant-management/organizations',          'business',        'PAGE',   8,  3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(17, 'SUBSCRIPTION_PLANS',   'Subscription Plans',     'Platform subscription plans',    '/app/tenant-management/subscription-plans',     'credit_card',     'PAGE',   8,  4, TRUE, TRUE, FALSE, 'system', 'system', 0),
(18, 'PROMOTIONS',           'Promotions',             'Platform promotions',            '/app/tenant-management/promotions',             'local_offer',     'PAGE',   8,  5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(19, 'FEATURE_CATALOG',      'Feature Catalog',        'Platform feature catalogue',     '/app/tenant-management/feature-catalog',        'apps',            'PAGE',   8,  6, TRUE, TRUE, FALSE, 'system', 'system', 0),
(20, 'TENANT_HEALTH',        'Tenant Health',          'Tenant health monitoring',       '/app/tenant-management/tenant-health',          'monitor_heart',   'PAGE',   8,  7, TRUE, TRUE, FALSE, 'system', 'system', 0),
(21, 'MIGRATION_CENTER',     'Migration Center',       'Tenant migration jobs',          '/app/tenant-management/migration-center',       'sync',            'PAGE',   8,  8, TRUE, TRUE, FALSE, 'system', 'system', 0),
(22, 'AUDIT_CENTER',         'Audit Center',           'Platform audit center',          '/app/tenant-management/audit-center',           'history',         'PAGE',   8,  9, TRUE, TRUE, FALSE, 'system', 'system', 0);

-- SUPER_ADMIN: platform module tree only (dynamic sidebar from DB)
DELETE FROM role_permissions WHERE role_id = 6;

INSERT IGNORE INTO role_permissions (id, organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(100, 1, 6, 1,  TRUE, FALSE, FALSE, 'system', 'system', 0),
(101, 1, 6, 8,  TRUE, TRUE,  TRUE,  'system', 'system', 0),
(103, 1, 6, 14, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(104, 1, 6, 15, TRUE, TRUE,  TRUE,  'system', 'system', 0),
(105, 1, 6, 16, TRUE, TRUE,  TRUE,  'system', 'system', 0),
(106, 1, 6, 17, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(107, 1, 6, 18, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(108, 1, 6, 19, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(109, 1, 6, 20, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(110, 1, 6, 21, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(111, 1, 6, 22, TRUE, TRUE,  FALSE, 'system', 'system', 0);
