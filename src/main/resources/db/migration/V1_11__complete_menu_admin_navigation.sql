-- ============================================================================
-- Migration: V1_11__complete_menu_admin_navigation.sql
-- Purpose: Ensure menu administration pages are available in dynamic navigation.
-- ============================================================================

INSERT INTO sub_menu_master (
    sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url,
    sub_menu_icon, sub_menu_order, is_active, menu_id
)
SELECT 'Sub Menu Management', 'ADMIN_SUB_MENU', 'Sub-menu configuration', '/app/manage-sub-menu',
       'pi pi-list-check', 2, TRUE, menu_id
FROM menu_master
WHERE menu_code = 'MENU_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'ADMIN_SUB_MENU');

INSERT INTO sub_menu_master (
    sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url,
    sub_menu_icon, sub_menu_order, is_active, menu_id
)
SELECT 'Menu Sequence Management', 'ADMIN_MENU_SEQUENCE', 'Menu and sub-menu ordering', '/app/menu-sequence',
       'pi pi-sort-alt', 3, TRUE, menu_id
FROM menu_master
WHERE menu_code = 'MENU_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'ADMIN_MENU_SEQUENCE');

INSERT INTO sub_menu_master (
    sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url,
    sub_menu_icon, sub_menu_order, is_active, menu_id
)
SELECT 'Role Menu Mapping', 'ADMIN_ROLE_MENU_MAPPING', 'Role-based menu and privilege mapping', '/app/role-menu-mapping',
       'pi pi-lock', 4, TRUE, menu_id
FROM menu_master
WHERE menu_code = 'MENU_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'ADMIN_ROLE_MENU_MAPPING');

UPDATE sub_menu_master SET sub_menu_order = 1 WHERE sub_menu_code = 'ADMIN_MENU';
UPDATE sub_menu_master SET sub_menu_order = 5 WHERE sub_menu_code = 'ADMIN_ROLE';
UPDATE sub_menu_master SET sub_menu_order = 6 WHERE sub_menu_code = 'ADMIN_ORG';

INSERT INTO submenu_privilege_mapping (sub_menu_id, privilege_id)
SELECT sm.sub_menu_id, p.privilege_id
FROM sub_menu_master sm
CROSS JOIN privilege_master p
WHERE sm.sub_menu_code IN ('ADMIN_SUB_MENU', 'ADMIN_MENU_SEQUENCE', 'ADMIN_ROLE_MENU_MAPPING')
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
  AND sm.sub_menu_code IN ('ADMIN_SUB_MENU', 'ADMIN_MENU_SEQUENCE', 'ADMIN_ROLE_MENU_MAPPING')
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
  AND sm.sub_menu_code IN ('ADMIN_SUB_MENU', 'ADMIN_MENU_SEQUENCE', 'ADMIN_ROLE_MENU_MAPPING')
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