INSERT INTO role_permissions (
    organization_id,
    role_id,
    menu_id,
    can_view,
    can_manage,
    can_approve,
    created_by,
    created_on,
    updated_by,
    updated_on,
    version
)
SELECT
    organization.id,
    role.id,
    menu.id,
    TRUE,
    TRUE,
    TRUE,
    'system',
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    0
FROM organizations organization
JOIN tenant_registry tenant ON tenant.organization_id = organization.id
JOIN roles role
  ON role.role_type IN ('ORGANIZATION_OWNER', 'ORGANIZATION_ADMIN')
 AND role.active = TRUE
JOIN menus menu
  ON menu.active = TRUE
 AND COALESCE(menu.show_in_sidebar, TRUE) = TRUE
 AND COALESCE(menu.menu_scope, 'SUBSCRIPTION') <> 'PLATFORM'
WHERE organization.active = TRUE
  AND tenant.active = TRUE
ON CONFLICT (organization_id, role_id, menu_id) DO NOTHING;