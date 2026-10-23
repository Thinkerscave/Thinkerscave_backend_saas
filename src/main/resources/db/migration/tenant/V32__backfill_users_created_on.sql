-- Rows created before JPA auditing populated created_on have NULL values.
-- Backfill them once here; read paths must not write to the database.
UPDATE users
SET created_on = COALESCE(updated_on, CURRENT_TIMESTAMP)
WHERE created_on IS NULL;
