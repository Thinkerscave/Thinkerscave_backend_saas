-- V25: Teaching staff subject specialization (1 to 3 subjects per teaching staff member).
--  * links staff to the academics subject master; the schema itself is the tenant boundary.
--  * non-teaching staff have no rows here.

CREATE TABLE IF NOT EXISTS staff_subject_specialization (
    specialization_id  BIGSERIAL  PRIMARY KEY,
    staff_id           BIGINT     NOT NULL REFERENCES staff (staff_id),
    subject_id         BIGINT     NOT NULL REFERENCES subject (subject_id),
    created_on         TIMESTAMP  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_staff_subject_specialization UNIQUE (staff_id, subject_id)
);

CREATE INDEX IF NOT EXISTS idx_staff_subject_specialization_subject
    ON staff_subject_specialization (subject_id);
