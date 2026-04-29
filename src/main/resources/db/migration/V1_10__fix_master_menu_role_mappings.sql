-- ============================================================================
-- Migration: V1_10__fix_master_menu_role_mappings.sql
--
-- Purpose: V1_9 ran but its role-mapping step may have failed silently
--          (wrong table name 'roles' vs correct 'role_master').
--          This migration ensures the 4 new sub-menus are properly mapped
--          to SUPER_ADMIN and ADMIN roles using the correct table names.
-- ============================================================================

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
    -- ── 1. Get VIEW privilege ──────────────────────────────────────────────
    SELECT privilege_id INTO v_view_privilege_id
    FROM privilege_master
    WHERE UPPER(privilege_name) IN ('VIEW','READ','FULL_ACCESS','ALL')
    ORDER BY privilege_id ASC
    LIMIT 1;

    IF v_view_privilege_id IS NULL THEN
        RAISE NOTICE '[V1_10] No VIEW/READ privilege found in privilege_master. Skipping.';
        RETURN;
    END IF;

    -- ── 2. Get role IDs from role_master ───────────────────────────────────
    SELECT role_id INTO v_super_admin_id
    FROM role_master WHERE UPPER(role_name) IN ('SUPER_ADMIN','SUPERADMIN') LIMIT 1;

    SELECT role_id INTO v_admin_id
    FROM role_master WHERE UPPER(role_name) = 'ADMIN' LIMIT 1;

    -- ── 3. Get sub-menu IDs ────────────────────────────────────────────────
    SELECT sub_menu_id INTO v_branch_sm_id  FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_BRANCH';
    SELECT sub_menu_id INTO v_dept_sm_id    FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_DEPARTMENT';
    SELECT sub_menu_id INTO v_class_sm_id   FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_CLASS';
    SELECT sub_menu_id INTO v_section_sm_id FROM sub_menu_master WHERE sub_menu_code = 'SUBMENU_MANAGE_SECTION';

    -- ── 4. Upsert mappings for SUPER_ADMIN ─────────────────────────────────
    IF v_super_admin_id IS NOT NULL THEN
        INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
        SELECT v_super_admin_id, sm_id, v_view_privilege_id
        FROM (VALUES (v_branch_sm_id),(v_dept_sm_id),(v_class_sm_id),(v_section_sm_id)) t(sm_id)
        WHERE sm_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM role_submenu_privilege_mapping
              WHERE role_id = v_super_admin_id
                AND sub_menu_id = t.sm_id
                AND privilege_id = v_view_privilege_id
          );
        RAISE NOTICE '[V1_10] Mapped Master Setup sub-menus to SUPER_ADMIN (role_id=%).',  v_super_admin_id;
    ELSE
        RAISE NOTICE '[V1_10] SUPER_ADMIN role not found. Map Master Setup menus manually via the Role Menu Mapping screen.';
    END IF;

    -- ── 5. Upsert mappings for ADMIN ──────────────────────────────────────
    IF v_admin_id IS NOT NULL THEN
        INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
        SELECT v_admin_id, sm_id, v_view_privilege_id
        FROM (VALUES (v_branch_sm_id),(v_dept_sm_id),(v_class_sm_id),(v_section_sm_id)) t(sm_id)
        WHERE sm_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM role_submenu_privilege_mapping
              WHERE role_id = v_admin_id
                AND sub_menu_id = t.sm_id
                AND privilege_id = v_view_privilege_id
          );
        RAISE NOTICE '[V1_10] Mapped Master Setup sub-menus to ADMIN (role_id=%).', v_admin_id;
    ELSE
        RAISE NOTICE '[V1_10] ADMIN role not found. Map Master Setup menus manually via the Role Menu Mapping screen.';
    END IF;
END $$;

-- ============================================================================
-- Migration complete
-- ============================================================================
