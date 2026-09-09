-- Align admissions lead data model with Lead 360 workflow across public + tenant schemas.
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
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = s
              AND table_name = 'inquiry'
        ) THEN
            EXECUTE format('ALTER TABLE %I.inquiry ADD COLUMN IF NOT EXISTS student_name varchar(100)', s);
            EXECUTE format('ALTER TABLE %I.inquiry ADD COLUMN IF NOT EXISTS parent_contact_name varchar(100)', s);

            EXECUTE format('UPDATE %I.inquiry SET student_name = COALESCE(NULLIF(TRIM(student_name), ''''), NULLIF(TRIM(name), '''')) WHERE student_name IS NULL OR TRIM(student_name) = ''''', s);
            EXECUTE format('UPDATE %I.inquiry SET parent_contact_name = COALESCE(NULLIF(TRIM(parent_contact_name), ''''), NULLIF(TRIM(name), '''')) WHERE parent_contact_name IS NULL OR TRIM(parent_contact_name) = ''''', s);

            EXECUTE format($f$
                UPDATE %I.inquiry
                SET inquiry_source = CASE UPPER(REPLACE(COALESCE(inquiry_source, ''OTHER''), ' ', '_'))
                    WHEN 'WEBSITE' THEN 'WEBSITE'
                    WHEN 'PHONE' THEN 'PHONE'
                    WHEN 'WALK-IN' THEN 'WALK_IN'
                    WHEN 'WALK_IN' THEN 'WALK_IN'
                    WHEN 'REFERRAL' THEN 'REFERRAL'
                    WHEN 'WHATSAPP' THEN 'WHATSAPP'
                    WHEN 'SOCIAL_MEDIA' THEN 'SOCIAL_MEDIA'
                    WHEN 'CAMPAIGN' THEN 'CAMPAIGN'
                    WHEN 'AFFILIATE' THEN 'AFFILIATE'
                    WHEN 'IMPORT' THEN 'IMPORT'
                    ELSE 'OTHER'
                END
            $f$, s);

            EXECUTE format($f$
                UPDATE %I.inquiry
                SET status = CASE UPPER(COALESCE(status, 'NEW'))
                    WHEN 'FOLLOW_UP' THEN 'CONTACTED'
                    WHEN 'MEETING_SCHEDULED' THEN 'CONTACTED'
                    WHEN 'COUNSELING' THEN 'INTERESTED'
                    WHEN 'DOCUMENTS_PENDING' THEN 'INTERESTED'
                    WHEN 'FOLLOW_UP_REQUIRED' THEN 'CONTACTED'
                    WHEN 'READY_FOR_ADMISSION' THEN 'INTERESTED'
                    WHEN 'CONVERTED' THEN 'APPLICATION_SUBMITTED'
                    WHEN 'CLOSED' THEN 'LOST'
                    ELSE UPPER(status)
                END
            $f$, s);

            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_inq_student_name ON %I.inquiry (student_name)', s);
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_inq_parent_contact_name ON %I.inquiry (parent_contact_name)', s);
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_inq_created_on ON %I.inquiry (created_on)', s);
            EXECUTE format('CREATE INDEX IF NOT EXISTS idx_inq_class_id ON %I.inquiry (class_id)', s);
        END IF;

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I.lead_counselor_assignment (
                assignment_id bigserial PRIMARY KEY,
                inquiry_id bigint NOT NULL,
                previous_counselor_staff_id bigint,
                new_counselor_staff_id bigint NOT NULL,
                reason varchar(300),
                assigned_by_user_id bigint,
                assigned_by_username varchar(100),
                assigned_on timestamp NOT NULL,
                active boolean NOT NULL DEFAULT true,
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0
            )', s);

        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_lca_inquiry ON %I.lead_counselor_assignment (inquiry_id)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_lca_active ON %I.lead_counselor_assignment (active)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_lca_new_counselor ON %I.lead_counselor_assignment (new_counselor_staff_id)', s);
    END LOOP;
END $$;
