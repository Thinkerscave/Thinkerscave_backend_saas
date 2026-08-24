-- Admissions CRM production columns and tables (public + tenant_* schemas).
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
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = s AND table_name = 'inquiry'
        ) THEN
            EXECUTE format('ALTER TABLE %I.inquiry ADD COLUMN IF NOT EXISTS inquiry_number varchar(40)', s);
            EXECUTE format('ALTER TABLE %I.inquiry ADD COLUMN IF NOT EXISTS academic_year_id bigint', s);
            EXECUTE format('ALTER TABLE %I.inquiry ADD COLUMN IF NOT EXISTS class_id bigint', s);
            EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS uq_inquiry_number ON %I.inquiry (inquiry_number) WHERE inquiry_number IS NOT NULL', s);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = s AND table_name = 'inquiry_follow_up'
        ) THEN
            EXECUTE format('ALTER TABLE %I.inquiry_follow_up ADD COLUMN IF NOT EXISTS lifecycle_status varchar(20) DEFAULT ''SCHEDULED''', s);
            EXECUTE format('ALTER TABLE %I.inquiry_follow_up ADD COLUMN IF NOT EXISTS outcome varchar(200)', s);
            EXECUTE format('ALTER TABLE %I.inquiry_follow_up ADD COLUMN IF NOT EXISTS completed_on timestamp', s);
            EXECUTE format('ALTER TABLE %I.inquiry_follow_up ADD COLUMN IF NOT EXISTS completed_by varchar(100)', s);
        END IF;

        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = s AND table_name = 'application_admission'
        ) THEN
            EXECUTE format('ALTER TABLE %I.application_admission ALTER COLUMN status TYPE varchar(40)', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS academic_year_id bigint', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS class_id bigint', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS section_id bigint', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS student_id bigint', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS fee_amount numeric(12,2)', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS fee_receipt_number varchar(60)', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS fee_payment_mode varchar(40)', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS fee_paid_on date', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS fee_received_by varchar(100)', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS fee_remarks text', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS fee_status varchar(20) DEFAULT ''PENDING''', s);
            EXECUTE format('ALTER TABLE %I.application_admission ADD COLUMN IF NOT EXISTS profile_details text', s);
        END IF;

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I.admissions_setting (
                setting_id bigserial PRIMARY KEY,
                organization_id bigint NOT NULL UNIQUE,
                inquiry_sources text,
                inquiry_statuses text,
                required_documents text,
                lead_prefix varchar(20) DEFAULT ''LD'',
                application_prefix varchar(20) DEFAULT ''APP'',
                admission_prefix varchar(20) DEFAULT ''ADM'',
                reminder_mode varchar(30) DEFAULT ''AUTO'',
                reminder_lead_time varchar(20) DEFAULT ''24H'',
                assignment_mode varchar(30) DEFAULT ''MANUAL'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0
            )', s);

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I.admission_application_document (
                document_id bigserial PRIMARY KEY,
                application_id bigint NOT NULL,
                document_type varchar(80) NOT NULL,
                original_name varchar(255),
                stored_path varchar(500),
                status varchar(20) NOT NULL DEFAULT ''PENDING'',
                remarks text,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_aad_application ON %I.admission_application_document (application_id)', s);
    END LOOP;
END $$;
