-- V27: Remove the third subject slot and the V25 join table. Primary and secondary subjects stay on staff.

ALTER TABLE staff DROP COLUMN IF EXISTS tertiary_subject_id;
DROP TABLE IF EXISTS staff_subject_specialization;
