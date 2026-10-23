-- Staff remarks were never used by any workflow; the column and UI field are removed.
ALTER TABLE staff DROP COLUMN IF EXISTS remarks;
