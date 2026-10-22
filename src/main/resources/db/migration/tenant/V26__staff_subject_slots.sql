-- V26: Subject slots on staff. Primary is required for teaching staff; secondary and tertiary are optional.
--  * V25 rows are copied into the slots, oldest first. The V25 table is kept, not dropped.

ALTER TABLE staff
    ADD COLUMN IF NOT EXISTS primary_subject_id   BIGINT REFERENCES subject (subject_id),
    ADD COLUMN IF NOT EXISTS secondary_subject_id BIGINT REFERENCES subject (subject_id),
    ADD COLUMN IF NOT EXISTS tertiary_subject_id  BIGINT REFERENCES subject (subject_id);

UPDATE staff s
SET primary_subject_id = (
        SELECT ss.subject_id FROM staff_subject_specialization ss
        WHERE ss.staff_id = s.staff_id
        ORDER BY ss.created_on, ss.specialization_id LIMIT 1 OFFSET 0),
    secondary_subject_id = (
        SELECT ss.subject_id FROM staff_subject_specialization ss
        WHERE ss.staff_id = s.staff_id
        ORDER BY ss.created_on, ss.specialization_id LIMIT 1 OFFSET 1),
    tertiary_subject_id = (
        SELECT ss.subject_id FROM staff_subject_specialization ss
        WHERE ss.staff_id = s.staff_id
        ORDER BY ss.created_on, ss.specialization_id LIMIT 1 OFFSET 2)
WHERE EXISTS (SELECT 1 FROM staff_subject_specialization ss WHERE ss.staff_id = s.staff_id);
