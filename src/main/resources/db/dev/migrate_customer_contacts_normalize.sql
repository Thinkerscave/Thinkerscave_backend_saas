-- Normalize Customer contacts (MySQL-safe, mostly idempotent).

-- Widen contact_type so PRIMARY/SECONDARY are allowed (legacy may be ENUM)
ALTER TABLE customer_contacts MODIFY COLUMN contact_type VARCHAR(30) NOT NULL;

-- Backfill PRIMARY contacts from legacy customer name columns when present
SET @has_pcn := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'primary_contact_name'
);
SET @has_primary_flag := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'primary_contact'
);

SET @sql := IF(@has_pcn > 0 AND @has_primary_flag > 0,
  'INSERT INTO customer_contacts (
      contact_code, customer_id, full_name, email, mobile_number, designation, contact_type,
      primary_contact, active, created_by, created_on, updated_by, updated_on, version
   )
   SELECT
      CONCAT(''CTC-MIG-'', c.id),
      c.id,
      COALESCE(NULLIF(c.primary_contact_name, ''''), c.customer_name, ''Owner''),
      c.business_email,
      c.mobile_number,
      NULL,
      ''PRIMARY'',
      TRUE,
      TRUE,
      ''system'',
      NOW(6),
      ''system'',
      NOW(6),
      0
   FROM customers c
   WHERE NOT EXISTS (
      SELECT 1 FROM customer_contacts cc
      WHERE cc.customer_id = c.id AND cc.active = TRUE
        AND (cc.contact_type IN (''PRIMARY'', ''OWNER'') OR cc.primary_contact = TRUE)
   )',
  IF(@has_pcn > 0,
    'INSERT INTO customer_contacts (
        contact_code, customer_id, full_name, email, mobile_number, designation, contact_type,
        active, created_by, created_on, updated_by, updated_on, version
     )
     SELECT
        CONCAT(''CTC-MIG-'', c.id),
        c.id,
        COALESCE(NULLIF(c.primary_contact_name, ''''), c.customer_name, ''Owner''),
        c.business_email,
        c.mobile_number,
        NULL,
        ''PRIMARY'',
        TRUE,
        ''system'',
        NOW(6),
        ''system'',
        NOW(6),
        0
     FROM customers c
     WHERE NOT EXISTS (
        SELECT 1 FROM customer_contacts cc
        WHERE cc.customer_id = c.id AND cc.active = TRUE AND cc.contact_type IN (''PRIMARY'', ''OWNER'')
     )',
    'SELECT 1'
  )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Normalize contact types
UPDATE customer_contacts SET contact_type = 'PRIMARY'
WHERE contact_type IN ('OWNER', 'DIRECTOR', 'PRINCIPAL', 'ADMINISTRATOR')
   OR contact_type NOT IN ('PRIMARY', 'SECONDARY');

UPDATE customer_contacts SET contact_type = 'SECONDARY'
WHERE contact_type IN ('BILLING', 'TECHNICAL', 'SALES', 'SUPPORT', 'OTHER');

UPDATE customer_contacts SET contact_type = 'PRIMARY'
WHERE contact_type NOT IN ('PRIMARY', 'SECONDARY');

-- Drop customer contact-name columns
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'primary_contact_name');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN primary_contact_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'secondary_contact_name');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN secondary_contact_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop obsolete contact columns
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'alternate_mobile_number');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN alternate_mobile_number', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'office_phone');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN office_phone', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'department');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN department', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'primary_contact');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN primary_contact', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'billing_contact');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN billing_contact', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'technical_contact');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN technical_contact', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'sales_contact');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN sales_contact', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'support_contact');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN support_contact', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_contacts' AND COLUMN_NAME = 'remarks');
SET @sql := IF(@col > 0, 'ALTER TABLE customer_contacts DROP COLUMN remarks', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE customers MODIFY COLUMN customer_name VARCHAR(150) NOT NULL;
ALTER TABLE customers MODIFY COLUMN notes VARCHAR(500) NULL;
ALTER TABLE customers MODIFY COLUMN mobile_number VARCHAR(30) NOT NULL;
ALTER TABLE customers MODIFY COLUMN alternate_mobile_number VARCHAR(30) NULL;
ALTER TABLE customer_contacts MODIFY COLUMN full_name VARCHAR(100) NOT NULL;
ALTER TABLE customer_contacts MODIFY COLUMN mobile_number VARCHAR(30) NULL;
ALTER TABLE customer_contacts MODIFY COLUMN contact_type VARCHAR(30) NOT NULL;
