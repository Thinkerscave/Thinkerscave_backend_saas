-- Flyway: simplify customers to lightweight account owner fields.
-- Note: production may need a Postgres variant; this script targets MySQL-compatible DDL.

ALTER TABLE customers ADD COLUMN IF NOT EXISTS customer_name VARCHAR(200) NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS business_email VARCHAR(150) NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS primary_contact_name VARCHAR(150) NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS secondary_contact_name VARCHAR(150) NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS notes VARCHAR(2000) NULL;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS owner_user_id BIGINT NULL;

UPDATE customers
SET customer_name = COALESCE(NULLIF(customer_name, ''), NULLIF(display_name, ''), NULLIF(legal_name, ''), CONCAT('Customer-', id))
WHERE customer_name IS NULL OR customer_name = '';

UPDATE customers
SET business_email = COALESCE(NULLIF(business_email, ''), email)
WHERE business_email IS NULL OR business_email = '';

UPDATE customers
SET primary_contact_name = COALESCE(NULLIF(primary_contact_name, ''), NULLIF(display_name, ''), NULLIF(legal_name, ''), 'Owner')
WHERE primary_contact_name IS NULL OR primary_contact_name = '';

UPDATE customers
SET notes = COALESCE(notes, remarks)
WHERE notes IS NULL;

ALTER TABLE customers DROP COLUMN IF EXISTS display_name;
ALTER TABLE customers DROP COLUMN IF EXISTS legal_name;
ALTER TABLE customers DROP COLUMN IF EXISTS customer_type;
ALTER TABLE customers DROP COLUMN IF EXISTS email;
ALTER TABLE customers DROP COLUMN IF EXISTS website;
ALTER TABLE customers DROP COLUMN IF EXISTS tax_number;
ALTER TABLE customers DROP COLUMN IF EXISTS registration_number;
ALTER TABLE customers DROP COLUMN IF EXISTS address_line_1;
ALTER TABLE customers DROP COLUMN IF EXISTS address_line_2;
ALTER TABLE customers DROP COLUMN IF EXISTS city;
ALTER TABLE customers DROP COLUMN IF EXISTS state;
ALTER TABLE customers DROP COLUMN IF EXISTS country;
ALTER TABLE customers DROP COLUMN IF EXISTS postal_code;
ALTER TABLE customers DROP COLUMN IF EXISTS logo_url;
ALTER TABLE customers DROP COLUMN IF EXISTS preferred_communication;
ALTER TABLE customers DROP COLUMN IF EXISTS onboarding_completed;
ALTER TABLE customers DROP COLUMN IF EXISTS remarks;
