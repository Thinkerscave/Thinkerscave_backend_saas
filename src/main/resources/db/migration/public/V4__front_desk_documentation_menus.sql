-- Front Desk and Documentation live in the platform schema with the other menus.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'menus'
    ) THEN
        INSERT INTO menus (menu_code, menu_name, description, route, icon, menu_type, display_order, show_in_sidebar, active, default_page, menu_scope, version)
        SELECT 'FRONT_DESK', 'Front Desk', 'Reception desk', '/app/front-desk', 'pi pi-building', 'MODULE', 45, TRUE, TRUE, FALSE, 'SUBSCRIPTION', 0
        WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'FRONT_DESK');

        INSERT INTO menus (menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, menu_scope, version)
        SELECT v.code, v.name, v.descr, v.route, 'pi pi-circle', 'PAGE', p.id, v.ord, TRUE, TRUE, FALSE, 'SUBSCRIPTION', 0
        FROM (VALUES
            ('FRONT_DESK_VISITORS', 'Visitor Pass', 'Visitor log', '/app/front-desk/visitors', 1),
            ('FRONT_DESK_APPOINTMENTS', 'Appointments', 'Appointments', '/app/front-desk/appointments', 2),
            ('FRONT_DESK_POSTAL', 'Postal', 'Inward and outward mail', '/app/front-desk/postal/inward', 3),
            ('FRONT_DESK_CALLS', 'Call Logs', 'Phone call logs', '/app/front-desk/calls', 4),
            ('FRONT_DESK_GRIEVANCES', 'Help Desk', 'Grievances', '/app/front-desk/grievances', 5),
            ('FRONT_DESK_CERTIFICATES', 'Certificate Requests', 'Certificate slips', '/app/front-desk/certificates', 6),
            ('FRONT_DESK_GATE_PASS', 'Early Exit', 'Gate pass', '/app/front-desk/gate-pass', 7),
            ('FRONT_DESK_ACTIVE_VISITORS', 'Active Visitors', 'Visitors still on campus', '/app/front-desk/visitors/active', 8),
            ('FRONT_DESK_INQUIRIES', 'Admission Inquiries', 'Walk-in inquiries', '/app/front-desk/inquiries', 9),
            ('FRONT_DESK_ONLINE_INQUIRIES', 'Online Inquiries', 'Website and ad inquiries', '/app/front-desk/inquiries/online', 10)
        ) AS v(code, name, descr, route, ord)
        JOIN menus p ON p.menu_code = 'FRONT_DESK'
        WHERE NOT EXISTS (SELECT 1 FROM menus m WHERE m.menu_code = v.code);

        INSERT INTO menus (menu_code, menu_name, description, route, icon, menu_type, display_order, show_in_sidebar, active, default_page, menu_scope, version)
        SELECT 'DOCUMENTATION', 'Documentation', 'Class document collection and study material', '/app/documentation/collections', 'pi pi-folder', 'MODULE', 46, TRUE, TRUE, FALSE, 'SUBSCRIPTION', 0
        WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'DOCUMENTATION');

        INSERT INTO menus (menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, display_order, show_in_sidebar, active, default_page, menu_scope, version)
        SELECT v.code, v.name, v.descr, v.route, 'pi pi-circle', 'PAGE', p.id, v.ord, TRUE, TRUE, FALSE, 'SUBSCRIPTION', 0
        FROM (VALUES
            ('DOC_COLLECTION_MASTER', 'Collection Master', 'Document collection types', '/app/documentation/collection-masters', 1),
            ('DOC_MATERIAL_MASTER', 'Material Master', 'Study material types', '/app/documentation/material-masters', 2),
            ('DOC_COLLECTIONS', 'Document Collection', 'Collect documents by class', '/app/documentation/collections', 3),
            ('DOC_HANDOVERS', 'Study Material Handover', 'Hand over study material', '/app/documentation/handovers', 4)
        ) AS v(code, name, descr, route, ord)
        JOIN menus p ON p.menu_code = 'DOCUMENTATION'
        WHERE NOT EXISTS (SELECT 1 FROM menus m WHERE m.menu_code = v.code);
    END IF;
END $$;
