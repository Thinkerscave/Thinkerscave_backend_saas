-- V23: Staff Management V2
--  * staff address links (present / permanent) and same-as-present flag
--  * staff_bank_details: AES-GCM encrypted account number, masked last4 for display
--  * staff_code_sequence: per-tenant STAFF code counter (single row, row-locked on increment)
--  * organization_id is not used: the schema itself is the tenant boundary.
-- Not applied to any tenant until explicitly released (see WORK_QUEUE.md).

-- Legacy column created outside migrations; removed (tenant test data is disposable).
ALTER TABLE staff DROP COLUMN IF EXISTS organization_id;

ALTER TABLE staff ADD COLUMN IF NOT EXISTS present_address_id BIGINT;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS permanent_address_id BIGINT;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS same_as_present_address BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_staff_present_address') THEN
        ALTER TABLE staff ADD CONSTRAINT fk_staff_present_address
            FOREIGN KEY (present_address_id) REFERENCES address (address_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_staff_permanent_address') THEN
        ALTER TABLE staff ADD CONSTRAINT fk_staff_permanent_address
            FOREIGN KEY (permanent_address_id) REFERENCES address (address_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS staff_bank_details (
    bank_detail_id            BIGSERIAL PRIMARY KEY,
    staff_id                  BIGINT       NOT NULL UNIQUE REFERENCES staff (staff_id),
    bank_name                 VARCHAR(150) NOT NULL,
    account_holder_name       VARCHAR(150) NOT NULL,
    account_number_encrypted  TEXT         NOT NULL,
    account_number_last4      VARCHAR(4)   NOT NULL,
    ifsc_code                 VARCHAR(11)  NOT NULL,
    encryption_key_version    INTEGER      NOT NULL DEFAULT 1,
    created_by                VARCHAR(255),
    created_on                TIMESTAMP,
    updated_by                VARCHAR(255),
    updated_on                TIMESTAMP,
    version                   BIGINT       NOT NULL DEFAULT 0,
    active                    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS staff_code_sequence (
    id          SMALLINT  PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    last_value  BIGINT    NOT NULL DEFAULT 0,
    updated_on  TIMESTAMP NOT NULL DEFAULT NOW()
);
