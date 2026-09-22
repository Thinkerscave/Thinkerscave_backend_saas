-- Finance ΓåÆ Payroll V1 schema (public + tenant_*).
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
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.payroll_configuration (
                payroll_configuration_id bigserial PRIMARY KEY,
                frequency varchar(20) NOT NULL DEFAULT ''MONTHLY'',
                default_generation_day smallint NOT NULL DEFAULT 1,
                approval_required boolean NOT NULL DEFAULT true,
                default_payment_method_id bigint,
                pf_enabled boolean NOT NULL DEFAULT false,
                esi_enabled boolean NOT NULL DEFAULT false,
                professional_tax_enabled boolean NOT NULL DEFAULT false,
                tds_enabled boolean NOT NULL DEFAULT false,
                working_days_basis varchar(30) NOT NULL DEFAULT ''CALENDAR'',
                include_paid_leave boolean NOT NULL DEFAULT true,
                lop_handling varchar(40) NOT NULL DEFAULT ''PRORATE_GROSS'',
                salary_rounding varchar(30) NOT NULL DEFAULT ''NEAREST_RUPEE'',
                payslip_number_format varchar(80),
                payslip_org_name varchar(200),
                show_org_logo boolean NOT NULL DEFAULT true,
                show_authorized_signatory boolean NOT NULL DEFAULT true,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_payroll_cfg_freq CHECK (frequency = ''MONTHLY''),
                CONSTRAINT ck_payroll_cfg_gen_day CHECK (default_generation_day BETWEEN 1 AND 28)
            )', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.salary_component (
                salary_component_id bigserial PRIMARY KEY,
                code varchar(40) NOT NULL,
                name varchar(100) NOT NULL,
                component_type varchar(30) NOT NULL,
                calculation_method varchar(30) NOT NULL,
                default_value numeric(12,2),
                statutory_code varchar(20),
                status varchar(20) NOT NULL DEFAULT ''ACTIVE'',
                sort_order int NOT NULL DEFAULT 0,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_salary_component_code UNIQUE (code),
                CONSTRAINT ck_salary_component_type CHECK (component_type IN (
                    ''EARNING'',''DEDUCTION'',''EMPLOYER_CONTRIBUTION'')),
                CONSTRAINT ck_salary_component_calc CHECK (calculation_method IN (
                    ''FIXED_AMOUNT'',''PERCENTAGE_OF_BASIC'',''PERCENTAGE_OF_GROSS'',''MANUAL_ENTRY'')),
                CONSTRAINT ck_salary_component_status CHECK (status IN (''ACTIVE'',''INACTIVE''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_salary_component_status ON %I.salary_component (status)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.salary_structure (
                salary_structure_id bigserial PRIMARY KEY,
                name varchar(150) NOT NULL,
                description varchar(500),
                applicable_staff_type varchar(30),
                status varchar(20) NOT NULL DEFAULT ''ACTIVE'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_salary_structure_status CHECK (status IN (''ACTIVE'',''INACTIVE''))
            )', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.salary_structure_item (
                salary_structure_item_id bigserial PRIMARY KEY,
                salary_structure_id bigint NOT NULL REFERENCES %I.salary_structure (salary_structure_id),
                salary_component_id bigint NOT NULL REFERENCES %I.salary_component (salary_component_id),
                calculation_method varchar(30) NOT NULL,
                value numeric(12,2) NOT NULL,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_salary_structure_item UNIQUE (salary_structure_id, salary_component_id),
                CONSTRAINT ck_ssi_calc CHECK (calculation_method IN (
                    ''FIXED_AMOUNT'',''PERCENTAGE_OF_BASIC'',''PERCENTAGE_OF_GROSS'',''MANUAL_ENTRY''))
            )', s, s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_ssi_structure ON %I.salary_structure_item (salary_structure_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.employee_salary (
                employee_salary_id bigserial PRIMARY KEY,
                staff_id bigint NOT NULL,
                payment_type varchar(20) NOT NULL,
                salary_structure_id bigint,
                effective_from date NOT NULL,
                effective_to date,
                active boolean NOT NULL DEFAULT true,
                bank_name varchar(150),
                account_holder_name varchar(150),
                account_number varchar(50),
                ifsc_code varchar(20),
                remarks text,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT ck_employee_salary_payment_type CHECK (payment_type IN (''SALARY'',''STIPEND''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_employee_salary_staff_active ON %I.employee_salary (staff_id, active)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_employee_salary_staff_from ON %I.employee_salary (staff_id, effective_from)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.employee_salary_component (
                employee_salary_component_id bigserial PRIMARY KEY,
                employee_salary_id bigint NOT NULL REFERENCES %I.employee_salary (employee_salary_id),
                salary_component_id bigint NOT NULL REFERENCES %I.salary_component (salary_component_id),
                component_type varchar(30) NOT NULL,
                calculation_method varchar(30) NOT NULL,
                value numeric(12,2) NOT NULL,
                applicable boolean NOT NULL DEFAULT true,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_employee_salary_component UNIQUE (employee_salary_id, salary_component_id)
            )', s, s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_esc_salary ON %I.employee_salary_component (employee_salary_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.payroll_run (
                payroll_run_id bigserial PRIMARY KEY,
                payroll_year int NOT NULL,
                payroll_month int NOT NULL,
                status varchar(30) NOT NULL,
                generated_on timestamptz,
                approved_on timestamptz,
                approved_by varchar(100),
                returned_on timestamptz,
                notes text,
                idempotency_key varchar(120),
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_payroll_run_year_month UNIQUE (payroll_year, payroll_month),
                CONSTRAINT ck_payroll_run_month CHECK (payroll_month BETWEEN 1 AND 12)
            )', s);
        EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS uk_payroll_run_idempotency
            ON %I.payroll_run (idempotency_key) WHERE idempotency_key IS NOT NULL', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.employee_payroll (
                employee_payroll_id bigserial PRIMARY KEY,
                payroll_run_id bigint NOT NULL REFERENCES %I.payroll_run (payroll_run_id),
                staff_id bigint NOT NULL,
                payroll_year int NOT NULL,
                payroll_month int NOT NULL,
                payment_type varchar(20) NOT NULL,
                employment_category varchar(30),
                designation varchar(100),
                working_days int,
                present_days int,
                paid_leave_days int,
                lop_days int,
                gross_amount numeric(12,2) NOT NULL,
                total_deductions numeric(12,2) NOT NULL,
                net_amount numeric(12,2) NOT NULL,
                status varchar(30) NOT NULL,
                payslip_document_id bigint,
                payslip_number varchar(60),
                employee_salary_id bigint,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_employee_payroll_staff_period UNIQUE (staff_id, payroll_year, payroll_month),
                CONSTRAINT ck_employee_payroll_payment_type CHECK (payment_type IN (''SALARY'',''STIPEND''))
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_employee_payroll_run ON %I.employee_payroll (payroll_run_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_employee_payroll_status ON %I.employee_payroll (status)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.employee_payroll_line (
                employee_payroll_line_id bigserial PRIMARY KEY,
                employee_payroll_id bigint NOT NULL REFERENCES %I.employee_payroll (employee_payroll_id),
                salary_component_id bigint,
                component_code varchar(40) NOT NULL,
                component_name varchar(100) NOT NULL,
                component_type varchar(30) NOT NULL,
                calculation_method varchar(30) NOT NULL,
                rate_or_percent numeric(12,2),
                amount numeric(12,2) NOT NULL,
                sort_order int NOT NULL DEFAULT 0,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_epl_payroll ON %I.employee_payroll_line (employee_payroll_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.payroll_payment (
                payroll_payment_id bigserial PRIMARY KEY,
                employee_payroll_id bigint NOT NULL REFERENCES %I.employee_payroll (employee_payroll_id),
                amount numeric(12,2) NOT NULL,
                paid_on date NOT NULL,
                payment_method_id bigint,
                payment_method_name varchar(80),
                reference_number varchar(100),
                remarks text,
                idempotency_key varchar(120) NOT NULL,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_payroll_payment_idempotency UNIQUE (idempotency_key),
                CONSTRAINT ck_payroll_payment_amount CHECK (amount > 0)
            )', s, s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_payroll_payment_ep ON %I.payroll_payment (employee_payroll_id)', s);

        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.payroll_migration_log (
                payroll_migration_log_id bigserial PRIMARY KEY,
                source_table varchar(80) NOT NULL,
                source_id bigint,
                target_table varchar(80),
                target_id bigint,
                message varchar(500),
                created_on timestamp DEFAULT CURRENT_TIMESTAMP
            )', s);
    END LOOP;
END $$;
