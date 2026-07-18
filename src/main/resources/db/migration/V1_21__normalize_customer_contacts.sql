-- Normalize Customer contacts for MySQL.
-- Prefer running db/dev/migrate_customer_contacts_normalize.sql in local/dev.

-- Backfill PRIMARY contact when missing
INSERT INTO customer_contacts (
    contact_code, customer_id, full_name, email, mobile_number, designation, contact_type, active,
    created_by, created_on, updated_by, updated_on, version
)
SELECT
    CONCAT('CTC-MIG-', c.id),
    c.id,
    COALESCE(c.customer_name, 'Owner'),
    c.business_email,
    c.mobile_number,
    NULL,
    'PRIMARY',
    TRUE,
    'system',
    NOW(6),
    'system',
    NOW(6),
    0
FROM customers c
WHERE NOT EXISTS (
    SELECT 1 FROM customer_contacts cc
    WHERE cc.customer_id = c.id AND cc.active = TRUE AND cc.contact_type = 'PRIMARY'
);

UPDATE customer_contacts SET contact_type = 'PRIMARY'
WHERE contact_type NOT IN ('PRIMARY', 'SECONDARY');
