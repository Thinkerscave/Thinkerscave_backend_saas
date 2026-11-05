CREATE TABLE IF NOT EXISTS document (
    document_id BIGSERIAL PRIMARY KEY,
    owner_type VARCHAR(50) NOT NULL,
    owner_id BIGINT NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_extension VARCHAR(20),
    file_size BIGINT,
    mime_type VARCHAR(100),
    remarks TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    source_document_type VARCHAR(50),
    source_document_id BIGINT,
    source_application_document_id BIGINT,
    replaced_document_id BIGINT,
    uploaded_on TIMESTAMP,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE document ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE document ADD COLUMN IF NOT EXISTS source_document_type VARCHAR(50);
ALTER TABLE document ADD COLUMN IF NOT EXISTS source_document_id BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS source_application_document_id BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS replaced_document_id BIGINT;
ALTER TABLE document ADD COLUMN IF NOT EXISTS uploaded_on TIMESTAMP;
ALTER TABLE document ALTER COLUMN file_path TYPE VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_document_owner_active
    ON document (owner_type, owner_id, active, document_id DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_document_legacy_source
    ON document (source_document_type, source_document_id)
        WHERE source_document_id IS NOT NULL
            AND source_document_type IN ('STUDENT_LEGACY', 'ADMISSION_LEGACY');

DO $$
BEGIN
    IF to_regclass('student_document') IS NOT NULL THEN
        INSERT INTO document (
            owner_type, owner_id, document_type, document_name, file_name, file_path,
            file_extension, file_size, mime_type, remarks, active, verification_status,
            source_document_type, source_document_id, replaced_document_id, uploaded_on,
            source_application_document_id, created_by, created_on
        )
        SELECT 'STUDENT', student_id, LEFT(document_type, 100), document_name,
               COALESCE(NULLIF(file_name, ''), document_name), document_path,
               file_extension, file_size, mime_type, remarks, active, status,
               'STUDENT_LEGACY', document_id, replaced_document_id, uploaded_on,
               source_application_document_id,
               created_by, uploaded_on
        FROM student_document s
        WHERE NULLIF(BTRIM(document_path), '') IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM document d
              WHERE d.source_document_type = 'STUDENT_LEGACY'
                AND d.source_document_id = s.document_id
          );
    END IF;

    IF to_regclass('admission_application_document') IS NOT NULL THEN
        INSERT INTO document (
            owner_type, owner_id, document_type, document_name, file_name, file_path,
            remarks, active, verification_status, source_document_type, source_document_id,
            source_application_document_id, uploaded_on, created_by, created_on, updated_by, updated_on, version
        )
        SELECT 'ADMISSION', application_id, LEFT(document_type, 100),
               COALESCE(NULLIF(original_name, ''), document_type),
               COALESCE(NULLIF(original_name, ''), document_type), stored_path,
               remarks, TRUE, status, 'ADMISSION_LEGACY', document_id, document_id,
               created_on, created_by, created_on, updated_by, updated_on, version
        FROM admission_application_document a
        WHERE NULLIF(BTRIM(stored_path), '') IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM document d
              WHERE d.source_document_type = 'ADMISSION_LEGACY'
                AND d.source_document_id = a.document_id
          );
    END IF;
END $$;