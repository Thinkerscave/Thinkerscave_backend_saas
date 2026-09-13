-- =============================================================================
-- PHASE 4: Customer Management module — dynamic platform menus for SUPER_ADMIN
-- Idempotent — safe to re-run via DevDataInitializer
-- Canonical hierarchy (no SUBSCRIPTIONS_GROUP / PLATFORM_GROUP duplicates):
--   Subscription Management → Plans, Promo Codes
--   Tenant Management → Health, Migration, Audit
-- =============================================================================

UPDATE menus SET menu_name = 'Platform Management', route = '/app/tenant-management/dashboard', icon = 'shield', menu_type = 'MODULE', show_in_sidebar = FALSE
WHERE menu_code = 'PLATFORM';

INSERT IGNORE INTO menus (id, menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, created_by, updated_by, version)
VALUES
(26, 'SUBSCRIPTION_MANAGEMENT', 'Subscription Management', 'Subscription plans and promo codes', NULL, 'pi pi-credit-card', 'MODULE', NULL, 3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(27, 'TENANT_MANAGEMENT',       'Tenant Management',       'Tenant health, migrations and audit', NULL, 'pi pi-server', 'MODULE', NULL, 5, TRUE, TRUE, FALSE, 'system', 'system', 0),
(28, 'PLATFORM_CATALOG',        'Platform Catalog',        'Menus and feature catalogue', NULL, 'pi pi-th-large', 'MODULE', NULL, 6, TRUE, TRUE, FALSE, 'system', 'system', 0),
(14, 'DASHBOARD',               'Dashboard',               'Role-based workspace home', '/app', 'pi pi-home', 'PAGE', NULL, 1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(15, 'CUSTOMERS',               'Customers',               'Commercial customer accounts', '/app/tenant-management/customers', 'pi pi-users', 'PAGE', NULL, 2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(16, 'TM_ORGANIZATIONS',        'Organizations',           'Tenant organizations', '/app/tenant-management/organizations', 'pi pi-building', 'PAGE', NULL, 3, TRUE, TRUE, FALSE, 'system', 'system', 0),
(17, 'SUBSCRIPTION_PLANS',      'Subscription Plans',      'Platform subscription plans', '/app/tenant-management/subscription-plans', 'pi pi-credit-card', 'PAGE', 26, 1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(18, 'PROMOTIONS',              'Promo Codes',             'Platform promo codes', '/app/tenant-management/promotions', 'pi pi-tag', 'PAGE', 26, 2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(19, 'FEATURE_CATALOG',         'Feature Catalog',         'Platform feature catalogue', '/app/tenant-management/feature-catalog', 'pi pi-box', 'PAGE', 28, 2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(20, 'TENANT_HEALTH',           'Tenant Health',           'Tenant health monitoring', '/app/tenant-management/tenant-health', 'pi pi-heart', 'PAGE', 27, 1, TRUE, TRUE, FALSE, 'system', 'system', 0),
(21, 'MIGRATION_CENTER',        'Migration Center',        'Tenant migration jobs', '/app/tenant-management/migration-center', 'pi pi-sync', 'PAGE', 27, 2, TRUE, TRUE, FALSE, 'system', 'system', 0),
(22, 'AUDIT_CENTER',            'Audit Center',            'Platform audit center', '/app/tenant-management/audit-center', 'pi pi-history', 'PAGE', 27, 3, TRUE, TRUE, FALSE, 'system', 'system', 0);

UPDATE menus SET menu_name = 'Dashboard', route = '/app', icon = 'pi pi-home', menu_type = 'PAGE', parent_menu_id = NULL, display_order = 1, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'DASHBOARD';

UPDATE menus SET menu_name = 'Customers', route = '/app/tenant-management/customers', icon = 'pi pi-users', menu_type = 'PAGE', parent_menu_id = NULL, display_order = 2, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'CUSTOMERS';

UPDATE menus SET menu_name = 'Organizations', route = '/app/tenant-management/organizations', icon = 'pi pi-building', menu_type = 'PAGE', parent_menu_id = NULL, display_order = 3, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'TM_ORGANIZATIONS';

UPDATE menus SET menu_name = 'Subscription Management', route = NULL, icon = 'pi pi-credit-card', menu_type = 'MODULE', parent_menu_id = NULL, display_order = 3, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'SUBSCRIPTION_MANAGEMENT';

UPDATE menus SET menu_name = 'Subscription Plans', route = '/app/tenant-management/subscription-plans', icon = 'pi pi-credit-card', menu_type = 'PAGE', parent_menu_id = 26, display_order = 1, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'SUBSCRIPTION_PLANS';

UPDATE menus SET menu_name = 'Promo Codes', route = '/app/tenant-management/promotions', icon = 'pi pi-tag', menu_type = 'PAGE', parent_menu_id = 26, display_order = 2, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'PROMOTIONS';

UPDATE menus SET menu_name = 'Tenant Management', route = NULL, icon = 'pi pi-server', menu_type = 'MODULE', parent_menu_id = NULL, display_order = 5, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'TENANT_MANAGEMENT';

UPDATE menus SET menu_name = 'Tenant Health', route = '/app/tenant-management/tenant-health', icon = 'pi pi-heart', menu_type = 'PAGE', parent_menu_id = 27, display_order = 1, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'TENANT_HEALTH';

UPDATE menus SET menu_name = 'Migration Center', route = '/app/tenant-management/migration-center', icon = 'pi pi-sync', menu_type = 'PAGE', parent_menu_id = 27, display_order = 2, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'MIGRATION_CENTER';

UPDATE menus SET menu_name = 'Audit Center', route = '/app/tenant-management/audit-center', icon = 'pi pi-history', menu_type = 'PAGE', parent_menu_id = 27, display_order = 3, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'AUDIT_CENTER';

UPDATE menus SET menu_name = 'Platform Catalog', route = NULL, icon = 'pi pi-th-large', menu_type = 'MODULE', parent_menu_id = NULL, display_order = 6, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'PLATFORM_CATALOG';

UPDATE menus SET menu_name = 'Feature Catalog', route = '/app/tenant-management/feature-catalog', icon = 'pi pi-box', menu_type = 'PAGE', parent_menu_id = 28, display_order = 2, show_in_sidebar = TRUE, active = TRUE
WHERE menu_code = 'FEATURE_CATALOG';

-- Hide / deactivate legacy duplicate roots if present from older seeds
UPDATE menus SET active = FALSE, show_in_sidebar = FALSE
WHERE menu_code IN ('SUBSCRIPTIONS_GROUP', 'PLATFORM_GROUP', 'PLATFORM_DASHBOARD', 'PROVISIONING_TEMPLATES');

-- SUPER_ADMIN: platform module tree only (dynamic sidebar from DB)
DELETE FROM role_permissions WHERE role_id = 6;

INSERT IGNORE INTO role_permissions (organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, updated_by, version)
SELECT 1, 6, id, TRUE, TRUE,
       CASE WHEN menu_code IN ('CUSTOMERS', 'TM_ORGANIZATIONS', 'FEATURE_CATALOG') THEN TRUE ELSE FALSE END,
       'system', 'system', 0
FROM menus
WHERE menu_code IN (
    'DASHBOARD', 'CUSTOMERS', 'TM_ORGANIZATIONS',
    'SUBSCRIPTION_MANAGEMENT', 'SUBSCRIPTION_PLANS', 'PROMOTIONS',
    'TENANT_MANAGEMENT', 'TENANT_HEALTH', 'MIGRATION_CENTER', 'AUDIT_CENTER',
    'PLATFORM_CATALOG', 'FEATURE_CATALOG'
);
