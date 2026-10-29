-- Remove the legacy full-slot constraint so V46 can allow validated choice alternatives.
ALTER TABLE timetable_entry
    DROP CONSTRAINT IF EXISTS timetable_entry_timetable_version_id_section_id_day_of_week_key;
