-- Shared managed document registry (public + tenant_*).
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
            CREATE TABLE IF NOT EXISTS %I.managed_document (
                managed_document_id bigserial PRIMARY KEY,
                document_type varchar(40) NOT NULL,
                owner_type varchar(40) NOT NULL,
                owner_id bigint NOT NULL,
                period_key varchar(20),
                storage_path varchar(500) NOT NULL,
                file_name varchar(255) NOT NULL,
                content_type varchar(100) NOT NULL,
                content_hash varchar(64) NOT NULL,
                byte_size bigint NOT NULL,
                idempotency_key varchar(120) NOT NULL,
                status varchar(20) NOT NULL DEFAULT ''ACTIVE'',
                created_by varchar(100),
                created_on timestamp,
                updated_by varchar(100),
                updated_on timestamp,
                version bigint NOT NULL DEFAULT 0,
                CONSTRAINT uk_managed_document_idem UNIQUE (document_type, owner_type, owner_id, idempotency_key),
                CONSTRAINT ck_managed_document_status CHECK (status IN (''ACTIVE'',''SUPERSEDED''))
            )', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_managed_document_type_period
            ON %I.managed_document (document_type, period_key)', s);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_managed_document_owner
            ON %I.managed_document (owner_type, owner_id)', s);
    END LOOP;
END $$;
