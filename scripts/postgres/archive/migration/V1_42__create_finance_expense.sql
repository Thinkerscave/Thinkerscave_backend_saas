-- Finance Expenses V1 schema (public + tenant_*).
DO $$
DECLARE
    s text;
BEGIN
    FOR s IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname = 'public'
           OR nspname LIKE 'tenant_%'
    LOOP
        -- Categories (system-seeded)
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.expense_category (
                expense_category_id bigserial PRIMARY KEY,
                code varchar(40) NOT NULL,
                name varchar(120) NOT NULL,
                description varchar(500),
                sort_order int NOT NULL DEFAULT 0,
                active boolean NOT NULL DEFAULT true,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_expense_category_code UNIQUE (code)
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_category_active_sort ON %I.expense_category (active, sort_order)', s);

        EXECUTE format('
            INSERT INTO %I.expense_category (code, name, description, sort_order, active, created_on, version)
            VALUES
                (''UTILITIES'', ''Utilities'', ''Electricity, water, internet'', 10, true, now(), 0),
                (''TRANSPORT'', ''Transport'', ''Fuel and transport'', 20, true, now(), 0),
                (''ACADEMIC'', ''Academic'', ''Academic materials'', 30, true, now(), 0),
                (''ADMINISTRATIVE'', ''Administrative'', ''Admin operations'', 40, true, now(), 0),
                (''MAINTENANCE'', ''Maintenance'', ''Facilities maintenance'', 50, true, now(), 0),
                (''MARKETING'', ''Marketing'', ''Marketing and outreach'', 60, true, now(), 0),
                (''STAFF_WELFARE'', ''Staff Welfare'', ''Staff welfare'', 70, true, now(), 0),
                (''OTHER'', ''Other'', ''Other expenses'', 100, true, now(), 0)
            ON CONFLICT (code) DO NOTHING
        ', s);

        -- Expense heads
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.expense_head (
                expense_head_id bigserial PRIMARY KEY,
                name varchar(150) NOT NULL,
                expense_category_id bigint NOT NULL REFERENCES %I.expense_category (expense_category_id),
                default_requester_staff_id bigint,
                description varchar(500),
                status varchar(20) NOT NULL DEFAULT ''ACTIVE'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_expense_head_status CHECK (status IN (''ACTIVE'',''INACTIVE''))
            )', s, s);
        EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS uk_expense_head_name_ci ON %I.expense_head (lower(name))', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_head_category ON %I.expense_head (expense_category_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_head_status ON %I.expense_head (status)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_head_default_requester ON %I.expense_head (default_requester_staff_id)', s);

        -- Configuration singleton
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.expense_configuration (
                expense_configuration_id bigserial PRIMARY KEY,
                approval_required boolean NOT NULL DEFAULT true,
                number_prefix varchar(20) NOT NULL DEFAULT ''EXP'',
                number_year_format varchar(10) NOT NULL DEFAULT ''YYYY'',
                number_pad_width smallint NOT NULL DEFAULT 6,
                number_start bigint NOT NULL DEFAULT 1,
                number_sequence_per_year boolean NOT NULL DEFAULT true,
                default_payment_method_id bigint,
                max_attachment_bytes bigint NOT NULL DEFAULT 10485760,
                allowed_attachment_content_types varchar(500) NOT NULL DEFAULT ''application/pdf,image/jpeg,image/png'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_expense_cfg_pad CHECK (number_pad_width BETWEEN 1 AND 10),
                CONSTRAINT ck_expense_cfg_start CHECK (number_start >= 1)
            )', s);

        EXECUTE format('
            INSERT INTO %I.expense_configuration (
                approval_required, number_prefix, number_year_format, number_pad_width, number_start,
                number_sequence_per_year, max_attachment_bytes, allowed_attachment_content_types, created_on, version
            )
            SELECT true, ''EXP'', ''YYYY'', 6, 1, true, 10485760, ''application/pdf,image/jpeg,image/png'', now(), 0
            WHERE NOT EXISTS (SELECT 1 FROM %I.expense_configuration)
        ', s, s);

        -- Number sequence
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.expense_number_sequence (
                expense_number_sequence_id bigserial PRIMARY KEY,
                sequence_key varchar(40) NOT NULL,
                last_value bigint NOT NULL DEFAULT 0,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_expense_number_sequence_key UNIQUE (sequence_key)
            )', s);

        -- Expense transaction
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.expense (
                expense_id bigserial PRIMARY KEY,
                expense_number varchar(40) NOT NULL,
                expense_date date NOT NULL,
                expense_head_id bigint NOT NULL REFERENCES %I.expense_head (expense_head_id),
                expense_category_id bigint NOT NULL REFERENCES %I.expense_category (expense_category_id),
                head_name_snapshot varchar(150) NOT NULL,
                category_code_snapshot varchar(40) NOT NULL,
                category_name_snapshot varchar(120) NOT NULL,
                amount numeric(14,2) NOT NULL,
                vendor_name varchar(200),
                vendor_invoice_number varchar(100),
                requester_staff_id bigint NOT NULL,
                remarks varchar(2000),
                approval_status varchar(30) NOT NULL,
                payment_status varchar(30) NOT NULL DEFAULT ''UNPAID'',
                paid_amount numeric(14,2) NOT NULL DEFAULT 0,
                remaining_amount numeric(14,2) NOT NULL,
                submitted_on timestamptz,
                approved_on timestamptz,
                approved_by varchar(100),
                rejected_on timestamptz,
                rejected_by varchar(100),
                rejection_reason varchar(1000),
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_expense_number UNIQUE (expense_number),
                CONSTRAINT ck_expense_amount CHECK (amount > 0),
                CONSTRAINT ck_expense_paid CHECK (paid_amount >= 0 AND paid_amount <= amount),
                CONSTRAINT ck_expense_approval CHECK (approval_status IN (
                    ''DRAFT'',''PENDING_APPROVAL'',''APPROVED'',''REJECTED'')),
                CONSTRAINT ck_expense_payment CHECK (payment_status IN (
                    ''UNPAID'',''PARTIALLY_PAID'',''PAID''))
            )', s, s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_date ON %I.expense (expense_date DESC)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_approval ON %I.expense (approval_status)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_payment ON %I.expense (payment_status)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_head ON %I.expense (expense_head_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_category ON %I.expense (expense_category_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_requester ON %I.expense (requester_staff_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_vendor_invoice ON %I.expense (vendor_invoice_number)', s);

        -- Payments
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.expense_payment (
                expense_payment_id bigserial PRIMARY KEY,
                expense_id bigint NOT NULL REFERENCES %I.expense (expense_id),
                amount numeric(14,2) NOT NULL,
                paid_on date NOT NULL,
                payment_method_id bigint,
                payment_method_name varchar(100),
                reference_number varchar(100),
                remarks varchar(1000),
                idempotency_key varchar(120) NOT NULL,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_expense_payment_idem UNIQUE (idempotency_key),
                CONSTRAINT ck_expense_payment_amount CHECK (amount > 0)
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_payment_expense ON %I.expense_payment (expense_id, paid_on)', s);

        -- Approval events
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.expense_approval_event (
                expense_approval_event_id bigserial PRIMARY KEY,
                expense_id bigint NOT NULL REFERENCES %I.expense (expense_id) ON DELETE CASCADE,
                event_type varchar(40) NOT NULL,
                from_status varchar(30),
                to_status varchar(30) NOT NULL,
                actor varchar(100) NOT NULL,
                remarks varchar(1000),
                occurred_on timestamptz NOT NULL DEFAULT now(),
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_expense_approval_event_type CHECK (event_type IN (
                    ''SUBMITTED'',''APPROVED'',''REJECTED'',''RETURNED_TO_DRAFT''))
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_expense_approval_event_expense ON %I.expense_approval_event (expense_id, occurred_on DESC)', s);
    END LOOP;
END $$;
