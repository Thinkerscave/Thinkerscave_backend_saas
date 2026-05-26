-- ============================================================================
-- Migration: V1_12__simplify_administration_navigation.sql
-- Purpose: Replace legacy technical admin entries with business-friendly IA.
-- ============================================================================

UPDATE sub_menu_master
SET sub_menu_name = 'Navigation & Access',
    sub_menu_code = 'ADMIN_NAV_ACCESS',
    sub_menu_description = 'Menu structure, roles, and permissions',
    sub_menu_url = '/app/navigation-access',
    sub_menu_icon = 'pi pi-lock',
    sub_menu_order = 1,
    is_active = TRUE
WHERE sub_menu_code = 'ADMIN_MENU';

UPDATE sub_menu_master
SET sub_menu_name = 'Organization Management',
    sub_menu_description = 'Organization settings',
    sub_menu_url = '/app/organization-registration',
    sub_menu_icon = 'pi pi-building',
    sub_menu_order = 2,
    is_active = TRUE
WHERE sub_menu_code = 'ADMIN_ORG';

UPDATE sub_menu_master
SET sub_menu_name = 'System Settings',
    sub_menu_code = 'ADMIN_SYSTEM_SETTINGS',
    sub_menu_description = 'Workspace preferences and system configuration',
    sub_menu_url = '/app/system-settings',
    sub_menu_icon = 'pi pi-sliders-h',
    sub_menu_order = 3,
    is_active = TRUE
WHERE sub_menu_code = 'ADMIN_SUB_MENU';

UPDATE sub_menu_master
SET sub_menu_name = 'Audit & Activity',
    sub_menu_code = 'ADMIN_AUDIT_ACTIVITY',
    sub_menu_description = 'Administration activity and change history',
    sub_menu_url = '/app/audit-activity',
    sub_menu_icon = 'pi pi-history',
    sub_menu_order = 4,
    is_active = TRUE
WHERE sub_menu_code = 'ADMIN_MENU_SEQUENCE';

UPDATE sub_menu_master
SET is_active = FALSE,
    sub_menu_description = 'Legacy entry retained inactive after consolidation'
WHERE sub_menu_code IN ('ADMIN_ROLE', 'ADMIN_ROLE_MENU_MAPPING');

INSERT INTO submenu_privilege_mapping (sub_menu_id, privilege_id)
SELECT sm.sub_menu_id, p.privilege_id
FROM sub_menu_master sm
CROSS JOIN privilege_master p
WHERE sm.sub_menu_code IN ('ADMIN_NAV_ACCESS', 'ADMIN_ORG', 'ADMIN_SYSTEM_SETTINGS', 'ADMIN_AUDIT_ACTIVITY')
  AND UPPER(p.privilege_name) IN ('VIEW', 'ADD', 'EDIT', 'DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM submenu_privilege_mapping spm
      WHERE spm.sub_menu_id = sm.sub_menu_id AND spm.privilege_id = p.privilege_id
  );

INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
SELECT r.role_id, sm.sub_menu_id, p.privilege_id
FROM role_master r
CROSS JOIN sub_menu_master sm
CROSS JOIN privilege_master p
WHERE UPPER(r.role_code) = 'SUPER_ADMIN'
  AND sm.sub_menu_code IN ('ADMIN_NAV_ACCESS', 'ADMIN_ORG', 'ADMIN_SYSTEM_SETTINGS', 'ADMIN_AUDIT_ACTIVITY')
  AND UPPER(p.privilege_name) IN ('VIEW', 'ADD', 'EDIT', 'DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM role_submenu_privilege_mapping rpm
      WHERE rpm.role_id = r.role_id
        AND rpm.sub_menu_id = sm.sub_menu_id
        AND rpm.privilege_id = p.privilege_id
  );

INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
SELECT r.role_id, sm.sub_menu_id, p.privilege_id
FROM role_master r
CROSS JOIN sub_menu_master sm
CROSS JOIN privilege_master p
WHERE UPPER(r.role_code) = 'ADMIN'
  AND sm.sub_menu_code IN ('ADMIN_NAV_ACCESS', 'ADMIN_SYSTEM_SETTINGS', 'ADMIN_AUDIT_ACTIVITY')
  AND UPPER(p.privilege_name) IN ('VIEW', 'EDIT')
  AND NOT EXISTS (
      SELECT 1 FROM role_submenu_privilege_mapping rpm
      WHERE rpm.role_id = r.role_id
        AND rpm.sub_menu_id = sm.sub_menu_id
        AND rpm.privilege_id = p.privilege_id
  );

-- ============================================================================
-- Migration complete
-- ============================================================================