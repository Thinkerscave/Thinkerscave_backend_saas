-- Retire the Alumni and Transfer Requests Student submenu entries.
-- Preserve menu rows and associated history; only remove them from navigation.
UPDATE menus
SET active = FALSE,
    show_in_sidebar = FALSE,
    version = COALESCE(version, 0) + 1
WHERE menu_code IN ('STUDENTS_ALUMNI', 'STUDENTS_TRANSFERS', 'STUDENT_TRANSFERS')
  AND (active IS DISTINCT FROM FALSE OR show_in_sidebar IS DISTINCT FROM FALSE);