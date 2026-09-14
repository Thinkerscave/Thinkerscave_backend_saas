-- Finance → Fees V1 schema (public + tenant_*).
-- Aligns with docs/Module/Finance/Finance_Fees_DB_Implementation.md
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
        -- Platform structured responsibility scope (Access/Staff owned; Finance reads)
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.responsibility_assignment_scope (
                scope_id bigserial PRIMARY KEY,
                assignment_id bigint NOT NULL,
                scope_type varchar(20) NOT NULL,
                class_id bigint,
                section_id bigint,
                student_id bigint,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_ras_scope_type CHECK (scope_type IN (''ORGANIZATION'',''CLASS'',''SECTION'',''STUDENT''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_ras_assignment ON %I.responsibility_assignment_scope (assignment_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_head (
                fee_head_id bigserial PRIMARY KEY,
                name varchar(100) NOT NULL,
                name_normalized varchar(100) NOT NULL,
                category varchar(40) NOT NULL,
                description varchar(500),
                status varchar(20) NOT NULL DEFAULT ''ACTIVE'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fee_head_name_normalized UNIQUE (name_normalized),
                CONSTRAINT ck_fee_head_category CHECK (category IN (
                    ''ACADEMIC'',''ADMISSION_AND_REGISTRATION'',''FACILITIES_AND_SERVICES'',''MISCELLANEOUS'')),
                CONSTRAINT ck_fee_head_status CHECK (status IN (''ACTIVE'',''INACTIVE''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_head_status ON %I.fee_head (status)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_head_category ON %I.fee_head (category)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_structure (
                fee_structure_id bigserial PRIMARY KEY,
                name varchar(150) NOT NULL,
                academic_year_id bigint NOT NULL,
                class_id bigint NOT NULL,
                due_day smallint NOT NULL,
                status varchar(20) NOT NULL DEFAULT ''ACTIVE'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_fee_structure_due_day CHECK (due_day BETWEEN 1 AND 28),
                CONSTRAINT ck_fee_structure_status CHECK (status IN (''ACTIVE'',''INACTIVE''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_structure_year_class ON %I.fee_structure (academic_year_id, class_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_structure_status ON %I.fee_structure (status)', s);
        EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS uk_fee_structure_active_year_class
            ON %I.fee_structure (academic_year_id, class_id) WHERE status = ''ACTIVE''', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_structure_item (
                fee_structure_item_id bigserial PRIMARY KEY,
                fee_structure_id bigint NOT NULL REFERENCES %I.fee_structure (fee_structure_id),
                fee_head_id bigint NOT NULL REFERENCES %I.fee_head (fee_head_id),
                amount numeric(12,2) NOT NULL,
                frequency varchar(20) NOT NULL,
                type varchar(20) NOT NULL,
                service_key varchar(20) NOT NULL DEFAULT ''NONE'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fee_structure_item_head UNIQUE (fee_structure_id, fee_head_id),
                CONSTRAINT ck_fee_structure_item_amount CHECK (amount > 0),
                CONSTRAINT ck_fee_structure_item_frequency CHECK (frequency IN (
                    ''ONE_TIME'',''MONTHLY'',''QUARTERLY'',''HALF_YEARLY'',''YEARLY'')),
                CONSTRAINT ck_fee_structure_item_type CHECK (type IN (''MANDATORY'',''OPTIONAL'')),
                CONSTRAINT ck_fee_structure_item_service CHECK (service_key IN (''NONE'',''TRANSPORT'',''HOSTEL'')),
                CONSTRAINT ck_fee_structure_item_service_rule CHECK (
                    (type = ''MANDATORY'' AND service_key = ''NONE'')
                    OR (type = ''OPTIONAL'' AND service_key IN (''TRANSPORT'',''HOSTEL''))
                )
            )', s, s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_structure_item_structure ON %I.fee_structure_item (fee_structure_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_structure_item_head ON %I.fee_structure_item (fee_head_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.student_fee_account (
                student_fee_account_id bigserial PRIMARY KEY,
                student_id bigint NOT NULL,
                academic_year_id bigint NOT NULL,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_student_fee_account UNIQUE (student_id, academic_year_id)
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sfa_year ON %I.student_fee_account (academic_year_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.student_billing_period (
                student_billing_period_id bigserial PRIMARY KEY,
                student_id bigint NOT NULL,
                academic_year_id bigint NOT NULL,
                fee_structure_id bigint NOT NULL,
                period_key varchar(32) NOT NULL,
                period_label varchar(80) NOT NULL,
                period_start date NOT NULL,
                period_end date NOT NULL,
                due_date date NOT NULL,
                total_amount numeric(12,2) NOT NULL,
                paid_amount numeric(12,2) NOT NULL DEFAULT 0,
                balance_amount numeric(12,2) NOT NULL,
                status varchar(20) NOT NULL DEFAULT ''DUE'',
                class_id bigint,
                class_name varchar(100),
                section_id bigint,
                section_name varchar(100),
                structure_name varchar(150),
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_student_billing_period UNIQUE (student_id, academic_year_id, period_key),
                CONSTRAINT ck_sbp_amounts CHECK (
                    total_amount >= 0 AND paid_amount >= 0 AND balance_amount >= 0
                    AND paid_amount + balance_amount = total_amount),
                CONSTRAINT ck_sbp_status CHECK (status IN (''DUE'',''PARTIALLY_PAID'',''PAID'',''OVERDUE''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbp_student_year ON %I.student_billing_period (student_id, academic_year_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbp_year_status ON %I.student_billing_period (academic_year_id, status)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbp_due_date ON %I.student_billing_period (due_date)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbp_class ON %I.student_billing_period (class_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbp_section ON %I.student_billing_period (section_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbp_structure ON %I.student_billing_period (fee_structure_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbp_year_due_status ON %I.student_billing_period (academic_year_id, due_date, status)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.student_billing_period_line (
                student_billing_period_line_id bigserial PRIMARY KEY,
                student_billing_period_id bigint NOT NULL REFERENCES %I.student_billing_period (student_billing_period_id),
                fee_head_id bigint NOT NULL,
                fee_head_name varchar(100) NOT NULL,
                fee_head_category varchar(40),
                amount numeric(12,2) NOT NULL,
                frequency varchar(20) NOT NULL,
                type varchar(20) NOT NULL,
                service_key varchar(20) NOT NULL DEFAULT ''NONE'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_sbpl_amount CHECK (amount > 0)
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbpl_period ON %I.student_billing_period_line (student_billing_period_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_sbpl_head ON %I.student_billing_period_line (fee_head_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_payment_method (
                fee_payment_method_id bigserial PRIMARY KEY,
                name varchar(80) NOT NULL,
                name_normalized varchar(80) NOT NULL,
                description varchar(255),
                status varchar(20) NOT NULL DEFAULT ''ACTIVE'',
                requires_reference boolean NOT NULL DEFAULT false,
                sort_order int NOT NULL DEFAULT 0,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fee_payment_method_name UNIQUE (name_normalized),
                CONSTRAINT ck_fee_payment_method_status CHECK (status IN (''ACTIVE'',''INACTIVE''))
            )', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_payment (
                fee_payment_id bigserial PRIMARY KEY,
                student_id bigint NOT NULL,
                academic_year_id bigint NOT NULL,
                payment_method_id bigint NOT NULL,
                payment_method_name varchar(80) NOT NULL,
                amount numeric(12,2) NOT NULL,
                paid_on timestamp NOT NULL,
                reference_number varchar(80),
                remarks varchar(500),
                status varchar(20) NOT NULL DEFAULT ''SUCCESS'',
                idempotency_key varchar(80),
                request_hash varchar(64),
                collected_by_user_id bigint,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_fee_payment_amount CHECK (amount > 0),
                CONSTRAINT ck_fee_payment_status CHECK (status IN (''SUCCESS'')),
                CONSTRAINT uk_fee_payment_idempotency UNIQUE (idempotency_key)
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_payment_student_year ON %I.fee_payment (student_id, academic_year_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_payment_date ON %I.fee_payment (paid_on)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_payment_method ON %I.fee_payment (payment_method_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_payment_year_date ON %I.fee_payment (academic_year_id, paid_on)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_payment_allocation (
                fee_payment_allocation_id bigserial PRIMARY KEY,
                fee_payment_id bigint NOT NULL REFERENCES %I.fee_payment (fee_payment_id),
                student_billing_period_id bigint NOT NULL,
                allocated_amount numeric(12,2) NOT NULL,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fpa_payment_period UNIQUE (fee_payment_id, student_billing_period_id),
                CONSTRAINT ck_fpa_amount CHECK (allocated_amount > 0)
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fpa_payment ON %I.fee_payment_allocation (fee_payment_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fpa_period ON %I.fee_payment_allocation (student_billing_period_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_receipt (
                fee_receipt_id bigserial PRIMARY KEY,
                fee_payment_id bigint NOT NULL,
                receipt_number varchar(60) NOT NULL,
                issued_on timestamp NOT NULL,
                status varchar(20) NOT NULL DEFAULT ''ISSUED'',
                student_id bigint NOT NULL,
                student_name varchar(200) NOT NULL,
                admission_number varchar(50) NOT NULL,
                class_name varchar(100),
                section_name varchar(100),
                academic_year_id bigint NOT NULL,
                academic_year_name varchar(50) NOT NULL,
                amount numeric(12,2) NOT NULL,
                payment_method_name varchar(80) NOT NULL,
                reference_number varchar(80),
                remarks varchar(500),
                school_name varchar(200) NOT NULL,
                school_logo_url varchar(500),
                school_address varchar(500),
                school_contact varchar(200),
                currency_code varchar(10),
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fee_receipt_number UNIQUE (receipt_number),
                CONSTRAINT uk_fee_receipt_payment UNIQUE (fee_payment_id),
                CONSTRAINT ck_fee_receipt_status CHECK (status IN (''ISSUED''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_receipt_student_year ON %I.fee_receipt (student_id, academic_year_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_receipt_date ON %I.fee_receipt (issued_on)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_receipt_year ON %I.fee_receipt (academic_year_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_receipt_line (
                fee_receipt_line_id bigserial PRIMARY KEY,
                fee_receipt_id bigint NOT NULL REFERENCES %I.fee_receipt (fee_receipt_id),
                student_billing_period_id bigint,
                period_key varchar(32) NOT NULL,
                period_label varchar(80) NOT NULL,
                amount numeric(12,2) NOT NULL,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_frl_amount CHECK (amount > 0)
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_receipt_line_receipt ON %I.fee_receipt_line (fee_receipt_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_receipt_sequence (
                fee_receipt_sequence_id bigserial PRIMARY KEY,
                sequence_key varchar(40) NOT NULL,
                last_value bigint NOT NULL DEFAULT 0,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fee_receipt_sequence_key UNIQUE (sequence_key)
            )', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.finance_configuration (
                finance_configuration_id bigserial PRIMARY KEY,
                auto_generate_dues boolean NOT NULL DEFAULT true,
                generation_lead_days int NOT NULL DEFAULT 5,
                default_due_day_for_new_structures smallint NOT NULL DEFAULT 10,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_fc_lead_days CHECK (generation_lead_days >= 0),
                CONSTRAINT ck_fc_due_day CHECK (default_due_day_for_new_structures BETWEEN 1 AND 28)
            )', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_reminder_rule (
                fee_reminder_rule_id bigserial PRIMARY KEY,
                rule_key varchar(40) NOT NULL,
                enabled boolean NOT NULL DEFAULT false,
                offset_days int NOT NULL DEFAULT 0,
                channels_csv varchar(100) NOT NULL DEFAULT ''IN_APP'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fee_reminder_rule_key UNIQUE (rule_key),
                CONSTRAINT ck_frr_offset CHECK (offset_days >= 0)
            )', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.fee_reminder_log (
                fee_reminder_log_id bigserial PRIMARY KEY,
                student_billing_period_id bigint NOT NULL,
                rule_key varchar(40) NOT NULL,
                sent_on date NOT NULL,
                notification_id bigint,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_fee_reminder_log UNIQUE (student_billing_period_id, rule_key, sent_on)
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_fee_reminder_log_period ON %I.fee_reminder_log (student_billing_period_id)', s);

        -- Seed config + reminder rules + payment methods (idempotent)
        EXECUTE format('
            INSERT INTO %I.finance_configuration (
                auto_generate_dues, generation_lead_days, default_due_day_for_new_structures,
                created_by, created_on, updated_by, updated_on, version)
            SELECT true, 5, 10, ''system'', NOW(), ''system'', NOW(), 0
            WHERE NOT EXISTS (SELECT 1 FROM %I.finance_configuration)
        ', s, s);

        EXECUTE format('
            INSERT INTO %I.fee_reminder_rule (rule_key, enabled, offset_days, channels_csv, created_by, created_on, updated_by, updated_on, version)
            SELECT v.rule_key, false, v.offset_days, ''IN_APP'', ''system'', NOW(), ''system'', NOW(), 0
            FROM (VALUES
                (''BEFORE_DUE'', 5),
                (''ON_DUE'', 0),
                (''AFTER_DUE'', 5),
                (''SECOND_REMINDER'', 10),
                (''FINAL_ESCALATION'', 30)
            ) AS v(rule_key, offset_days)
            WHERE NOT EXISTS (
                SELECT 1 FROM %I.fee_reminder_rule r WHERE r.rule_key = v.rule_key)
        ', s, s);

        EXECUTE format('
            INSERT INTO %I.fee_payment_method (
                name, name_normalized, description, status, requires_reference, sort_order,
                created_by, created_on, updated_by, updated_on, version)
            SELECT v.name, lower(v.name), v.description, ''ACTIVE'', v.requires_reference, v.sort_order,
                   ''system'', NOW(), ''system'', NOW(), 0
            FROM (VALUES
                (''Cash'', ''Cash payments'', false, 1),
                (''UPI'', ''UPI payments'', true, 2),
                (''Bank Transfer'', ''Bank transfer / NEFT / RTGS'', true, 3),
                (''Cheque'', ''Cheque payments'', true, 4),
                (''Card'', ''Card payments'', false, 5),
                (''Online Payment'', ''Online gateway payments'', true, 6)
            ) AS v(name, description, requires_reference, sort_order)
            WHERE NOT EXISTS (
                SELECT 1 FROM %I.fee_payment_method m WHERE m.name_normalized = lower(v.name))
        ', s, s);
    END LOOP;
END $$;
