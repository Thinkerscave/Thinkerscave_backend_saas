-- Attach Front Desk / Documentation to the CORE Front Office group so tenant admins
-- see entitled children under Front Office (subscription pages inherit from CORE parent).
DO $$
DECLARE
    front_office_id BIGINT;
    front_desk_id BIGINT;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'menus'
    ) THEN
        RETURN;
    END IF;

    INSERT INTO menus (menu_code, menu_name, description, route, icon, menu_type, display_order, show_in_sidebar, active, default_page, menu_scope, version)
    SELECT 'FRONT_OFFICE', 'Front Office', 'Reception and front-desk operations', NULL, 'pi pi-building', 'MODULE', 44, TRUE, TRUE, FALSE, 'CORE', 0
    WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'FRONT_OFFICE');

    SELECT id INTO front_office_id FROM menus WHERE menu_code = 'FRONT_OFFICE';
    SELECT id INTO front_desk_id FROM menus WHERE menu_code = 'FRONT_DESK';

    IF front_office_id IS NULL OR front_desk_id IS NULL THEN
        RETURN;
    END IF;

    UPDATE menus child
    SET parent_menu_id = front_office_id
    WHERE child.parent_menu_id = front_desk_id;

    UPDATE menus
    SET show_in_sidebar = FALSE
    WHERE menu_code = 'FRONT_DESK';

    UPDATE menus
    SET parent_menu_id = front_office_id
    WHERE menu_code = 'DOCUMENTATION'
      AND (parent_menu_id IS NULL OR parent_menu_id <> front_office_id);

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'organizations'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'organization_modules'
    ) THEN
        INSERT INTO organization_modules (organization_id, menu_id, enabled, created_by, created_on, updated_by, updated_on, version)
        SELECT o.id, m.id, TRUE, 'system', NOW(), 'system', NOW(), 0
        FROM organizations o
        CROSS JOIN menus m
        WHERE m.menu_code IN ('FRONT_DESK', 'DOCUMENTATION')
          AND NOT EXISTS (
              SELECT 1 FROM organization_modules om
              WHERE om.organization_id = o.id AND om.menu_id = m.id
          );
    END IF;
END $$;
