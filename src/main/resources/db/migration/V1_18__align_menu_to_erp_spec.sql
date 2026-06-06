-- ============================================================================
-- Migration: V1_18__align_menu_to_erp_spec.sql
-- Purpose : Align menu_master / sub_menu_master to the ThinkersCave ERP spec.
--           - Top-level menus renamed and routed to spec workspaces.
--           - Per-module sub-menus collapsed to the spec taxonomy (3 Admissions,
--             5 Students, 5 Staff, 6 Academics, etc.). Legacy entries are kept
--             reachable but flagged inactive (is_active = FALSE) so privilege
--             mappings remain valid and historical URLs stay redirectable.
--
-- Idempotent: uses UPDATE / INSERT...WHERE NOT EXISTS so re-running is safe.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Top-level menu_master alignment.
-- ---------------------------------------------------------------------------
UPDATE menu_master SET name = 'Student Management',
                        description = 'Student lifecycle workspace',
                        url         = '/app/students/directory',
                        icon        = 'pi pi-users'
WHERE menu_code = 'MENU_STUDENT';

UPDATE menu_master SET name = 'Staff Management',
                        description = 'Employee lifecycle workspace',
                        url         = '/app/staff/directory',
                        icon        = 'pi pi-id-card'
WHERE menu_code = 'MENU_STAFF';

UPDATE menu_master SET name = 'Academics',
                        description = 'Academic setup, timetable, syllabus',
                        url         = '/app/academics/academic-setup',
                        icon        = 'pi pi-book'
WHERE menu_code = 'MENU_ACADEMICS';

UPDATE menu_master SET name = 'Admissions',
                        description = 'Inquiry Center, Admission Center, Settings',
                        url         = '/app/admissions/inquiry-center',
                        icon        = 'pi pi-inbox'
WHERE menu_code = 'MENU_INQUIRY';

-- ---------------------------------------------------------------------------
-- 2. Admissions module sub_menu_master: keep three spec entries active.
-- ---------------------------------------------------------------------------
UPDATE sub_menu_master SET sub_menu_name        = 'Inquiry Center',
                            sub_menu_description = 'Inquiry pipeline, follow-ups, counseling',
                            sub_menu_url         = '/app/admissions/inquiry-center',
                            sub_menu_icon        = 'pi pi-sparkles',
                            sub_menu_order       = 1,
                            is_active            = TRUE
WHERE sub_menu_code = 'INQ_DASHBOARD';

UPDATE sub_menu_master SET sub_menu_name        = 'Admission Center',
                            sub_menu_description = 'Applications, documents, enrollment',
                            sub_menu_url         = '/app/admissions/admission-center',
                            sub_menu_icon        = 'pi pi-file-edit',
                            sub_menu_order       = 2,
                            is_active            = TRUE
WHERE sub_menu_code = 'INQ_APPLICATIONS';

INSERT INTO sub_menu_master (sub_menu_name, sub_menu_code, sub_menu_description, sub_menu_url, sub_menu_icon, sub_menu_order, is_active, menu_id)
SELECT 'Settings', 'INQ_SETTINGS',
       'Sources, statuses, required documents, configuration',
       '/app/admissions/settings', 'pi pi-cog', 3, TRUE,
       (SELECT menu_id FROM menu_master WHERE menu_code = 'MENU_INQUIRY')
WHERE NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = 'INQ_SETTINGS');

UPDATE sub_menu_master
   SET is_active            = FALSE,
       sub_menu_description = COALESCE(sub_menu_description, '') ||
                              CASE WHEN COALESCE(sub_menu_description, '') = ''
                                   THEN 'Folded into Inquiry Center / Admission Center.'
                                   ELSE ' (folded into Inquiry Center / Admission Center)' END
 WHERE sub_menu_code IN ('INQ_PIPELINE','INQ_MANAGEMENT','INQ_FOLLOWUP_CENTER',
                         'INQ_COUNSELING','INQ_DOCUMENTS','INQ_COMMUNICATION',
                         'INQ_ANALYTICS');

