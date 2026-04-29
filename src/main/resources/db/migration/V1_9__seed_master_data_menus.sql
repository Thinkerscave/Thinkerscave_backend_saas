-- ============================================================================
-- Migration: V1_9__seed_master_data_menus.sql
--
-- Purpose: Add menus for the 4 new Master Data management pages:
--          Branch, Department, Class, Section
--
-- Tables used (from JPA @Entity annotations):
--   - menu_master        (@Table(name="menu_master"))
--   - sub_menu_master    (@Table(name="sub_menu_master"))
--   - role_master        (@Entity(name="role_master"))
--   - privilege_master   (@Table(name="privilege_master"))
--   - role_submenu_privilege_mapping  (@Table(name="role_submenu_privilege_mapping"))
-- ============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- STEP 1: Insert parent menu "Master Setup"
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO menu_master (menu_code, name, description, url, icon, menu_order, is_active)
SELECT 'MENU_MASTER_SETUP',
       'Master Setup',
       'Manage master data: branches, departments, classes and sections',
       NULL,
       'pi pi-cog',
       90,
       TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- STEP 2: Insert 4 sub-menus (routerLinks are bare paths — backend prepends /app/)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sub_menu_master (sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url, sub_menu_icon, sub_menu_order, is_active, menu_id)
SELECT 'Manage Branches','SUBMENU_MANAGE_BRANCH','Create and manage branch locations','manage-branch','pi pi-building',1,TRUE,
       (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_BRANCH');

INSERT INTO sub_menu_master (sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url, sub_menu_icon, sub_menu_order, is_active, menu_id)
SELECT 'Manage Departments','SUBMENU_MANAGE_DEPARTMENT','Create and manage departments','manage-department','pi pi-sitemap',2,TRUE,
       (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_DEPARTMENT');

INSERT INTO sub_menu_master (sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url, sub_menu_icon, sub_menu_order, is_active, menu_id)
SELECT 'Manage Classes','SUBMENU_MANAGE_CLASS','Create and manage academic classes','manage-class','pi pi-book',3,TRUE,
       (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_CLASS');

INSERT INTO sub_menu_master (sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url, sub_menu_icon, sub_menu_order, is_active, menu_id)
SELECT 'Manage Sections','SUBMENU_MANAGE_SECTION','Create and manage class sections','manage-section','pi pi-list',4,TRUE,
       (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_SECTION');

-- ─────────────────────────────────────────────────────────────────────────────
-- STEP 3: Map sub-menus to SUPER_ADMIN and ADMIN roles
--         Uses correct table names from JPA @Entity / @Table annotations:
--           role_master.role_id, role_master.role_name
--           privilege_master.privilege_id, privilege_master.privilege_name
--           role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    v_view_privilege_id BIGINT;
    v_super_admin_id    BIGINT;
    v_admin_id          BIGINT;
    v_branch_sm_id      BIGINT;
    v_dept_sm_id        BIGINT;
    v_class_sm_id       BIGINT;
    v_section_sm_id     BIGINT;
BEGIN
    -- Look up VIEW privilege from privilege_master (try common name variants)
    SELECT privilege_id INTO v_view_privilege_id
    FROM privilege_master
    WHERE UPPER(privilege_name) IN ('VIEW','READ','FULL_ACCESS','ALL')
    ORDER BY privilege_id ASC
    LIMIT 1;

    -- Look up role IDs from role_master (correct table name from @Entity(name="role_master"))
    SELECT role_id INTO v_super_admin_id
    FROM role_master WHERE UPPER(role_name) IN ('SUPER_ADMIN','SUPERADMIN') LIMIT 1;

    SELECT role_id INTO v_admin_id
    FROM role_master WHERE UPPER(role_name) = 'ADMIN' LIMIT 1;

    -- Look up the 4 new sub-menu IDs
    SELECT sub_menu_id INTO v_branch_sm_id  FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_BRANCH';
    SELECT sub_menu_id INTO v_dept_sm_id    FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_DEPARTMENT';
    SELECT sub_menu_id INTO v_class_sm_id   FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_CLASS';
    SELECT sub_menu_id INTO v_section_sm_id FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_SECTION';

    IF v_view_privilege_id IS NULL THEN
        RAISE NOTICE 'No VIEW/READ privilege found in privilege_master. Add role mappings manually via the Role Menu Mapping UI.';
        RETURN;
    END IF;

    -- Map for SUPER_ADMIN
    IF v_super_admin_id IS NOT NULL THEN
        INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
        SELECT v_super_admin_id, sm_id, v_view_privilege_id
        FROM (VALUES (v_branch_sm_id),(v_dept_sm_id),(v_class_sm_id),(v_section_sm_id)) t(sm_id)
        WHERE sm_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM role_submenu_privilege_mapping
              WHERE role_id = v_super_admin_id AND sub_menu_id = t.sm_id AND privilege_id = v_view_privilege_id
          );
        RAISE NOTICE 'Mapped Master Setup menus to SUPER_ADMIN.';
    ELSE
        RAISE NOTICE 'SUPER_ADMIN role not found in role_master. Map manually via UI.';
    END IF;

    -- Map for ADMIN
    IF v_admin_id IS NOT NULL THEN
        INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
        SELECT v_admin_id, sm_id, v_view_privilege_id
        FROM (VALUES (v_branch_sm_id),(v_dept_sm_id),(v_class_sm_id),(v_section_sm_id)) t(sm_id)
        WHERE sm_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM role_submenu_privilege_mapping
              WHERE role_id = v_admin_id AND sub_menu_id = t.sm_id AND privilege_id = v_view_privilege_id
          );
        RAISE NOTICE 'Mapped Master Setup menus to ADMIN.';
    ELSE
        RAISE NOTICE 'ADMIN role not found in role_master. Map manually via UI.';
    END IF;
END $$;

-- ============================================================================
-- Migration complete
-- ============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- STEP 1: Insert parent menu "Master Setup"
--         Only if it does not already exist (idempotent)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO menu_master (menu_code, name, description, url, icon, menu_order, is_active)
SELECT 'MENU_MASTER_SETUP',
       'Master Setup',
       'Manage master data: branches, departments, classes and sections',
       NULL,
       'pi pi-cog',
       90,
       TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- STEP 2: Insert sub-menus under "Master Setup"
-- ─────────────────────────────────────────────────────────────────────────────

-- 2a. Manage Branches
INSERT INTO sub_menu_master (
    sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url,
    sub_menu_icon, sub_menu_order, is_active, menu_id
)
SELECT
    'Manage Branches',
    'SUBMENU_MANAGE_BRANCH',
    'Create and manage branch locations',
    'manage-branch',
    'pi pi-building',
    1,
    TRUE,
    (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (
    SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_BRANCH'
);

-- 2b. Manage Departments
INSERT INTO sub_menu_master (
    sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url,
    sub_menu_icon, sub_menu_order, is_active, menu_id
)
SELECT
    'Manage Departments',
    'SUBMENU_MANAGE_DEPARTMENT',
    'Create and manage departments',
    'manage-department',
    'pi pi-sitemap',
    2,
    TRUE,
    (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (
    SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_DEPARTMENT'
);

-- 2c. Manage Classes
INSERT INTO sub_menu_master (
    sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url,
    sub_menu_icon, sub_menu_order, is_active, menu_id
)
SELECT
    'Manage Classes',
    'SUBMENU_MANAGE_CLASS',
    'Create and manage academic classes / grade groups',
    'manage-class',
    'pi pi-book',
    3,
    TRUE,
    (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (
    SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_CLASS'
);

-- 2d. Manage Sections
INSERT INTO sub_menu_master (
    sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url,
    sub_menu_icon, sub_menu_order, is_active, menu_id
)
SELECT
    'Manage Sections',
    'SUBMENU_MANAGE_SECTION',
    'Create and manage class sections / divisions',
    'manage-section',
    'pi pi-list',
    4,
    TRUE,
    (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_MASTER_SETUP')
WHERE NOT EXISTS (
    SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_SECTION'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- STEP 3: Map sub-menus to SUPER_ADMIN and ADMIN roles with VIEW privilege
--         Uses role names and privilege names as they exist in the tenant schema.
--         Skips mapping if it already exists (idempotent).
-- ─────────────────────────────────────────────────────────────────────────────

-- Helper: get the VIEW privilege id (assumed to exist from earlier migrations)
-- Roles: SUPER_ADMIN, ADMIN

DO $$
DECLARE
    v_view_privilege_id BIGINT;
    v_super_admin_id    BIGINT;
    v_admin_id          BIGINT;
    v_branch_sm_id      BIGINT;
    v_dept_sm_id        BIGINT;
    v_class_sm_id       BIGINT;
    v_section_sm_id     BIGINT;
BEGIN
    -- Look up the VIEW privilege (try common names)
    SELECT privilege_id INTO v_view_privilege_id
    FROM privilege_master
    WHERE UPPER(privilege_name) IN ('VIEW', 'READ', 'FULL_ACCESS')
    ORDER BY privilege_id ASC
    LIMIT 1;

    -- Look up role IDs
    SELECT role_id INTO v_super_admin_id FROM roles WHERE UPPER(role_name) = 'SUPER_ADMIN' LIMIT 1;
    SELECT role_id INTO v_admin_id       FROM roles WHERE UPPER(role_name) = 'ADMIN'       LIMIT 1;

    -- Look up sub-menu IDs
    SELECT sub_menu_id INTO v_branch_sm_id  FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_BRANCH';
    SELECT sub_menu_id INTO v_dept_sm_id    FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_DEPARTMENT';
    SELECT sub_menu_id INTO v_class_sm_id   FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_CLASS';
    SELECT sub_menu_id INTO v_section_sm_id FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_SECTION';

    -- Only proceed if all lookups succeeded
    IF v_view_privilege_id IS NULL OR v_super_admin_id IS NULL OR v_admin_id IS NULL THEN
        RAISE NOTICE 'Skipping role-menu mapping: VIEW privilege or ADMIN/SUPER_ADMIN role not found. Map roles manually via UI.';
        RETURN;
    END IF;

    -- Insert mappings for SUPER_ADMIN
    INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
    SELECT v_super_admin_id, sm.sub_menu_id, v_view_privilege_id
    FROM (VALUES
        (v_branch_sm_id),
        (v_dept_sm_id),
        (v_class_sm_id),
        (v_section_sm_id)
    ) AS sm(sub_menu_id)
    WHERE sm.sub_menu_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM role_submenu_privilege_mapping
        WHERE role_id = v_super_admin_id
          AND sub_menu_id = sm.sub_menu_id
          AND privilege_id = v_view_privilege_id
      );

    -- Insert mappings for ADMIN
    INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
    SELECT v_admin_id, sm.sub_menu_id, v_view_privilege_id
    FROM (VALUES
        (v_branch_sm_id),
        (v_dept_sm_id),
        (v_class_sm_id),
        (v_section_sm_id)
    ) AS sm(sub_menu_id)
    WHERE sm.sub_menu_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 1 FROM role_submenu_privilege_mapping
        WHERE role_id = v_admin_id
          AND sub_menu_id = sm.sub_menu_id
          AND privilege_id = v_view_privilege_id
      );

    RAISE NOTICE 'Master Setup menus mapped to SUPER_ADMIN and ADMIN roles.';
END $$;

-- ============================================================================
-- Migration complete
-- ============================================================================
