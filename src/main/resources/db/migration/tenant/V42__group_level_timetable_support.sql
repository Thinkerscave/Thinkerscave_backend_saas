-- V42: Allow timetable lessons for academic groups that have no sections.
-- Teacher allocations remain section-scoped, so a group-level lesson links to the
-- TeacherGroupAllocation's staff member through staff_id (teacher_allocation_id is null).

ALTER TABLE timetable_entry
    DROP CONSTRAINT IF EXISTS chk_timetable_entry_subject;

ALTER TABLE timetable_entry
    ADD CONSTRAINT chk_timetable_entry_subject
    CHECK (
        entry_type <> 'SUBJECT'
        OR (
            (section_id IS NOT NULL OR academic_group_id IS NOT NULL)
            AND (teacher_allocation_id IS NOT NULL OR staff_id IS NOT NULL)
        )
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_timetable_entry_group_slot
    ON timetable_entry (
        timetable_version_id, academic_group_id, day_of_week, timetable_period_id
    )
    WHERE section_id IS NULL AND academic_group_id IS NOT NULL;

-- Rollback: drop the index, restore the original subject check constraint.
