-- Add customer owner linkage column for owner workspace switching support.
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS owner_user_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_customer_owner ON customers (owner_user_id);
