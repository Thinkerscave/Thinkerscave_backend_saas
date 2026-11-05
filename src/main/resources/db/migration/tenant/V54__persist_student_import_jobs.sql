CREATE TABLE IF NOT EXISTS student_import_job (
    job_id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    total_records INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failure_count INTEGER NOT NULL DEFAULT 0,
    error_report TEXT,
    completed_on TIMESTAMP,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_student_import_job_created
    ON student_import_job (created_on DESC);