-- ---------------------------------------------------------------------------
-- 3. Student Management sub_menu_master: keep five spec entries active.
-- ---------------------------------------------------------------------------
UPDATE sub_menu_master SET sub_menu_name        = 'Students',
                            sub_menu_description = 'Active, inactive, and alumni records',
                            sub_menu_url         = '/app/students/directory',
                            sub_menu_icon        = 'pi pi-users',
                            sub_menu_order       = 1,
                            is_active            = TRUE
WHERE sub_menu_code = 'STUDENT_DIRECTORY';

UPDATE sub_menu_master SET sub_menu_name        = 'Academic Movement',
                            sub_menu_description = 'Promotion and academic progression',
                            sub_menu_url         = '/app/students/academic-movement',
                            sub_menu_icon        = 'pi pi-arrow-up-right',
                            sub_menu_order       = 2,
                            is_active            = TRUE
WHERE sub_menu_code = 'STUDENT_PROMOTION';

UPDATE sub_menu_master SET sub_menu_name        = 'Student Movement',
                            sub_menu_description = 'Transfers, withdrawals, readmissions',
                            sub_menu_url         = '/app/students/student-movement',
                            sub_menu_icon        = 'pi pi-send',
                            sub_menu_order       = 3,
                            is_active            = TRUE
WHERE sub_menu_code = 'STUDENT_TRANSFER';

UPDATE sub_menu_master SET sub_menu_name        = 'Documents',
                            sub_menu_description = 'Student document vault',
                            sub_menu_url         = '/app/students/documents',
                            sub_menu_icon        = 'pi pi-folder',
                            sub_menu_order       = 4,
                            is_active            = TRUE
WHERE sub_menu_code = 'STUDENT_DOCUMENTS';

UPDATE sub_menu_master SET sub_menu_name        = 'Alumni',
                            sub_menu_description = 'Alumni lifecycle records',
                            sub_menu_url         = '/app/students/alumni',
                            sub_menu_icon        = 'pi pi-verified',
                            sub_menu_order       = 5,
                            is_active            = TRUE
WHERE sub_menu_code = 'STUDENT_ALUMNI';

UPDATE sub_menu_master
   SET is_active            = FALSE,
       sub_menu_description = 'Folded into Student 360 / Admissions / Academics.'
 WHERE sub_menu_code IN ('STUDENT_DASHBOARD','STUDENT_PROFILES','STUDENT_ADMISSIONS',
                         'STUDENT_CLASSES','STUDENT_SECTIONS','STUDENT_PARENTS','STUDENT_ID_CARDS');

-- ---------------------------------------------------------------------------
-- 4. Academics sub_menu_master: keep six spec entries active.
-- ---------------------------------------------------------------------------
UPDATE sub_menu_master SET sub_menu_name        = 'Academic Setup',
                            sub_menu_description = 'Configure academic year, classes, sections, subjects, teachers',
                            sub_menu_url         = '/app/academics/academic-setup',
                            sub_menu_icon        = 'pi pi-cog',
                            sub_menu_order       = 1,
                            is_active            = TRUE
WHERE sub_menu_code = 'ACAD_DASHBOARD';

UPDATE sub_menu_master SET sub_menu_name        = 'Timetable',
                            sub_menu_description = 'Academic timetable scheduler',
                            sub_menu_url         = '/app/academics/timetable',
                            sub_menu_icon        = 'pi pi-table',
                            sub_menu_order       = 2,
                            is_active            = TRUE
WHERE sub_menu_code = 'ACAD_TIMETABLE';

UPDATE sub_menu_master SET sub_menu_name        = 'Teacher Arrangement',
                            sub_menu_description = 'Substitute teacher arrangement and workload',
                            sub_menu_url         = '/app/academics/teacher-arrangement',
                            sub_menu_icon        = 'pi pi-user-plus',
                            sub_menu_order       = 3,
                            is_active            = TRUE
WHERE sub_menu_code = 'ACAD_TEACHER_ALLOCATION';

