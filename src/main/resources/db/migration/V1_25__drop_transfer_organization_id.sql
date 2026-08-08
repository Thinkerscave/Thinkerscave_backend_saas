-- Align transfer_request with schema-per-tenant: drop leftover organization_id.
-- Apply in each tenant schema (and public if present).
ALTER TABLE IF EXISTS transfer_request DROP COLUMN IF EXISTS organization_id;
ALTER TABLE IF EXISTS student_document DROP COLUMN IF EXISTS organization_id;
