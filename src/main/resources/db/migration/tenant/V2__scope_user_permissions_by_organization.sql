ALTER TABLE user_permissions
    ADD COLUMN IF NOT EXISTS organization_id BIGINT;

ALTER TABLE user_permissions DROP CONSTRAINT IF EXISTS uk_user_permission;
ALTER TABLE user_permissions ADD CONSTRAINT uk_user_permission
    UNIQUE (user_id, menu_id, organization_id);

CREATE INDEX IF NOT EXISTS idx_user_permission_organization
    ON user_permissions(organization_id);
