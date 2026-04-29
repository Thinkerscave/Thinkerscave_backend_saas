-- Add organization_id to student and class_entity tables
ALTER TABLE student ADD COLUMN IF NOT EXISTS organization_id BIGINT;
ALTER TABLE class_entity ADD COLUMN IF NOT EXISTS organization_id BIGINT;

-- Create student_document table for tracking uploaded documents
CREATE TABLE IF NOT EXISTS student_document (
    document_id BIGSERIAL PRIMARY KEY,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(255) NOT NULL,
    document_path VARCHAR(255) NOT NULL,
    student_id BIGINT NOT NULL,
    organization_id BIGINT,
    created_by VARCHAR(50) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(50),
    last_modified_date TIMESTAMP,
    CONSTRAINT fk_student_document_student FOREIGN KEY (student_id) REFERENCES student (student_id)
);
