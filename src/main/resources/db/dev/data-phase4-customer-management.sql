-- =============================================================================
-- PHASE 4: Customer Management module — dynamic platform menus for SUPER_ADMIN
-- Idempotent — safe to re-run via DevDataInitializer
-- =============================================================================

UPDATE menus SET menu_name = 'Platform Management', route = '/app/tenant-management/dashboard', icon = 'shield', menu_type = 'MODULE', show_in_sidebar = FALSE
WHERE menu_code = 'PLATFORM';

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(14, 'PLATFORM_DASHBOARD',   'Dashboard',               'Platform overview',                    '/app/tenant-management/dashboard',        'dashboard',     'PAGE',   NULL, 1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(15, 'CUSTOMERS',            'Customers',               'Commercial customer accounts',         '/app/tenant-management/customers',        'groups',        'PAGE',   NULL, 2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(16, 'TM_ORGANIZATIONS',     'Organizations',           'Tenant organizations',                 '/app/tenant-management/organizations',    'business',      'PAGE',   NULL, 3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(17, 'SUBSCRIPTION_PLANS',   'Subscription Plans',      'Platform subscription plans',          '/app/tenant-management/subscription-plans', 'credit_card', 'PAGE',   23,   1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(18, 'PROMOTIONS',           'Promotions',              'Platform promotions',                  '/app/tenant-management/promotions',       'local_offer',   'PAGE',   23,   2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(19, 'FEATURE_CATALOG',      'Feature Catalog',         'Platform feature catalogue',           '/app/tenant-management/feature-catalog',  'apps',          'PAGE',   24,   1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(20, 'TENANT_HEALTH',        'Tenant Health',           'Tenant health monitoring',             '/app/tenant-management/tenant-health',    'monitor_heart', 'PAGE',   24,   3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(21, 'MIGRATION_CENTER',     'Migration Center',        'Tenant migration jobs',                '/app/tenant-management/migration-center', 'sync',          'PAGE',   24,   4, TRUE, TRUE, FALSE, 'system', 'system', 0),
(22, 'AUDIT_CENTER',         'Audit Center',            'Platform audit center',                '/app/tenant-management/audit-center',     'history',       'PAGE',   NULL, 6, TRUE, TRUE, FALSE, 'system', 'system', 0),
(23, 'SUBSCRIPTIONS_GROUP',  'Subscriptions',           'Commercial subscription management',    NULL,                                      'credit_card',   'MODULE', NULL, 4, TRUE, TRUE, FALSE, 'system', 'system', 0),
(24, 'PLATFORM_GROUP',       'Platform',                'Platform operations and infrastructure', NULL,                                     'server',        'MODULE', NULL, 5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(25, 'PROVISIONING_TEMPLATES','Provisioning Templates', 'Provisioning templates and onboarding', '/app/tenant-management/organizations/create', 'sliders-h', 'PAGE', 24, 2, TRUE, TRUE, FALSE, 'system', 'system', 0);

UPDATE menus SET menu_name = 'Dashboard', route = '/app/tenant-management/dashboard', icon = 'dashboard', menu_type = 'PAGE', parent_menu_id = NULL, display_order = 1, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'PLATFORM_DASHBOARD';

UPDATE menus SET menu_name = 'Customers', route = '/app/tenant-management/customers', icon = 'groups', menu_type = 'PAGE', parent_menu_id = NULL, display_order = 2, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'CUSTOMERS';

UPDATE menus SET menu_name = 'Organizations', route = '/app/tenant-management/organizations', icon = 'business', menu_type = 'PAGE', parent_menu_id = NULL, display_order = 3, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'TM_ORGANIZATIONS';

UPDATE menus SET menu_name = 'Subscriptions', route = NULL, icon = 'credit_card', menu_type = 'MODULE', parent_menu_id = NULL, display_order = 4, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'SUBSCRIPTIONS_GROUP';

UPDATE menus SET menu_name = 'Subscription Plans', route = '/app/tenant-management/subscription-plans', icon = 'credit_card', menu_type = 'PAGE', parent_menu_id = 23, display_order = 1, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'SUBSCRIPTION_PLANS';

UPDATE menus SET menu_name = 'Promotions', route = '/app/tenant-management/promotions', icon = 'local_offer', menu_type = 'PAGE', parent_menu_id = 23, display_order = 2, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'PROMOTIONS';

UPDATE menus SET menu_name = 'Platform', route = NULL, icon = 'server', menu_type = 'MODULE', parent_menu_id = NULL, display_order = 5, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'PLATFORM_GROUP';

UPDATE menus SET menu_name = 'Feature Catalog', route = '/app/tenant-management/feature-catalog', icon = 'apps', menu_type = 'PAGE', parent_menu_id = 24, display_order = 1, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'FEATURE_CATALOG';

UPDATE menus SET menu_name = 'Provisioning Templates', route = '/app/tenant-management/organizations/create', icon = 'sliders-h', menu_type = 'PAGE', parent_menu_id = 24, display_order = 2, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'PROVISIONING_TEMPLATES';

UPDATE menus SET menu_name = 'Tenant Health', route = '/app/tenant-management/tenant-health', icon = 'monitor_heart', menu_type = 'PAGE', parent_menu_id = 24, display_order = 3, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'TENANT_HEALTH';

UPDATE menus SET menu_name = 'Migration Center', route = '/app/tenant-management/migration-center', icon = 'sync', menu_type = 'PAGE', parent_menu_id = 24, display_order = 4, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'MIGRATION_CENTER';

UPDATE menus SET menu_name = 'Audit Center', route = '/app/tenant-management/audit-center', icon = 'history', menu_type = 'PAGE', parent_menu_id = NULL, display_order = 6, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'AUDIT_CENTER';

-- SUPER_ADMIN: platform module tree only (dynamic sidebar from DB)
DELETE FROM role_permissions WHERE role_id = 6;

INSERT IGNORE INTO role_permissions (organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
VALUES
(1, 6, 14, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 15, TRUE, TRUE,  TRUE,  'system', 'system', 0),
(1, 6, 16, TRUE, TRUE,  TRUE,  'system', 'system', 0),
(1, 6, 23, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 17, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 18, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 24, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 19, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 25, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 20, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 21, TRUE, TRUE,  FALSE, 'system', 'system', 0),
(1, 6, 22, TRUE, TRUE,  FALSE, 'system', 'system', 0);
