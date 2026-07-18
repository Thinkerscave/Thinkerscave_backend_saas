-- Simplify customers table: lightweight account owner only.
-- MySQL-compatible. Safe to re-run where possible.

-- New columns
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'customer_name');
SET @sql := IF(@col = 0, 'ALTER TABLE customers ADD COLUMN customer_name VARCHAR(200) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'business_email');
SET @sql := IF(@col = 0, 'ALTER TABLE customers ADD COLUMN business_email VARCHAR(150) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'notes');
SET @sql := IF(@col = 0, 'ALTER TABLE customers ADD COLUMN notes VARCHAR(2000) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'owner_user_id');
SET @sql := IF(@col = 0, 'ALTER TABLE customers ADD COLUMN owner_user_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill from legacy columns when present
UPDATE customers
SET customer_name = COALESCE(NULLIF(customer_name, ''), NULLIF(display_name, ''), NULLIF(legal_name, ''), CONCAT('Customer-', id))
WHERE customer_name IS NULL OR customer_name = '';

UPDATE customers
SET business_email = COALESCE(NULLIF(business_email, ''), email)
WHERE business_email IS NULL OR business_email = '';

UPDATE customers
SET notes = COALESCE(notes, remarks)
WHERE notes IS NULL AND remarks IS NOT NULL;

-- Drop obsolete indexes (ignore errors via procedure pattern)
SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND INDEX_NAME = 'idx_customer_name');
SET @sql := IF(@idx > 0, 'ALTER TABLE customers DROP INDEX idx_customer_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND INDEX_NAME = 'idx_customer_email');
SET @sql := IF(@idx > 0, 'ALTER TABLE customers DROP INDEX idx_customer_email', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop obsolete columns
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'display_name');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN display_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'legal_name');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN legal_name', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'customer_type');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN customer_type', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'email');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN email', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'website');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN website', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'tax_number');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN tax_number', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'registration_number');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN registration_number', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'address_line_1');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN address_line_1', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'address_line_2');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN address_line_2', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'city');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN city', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'state');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN state', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'country');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN country', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'postal_code');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN postal_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'logo_url');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN logo_url', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'preferred_communication');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN preferred_communication', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'onboarding_completed');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN onboarding_completed', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'remarks');
SET @sql := IF(@col > 0, 'ALTER TABLE customers DROP COLUMN remarks', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Enforce NOT NULL where needed
ALTER TABLE customers
    MODIFY COLUMN customer_name VARCHAR(200) NOT NULL,
    MODIFY COLUMN business_email VARCHAR(150) NOT NULL;

-- Recreate indexes
CREATE INDEX idx_customer_name ON customers (customer_name);
CREATE INDEX idx_customer_email ON customers (business_email);
CREATE INDEX idx_customer_owner ON customers (owner_user_id);
