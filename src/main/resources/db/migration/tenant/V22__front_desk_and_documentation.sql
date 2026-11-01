-- Front desk operations and class documentation campaigns.

CREATE TABLE IF NOT EXISTS front_desk_setting (
    setting_id      BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE,
    overstay_hours  INTEGER NOT NULL DEFAULT 8,
    created_by      VARCHAR(100),
    created_on      TIMESTAMP,
    updated_by      VARCHAR(100),
    updated_on      TIMESTAMP,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS front_desk_visitor (
    visitor_id        BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL,
    visitor_name      VARCHAR(150) NOT NULL,
    mobile_number     VARCHAR(20) NOT NULL,
    id_type           VARCHAR(40),
    id_number         VARCHAR(80),
    purpose           VARCHAR(30) NOT NULL,
    meet_person_type  VARCHAR(20),
    meet_person_id    BIGINT,
    meet_person_name  VARCHAR(200),
    vehicle_number    VARCHAR(40),
    badge_number      VARCHAR(40),
    in_time           TIMESTAMP NOT NULL,
    out_time          TIMESTAMP,
    photo_path        VARCHAR(1000),
    created_by        VARCHAR(100),
    created_on        TIMESTAMP,
    updated_by        VARCHAR(100),
    updated_on        TIMESTAMP,
    version           BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_visitor_org ON front_desk_visitor (organization_id, in_time);
CREATE INDEX IF NOT EXISTS idx_fd_visitor_open ON front_desk_visitor (organization_id) WHERE out_time IS NULL;

CREATE TABLE IF NOT EXISTS front_desk_appointment (
    appointment_id   BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    visitor_name     VARCHAR(150) NOT NULL,
    staff_id         BIGINT,
    staff_name       VARCHAR(200),
    requested_slot   TIMESTAMP NOT NULL,
    agenda           VARCHAR(500),
    status           VARCHAR(30) NOT NULL,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_appt_org ON front_desk_appointment (organization_id, requested_slot);

CREATE TABLE IF NOT EXISTS front_desk_postal_inward (
    inward_id        BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    sender_name      VARCHAR(150) NOT NULL,
    tracking_number  VARCHAR(80),
    courier_partner  VARCHAR(120),
    addressed_to     VARCHAR(200) NOT NULL,
    received_at      TIMESTAMP NOT NULL,
    status           VARCHAR(30) NOT NULL,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_inward_org ON front_desk_postal_inward (organization_id, received_at);

CREATE TABLE IF NOT EXISTS front_desk_postal_outward (
    outward_id         BIGSERIAL PRIMARY KEY,
    organization_id    BIGINT NOT NULL,
    recipient_name     VARCHAR(150) NOT NULL,
    destination_address VARCHAR(500) NOT NULL,
    document_type      VARCHAR(80),
    dispatch_mode      VARCHAR(40),
    dispatch_date      DATE NOT NULL,
    tracking_number    VARCHAR(80),
    sender_reference   VARCHAR(80),
    created_by         VARCHAR(100),
    created_on         TIMESTAMP,
    updated_by         VARCHAR(100),
    updated_on         TIMESTAMP,
    version            BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_outward_org ON front_desk_postal_outward (organization_id, dispatch_date);

CREATE TABLE IF NOT EXISTS front_desk_call_log (
    call_id          BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    caller_name      VARCHAR(150) NOT NULL,
    phone_number     VARCHAR(20) NOT NULL,
    call_type        VARCHAR(20) NOT NULL,
    duration_minutes INTEGER,
    purpose          VARCHAR(300),
    action_taken     VARCHAR(500),
    forwarded_to     VARCHAR(200),
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_call_org ON front_desk_call_log (organization_id, created_on);

CREATE TABLE IF NOT EXISTS front_desk_grievance (
    grievance_id     BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    complainant_type VARCHAR(20) NOT NULL,
    complainant_name VARCHAR(150),
    linked_id        BIGINT,
    category         VARCHAR(30) NOT NULL,
    details          VARCHAR(1000),
    assignee_name    VARCHAR(200),
    status           VARCHAR(30) NOT NULL,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_grievance_org ON front_desk_grievance (organization_id, status);

CREATE TABLE IF NOT EXISTS front_desk_certificate_request (
    request_id        BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL,
    student_id        BIGINT NOT NULL,
    student_name      VARCHAR(200) NOT NULL,
    class_name        VARCHAR(100),
    roll_number       VARCHAR(50),
    certificate_type  VARCHAR(40) NOT NULL,
    issuance_status   VARCHAR(30) NOT NULL,
    printed_on        TIMESTAMP,
    tc_number         VARCHAR(80),
    tc_issued_on      DATE,
    created_by        VARCHAR(100),
    created_on        TIMESTAMP,
    updated_by        VARCHAR(100),
    updated_on        TIMESTAMP,
    version           BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_cert_org ON front_desk_certificate_request (organization_id, created_on);

CREATE TABLE IF NOT EXISTS front_desk_gate_pass (
    gate_pass_id      BIGSERIAL PRIMARY KEY,
    organization_id   BIGINT NOT NULL,
    student_id        BIGINT NOT NULL,
    student_name      VARCHAR(200) NOT NULL,
    class_name        VARCHAR(100),
    section_name      VARCHAR(50),
    reason            VARCHAR(500) NOT NULL,
    pickup_person     VARCHAR(150) NOT NULL,
    verification_mode VARCHAR(20) NOT NULL,
    attendance_status VARCHAR(30) NOT NULL,
    issued_at         TIMESTAMP NOT NULL,
    created_by        VARCHAR(100),
    created_on        TIMESTAMP,
    updated_by        VARCHAR(100),
    updated_on        TIMESTAMP,
    version           BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_gate_org ON front_desk_gate_pass (organization_id, issued_at);

CREATE TABLE IF NOT EXISTS front_desk_gate_otp (
    otp_id           BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    student_id       BIGINT NOT NULL,
    code_hash        VARCHAR(128) NOT NULL,
    expires_at       TIMESTAMP NOT NULL,
    verified         BOOLEAN NOT NULL DEFAULT FALSE,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fd_otp_student ON front_desk_gate_otp (organization_id, student_id, expires_at);

CREATE TABLE IF NOT EXISTS doc_collection_master (
    master_id        BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    master_name      VARCHAR(150) NOT NULL,
    collection_type  VARCHAR(80) NOT NULL,
    description      VARCHAR(500),
    class_ids        VARCHAR(400) NOT NULL,
    status           VARCHAR(30) NOT NULL,
    rejection_reason VARCHAR(500),
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_doc_master_org ON doc_collection_master (organization_id, status);

CREATE TABLE IF NOT EXISTS study_material_master (
    master_id        BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    master_name      VARCHAR(150) NOT NULL,
    material_type    VARCHAR(80) NOT NULL,
    description      VARCHAR(500),
    class_ids        VARCHAR(400) NOT NULL,
    status           VARCHAR(30) NOT NULL,
    rejection_reason VARCHAR(500),
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_mat_master_org ON study_material_master (organization_id, status);

CREATE TABLE IF NOT EXISTS doc_collection_batch (
    batch_id         BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    class_id         BIGINT NOT NULL,
    class_name       VARCHAR(100),
    master_id        BIGINT NOT NULL,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS doc_collection_item (
    item_id          BIGSERIAL PRIMARY KEY,
    batch_id         BIGINT NOT NULL REFERENCES doc_collection_batch (batch_id),
    student_id       BIGINT NOT NULL,
    student_name     VARCHAR(200) NOT NULL,
    document_name    VARCHAR(200) NOT NULL,
    files_json       TEXT,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS study_material_handover (
    handover_id      BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT NOT NULL,
    class_id         BIGINT NOT NULL,
    class_name       VARCHAR(100),
    master_id        BIGINT NOT NULL,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS study_material_handover_item (
    item_id          BIGSERIAL PRIMARY KEY,
    handover_id      BIGINT NOT NULL REFERENCES study_material_handover (handover_id),
    student_id       BIGINT NOT NULL,
    student_name     VARCHAR(200) NOT NULL,
    description      VARCHAR(500),
    handed_over      BOOLEAN NOT NULL DEFAULT FALSE,
    status           VARCHAR(30) NOT NULL,
    created_by       VARCHAR(100),
    created_on       TIMESTAMP,
    updated_by       VARCHAR(100),
    updated_on       TIMESTAMP,
    version          BIGINT NOT NULL DEFAULT 0
);

-- Menus, when the tenant already has the menus table.
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

-- Group Front Desk pages and Documentation under the CORE Front Office menu.
DO $$
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

    UPDATE menus child
    SET parent_menu_id = front_office.id
    FROM menus front_office, menus desk
    WHERE front_office.menu_code = 'FRONT_OFFICE'
      AND desk.menu_code = 'FRONT_DESK'
      AND child.parent_menu_id = desk.id;

    UPDATE menus SET show_in_sidebar = FALSE WHERE menu_code = 'FRONT_DESK';

    UPDATE menus doc
    SET parent_menu_id = front_office.id
    FROM menus front_office
    WHERE front_office.menu_code = 'FRONT_OFFICE'
      AND doc.menu_code = 'DOCUMENTATION'
      AND doc.parent_menu_id IS DISTINCT FROM front_office.id;
END $$;

-- Enable the modules and give owner/admin roles full access to them.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'organizations'
    ) THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'organization_modules'
    ) THEN
        INSERT INTO organization_modules (organization_id, menu_id, enabled, created_by, created_on, updated_by, updated_on, version)
        SELECT o.id, m.id, TRUE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0
        FROM organizations o
        JOIN menus m ON m.menu_code IN ('FRONT_DESK', 'DOCUMENTATION')
        WHERE o.active = TRUE
          AND NOT EXISTS (
              SELECT 1 FROM organization_modules om
              WHERE om.organization_id = o.id AND om.menu_id = m.id
          );
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'role_permissions'
    ) THEN
        INSERT INTO role_permissions (organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_by, created_on, updated_by, updated_on, version)
        SELECT o.id, r.id, m.id, TRUE, TRUE, TRUE, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0
        FROM organizations o
        JOIN roles r ON r.role_type IN ('ORGANIZATION_OWNER', 'ORGANIZATION_ADMIN') AND r.active = TRUE
        JOIN menus m ON m.active = TRUE
            AND (m.menu_code IN ('FRONT_OFFICE', 'FRONT_DESK', 'DOCUMENTATION')
                 OR m.menu_code LIKE 'FRONT\_DESK\_%'
                 OR m.menu_code LIKE 'DOC\_%')
        WHERE o.active = TRUE
          AND NOT EXISTS (
              SELECT 1 FROM role_permissions rp
              WHERE rp.organization_id = o.id AND rp.role_id = r.id AND rp.menu_id = m.id
          );
    END IF;
END $$;
