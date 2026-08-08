-- V1_26: Student grade-promotion batch tables (schema-per-tenant).
-- Flyway may be disabled in test/prod; safe to apply manually across tenant schemas.

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
        EXECUTE format($sql$
            CREATE TABLE IF NOT EXISTS %I.promotion_batch (
                batch_id BIGSERIAL PRIMARY KEY,
                batch_code VARCHAR(50) NOT NULL,
                from_academic_year_id BIGINT NOT NULL,
                to_academic_year_id BIGINT NOT NULL,
                status VARCHAR(30) NOT NULL,
                planned_count INTEGER DEFAULT 0,
                processed_count INTEGER DEFAULT 0,
                executed_on TIMESTAMP,
                remarks TEXT,
                created_by VARCHAR(50),
                created_on TIMESTAMP,
                updated_by VARCHAR(50),
                updated_on TIMESTAMP,
                version BIGINT DEFAULT 0
            )
        $sql$, s);

        EXECUTE format($sql$
            CREATE TABLE IF NOT EXISTS %I.promotion_record (
                record_id BIGSERIAL PRIMARY KEY,
                batch_id BIGINT NOT NULL,
                student_id BIGINT NOT NULL,
                from_enrollment_id BIGINT,
                to_enrollment_id BIGINT,
                from_class_id BIGINT,
                to_class_id BIGINT,
                decision VARCHAR(30) NOT NULL,
                reason VARCHAR(500),
                created_by VARCHAR(50),
                created_on TIMESTAMP,
                updated_by VARCHAR(50),
                updated_on TIMESTAMP,
                version BIGINT DEFAULT 0,
                CONSTRAINT uk_promotion_batch_student UNIQUE (batch_id, student_id)
            )
        $sql$, s);

        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_promotion_batch_status ON %I.promotion_batch(status)',
            s
        );
        EXECUTE format(
            'CREATE INDEX IF NOT EXISTS idx_promotion_record_batch ON %I.promotion_record(batch_id)',
            s
        );
    END LOOP;
END $$;
