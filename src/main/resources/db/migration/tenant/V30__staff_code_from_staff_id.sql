-- Staff codes are now derived from staff_id after insert, so the per-tenant counter table is no longer used.
-- Existing staff codes are unchanged. staff_code is nullable only so a row can be inserted before its code is set.
ALTER TABLE staff ALTER COLUMN staff_code DROP NOT NULL;

DROP TABLE IF EXISTS staff_code_sequence;
