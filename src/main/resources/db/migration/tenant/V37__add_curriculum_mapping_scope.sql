ALTER TABLE academic_group_subject_mapping
    ADD COLUMN requirement_type VARCHAR(20) NOT NULL DEFAULT 'COMPULSORY',
    ADD COLUMN scope_type VARCHAR(30) NOT NULL DEFAULT 'ALL_SECTIONS';

CREATE TABLE academic_group_subject_mapping_section (
    academic_group_subject_mapping_section_id BIGSERIAL PRIMARY KEY,
    academic_group_subject_mapping_id BIGINT NOT NULL
        REFERENCES academic_group_subject_mapping(academic_group_subject_mapping_id) ON DELETE CASCADE,
    section_id BIGINT NOT NULL REFERENCES academic_section(section_id),
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_by VARCHAR(100),
    updated_on TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_group_subject_mapping_section
        UNIQUE (academic_group_subject_mapping_id, section_id)
);

CREATE INDEX idx_group_subject_mapping_section_section
    ON academic_group_subject_mapping_section (section_id);