UPDATE sub_menu_master SET sub_menu_name        = 'Academic Calendar',
                            sub_menu_description = 'Academic calendar events and milestones',
                            sub_menu_url         = '/app/academics/academic-calendar',
                            sub_menu_icon        = 'pi pi-calendar',
                            sub_menu_order       = 4,
                            is_active            = TRUE
WHERE sub_menu_code = 'ACAD_CALENDAR';

UPDATE sub_menu_master SET sub_menu_name        = 'Syllabus Tracker',
                            sub_menu_description = 'Syllabus planning and completion tracking',
                            sub_menu_url         = '/app/academics/syllabus-tracker',
                            sub_menu_icon        = 'pi pi-list-check',
                            sub_menu_order       = 5,
                            is_active            = TRUE
WHERE sub_menu_code = 'ACAD_SYLLABUS';

UPDATE sub_menu_master SET sub_menu_name        = 'Settings',
                            sub_menu_description = 'Academic operating rules and preferences',
                            sub_menu_url         = '/app/academics/settings',
                            sub_menu_icon        = 'pi pi-cog',
                            sub_menu_order       = 6,
                            is_active            = TRUE
WHERE sub_menu_code = 'ACAD_SETTINGS';

UPDATE sub_menu_master
   SET is_active            = FALSE,
       sub_menu_description = 'Folded into Academic Setup / Syllabus Tracker / Teacher Arrangement.'
 WHERE sub_menu_code IN ('ACAD_YEARS','ACAD_CLASSES_SECTIONS','ACAD_SUBJECTS',
                         'ACAD_CURRICULUM','ACAD_CLASS_TEACHER','ACAD_HIERARCHY');

-- ---------------------------------------------------------------------------
-- 5. Staff Management sub_menu_master: align top-level label.
--     Spec page rebuild lands in P5; for now ensure Employees is the active
--     primary entry and legacy directory/operations are reachable but secondary.
-- ---------------------------------------------------------------------------
UPDATE sub_menu_master SET sub_menu_name        = 'Employees',
                            sub_menu_description = 'Employee directory and lifecycle',
                            sub_menu_url         = '/app/staff/directory',
                            sub_menu_icon        = 'pi pi-id-card',
                            sub_menu_order       = 1,
                            is_active            = TRUE
WHERE sub_menu_code = 'STAFF_DASHBOARD';

UPDATE sub_menu_master SET is_active = FALSE,
                            sub_menu_description = 'Legacy directory entry retained inactive after consolidation.'
WHERE sub_menu_code = 'STAFF_DIRECTORY';

-- ---------------------------------------------------------------------------
-- 6. Wire INQ_SETTINGS into role/privilege mappings for admin-tier roles.
-- ---------------------------------------------------------------------------
INSERT INTO submenu_privilege_mapping (sub_menu_id, privilege_id)
SELECT sm.sub_menu_id, p.privilege_id
  FROM sub_menu_master sm
  CROSS JOIN privilege_master p
 WHERE sm.sub_menu_code = 'INQ_SETTINGS'
   AND UPPER(p.privilege_name) IN ('VIEW','ADD','EDIT','DELETE')
   AND NOT EXISTS (
       SELECT 1 FROM submenu_privilege_mapping spm
        WHERE spm.sub_menu_id = sm.sub_menu_id
          AND spm.privilege_id = p.privilege_id
   );

INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id)
SELECT r.role_id, sm.sub_menu_id, p.privilege_id
  FROM role_master r
  CROSS JOIN sub_menu_master sm
  CROSS JOIN privilege_master p
 WHERE UPPER(r.role_name) IN ('SUPER_ADMIN','SUPERADMIN','ADMIN','PRINCIPAL')
   AND sm.sub_menu_code = 'INQ_SETTINGS'
   AND UPPER(p.privilege_name) IN ('VIEW','ADD','EDIT','DELETE')
   AND NOT EXISTS (
       SELECT 1 FROM role_submenu_privilege_mapping rpm
        WHERE rpm.role_id      = r.role_id
          AND rpm.sub_menu_id  = sm.sub_menu_id
          AND rpm.privilege_id = p.privilege_id
   );

-- ============================================================================
-- Migration complete
-- ============================================================================
