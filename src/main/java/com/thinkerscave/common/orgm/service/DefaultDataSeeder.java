package com.thinkerscave.common.orgm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Seeds default data for newly created tenant schemas.
 *
 * This includes:
 * - Default roles (SUPER_ADMIN, ADMIN, IT_SUPPORT, etc.)
 * - Standard CRUD privileges (VIEW, ADD, EDIT, DELETE, APPROVE)
 * - Default menu and sub-menu structure
 * - The Role -> SubMenu -> Privilege Matrix Mapping
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultDataSeeder {

    private final JdbcTemplate jdbcTemplate;

    // Default roles to seed
    private static final List<RoleDefinition> DEFAULT_ROLES = Arrays.asList(
            new RoleDefinition("SUPER_ADMIN", "Super Administrator with full system access", "SYSTEM"),
            new RoleDefinition("IT_SUPPORT", "Technical Support Administrator", "SYSTEM"),
            new RoleDefinition("ADMIN", "Organization Administrator", "SCHOOL"),
            new RoleDefinition("TEACHER", "Teacher/Instructor role", "SCHOOL"),
            new RoleDefinition("STAFF", "General staff member", "SCHOOL"),
            new RoleDefinition("STUDENT", "Student role", "SCHOOL"),
            new RoleDefinition("PARENT", "Parent/Guardian role", "SCHOOL"),
            new RoleDefinition("USER", "Basic user role", "SCHOOL"));

    // Standard CRUD Privileges
    private static final List<String> STANDARD_PRIVILEGES = Arrays.asList(
            "VIEW", "ADD", "EDIT", "DELETE", "APPROVE");

    /**
     * Seeds all default data for a new tenant schema.
     * Starts in REQUIRES_NEW to isolate from the outer transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void seedDefaultData(String tenantSchema) {
        log.info("🌱 Starting default data seeding (Matrix Model) for tenant: {}", tenantSchema);
        long startTime = System.currentTimeMillis();

        seedDefaultRoles(tenantSchema);
        seedDefaultPrivileges(tenantSchema);
        seedMenusAndSubMenus(tenantSchema);
        seedMatrixMappings(tenantSchema);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Default data seeding completed for {} in {}ms", tenantSchema, duration);
    }

    // ──────────────────────────────────────────────────────────────────────
    // 1. Seed Roles
    // ──────────────────────────────────────────────────────────────────────
    private void seedDefaultRoles(String schema) {
        String sql = "INSERT INTO role_master (role_name, role_code, description, is_active, created_date, role_type) "
                + "SELECT ?, ?, ?, true, CURRENT_TIMESTAMP, ? "
                + "WHERE NOT EXISTS (SELECT 1 FROM role_master WHERE role_code = ?)";
        for (RoleDefinition role : DEFAULT_ROLES) {
            try {
                jdbcTemplate.execute("SET LOCAL search_path TO \"" + schema + "\"");
                jdbcTemplate.update(sql, role.name(), role.name(), role.description(), role.type(), role.name());
            } catch (Exception e) {
                log.warn("Role {} might already exist or failed: {}", role.name(), e.getMessage());
            } finally {
                safeResetSchema();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 2. Seed Standard Privileges
    // ──────────────────────────────────────────────────────────────────────
    private void seedDefaultPrivileges(String schema) {
        String sql = "INSERT INTO privilege_master (privilege_name) "
                + "SELECT ? WHERE NOT EXISTS (SELECT 1 FROM privilege_master WHERE privilege_name = ?)";
        for (String privilege : STANDARD_PRIVILEGES) {
            try {
                jdbcTemplate.execute("SET LOCAL search_path TO \"" + schema + "\"");
                jdbcTemplate.update(sql, privilege, privilege);
            } catch (Exception e) {
                log.warn("Privilege {} failed: {}", privilege, e.getMessage());
            } finally {
                safeResetSchema();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 3. Seed Menus & Sub-Menus
    // ──────────────────────────────────────────────────────────────────────
    private void seedMenusAndSubMenus(String schema) {
        // Menu 1: Dashboard
        seedMenu(schema, "Dashboard", "/dashboard", "dashboard", 1);
        seedSubMenu(schema, "Dashboard", "Overview", "/dashboard/overview", "DASHBOARD_OVERVIEW", 1);

        // Menu 2: Academics
        seedMenu(schema, "Academics", "/academics", "book", 2);
        seedSubMenu(schema, "Academics", "Academic Structure", "/academics/structure", "ACADEMIC_STRUCTURE", 1);
        seedSubMenu(schema, "Academics", "Courses", "/academics/courses", "MANAGE_COURSES", 2);
        seedSubMenu(schema, "Academics", "Subjects", "/academics/subjects", "MANAGE_SUBJECTS", 3);
        seedSubMenu(schema, "Academics", "Syllabus", "/academics/syllabus", "MANAGE_SYLLABUS", 4);

        // Menu 3: Students
        seedMenu(schema, "Students", "/students", "school", 3);
        seedSubMenu(schema, "Students", "Admissions", "/students/admissions", "STUDENT_ADMISSIONS", 1);
        seedSubMenu(schema, "Students", "Directory", "/students/directory", "STUDENT_DIRECTORY", 2);
        seedSubMenu(schema, "Students", "Attendance", "/students/attendance", "STUDENT_ATTENDANCE", 3);

        // Menu 4: Staff
        seedMenu(schema, "Staff", "/staff", "people", 4);
        seedSubMenu(schema, "Staff", "Directory", "/staff/directory", "STAFF_DIRECTORY", 1);
        seedSubMenu(schema, "Staff", "Leave Management", "/staff/leave", "STAFF_LEAVE", 2);
        seedSubMenu(schema, "Staff", "Payroll", "/staff/payroll", "STAFF_PAYROLL", 3);

        // Menu 5: Administration
        seedMenu(schema, "Administration", "/admin", "settings", 5);
        seedSubMenu(schema, "Administration", "Roles & Permissions", "/admin/roles", "MANAGE_ROLES", 1);
        seedSubMenu(schema, "Administration", "Menu Configuration", "/admin/menus", "MANAGE_MENUS", 2);
        seedSubMenu(schema, "Administration", "Tenant Settings", "/admin/settings", "TENANT_SETTINGS", 3);
    }

    private void seedMenu(String schema, String name, String url, String icon, int order) {
        String sql = "INSERT INTO menu_master (name, menu_code, url, icon, menu_order, is_active) "
                + "SELECT ?, ?, ?, ?, ?, true "
                + "WHERE NOT EXISTS (SELECT 1 FROM menu_master WHERE menu_code = ?)";
        try {
            jdbcTemplate.execute("SET LOCAL search_path TO \"" + schema + "\"");
            String menuCode = name.toUpperCase().replace(" ", "_");
            jdbcTemplate.update(sql, name, menuCode, url, icon, order, menuCode);
        } catch (Exception e) {
            log.warn("Menu {} failed: {}", name, e.getMessage());
        } finally {
            safeResetSchema();
        }
    }

    private void seedSubMenu(String schema, String parentMenuName, String name, String url, String code, int order) {
        String sql = "INSERT INTO sub_menu_master (menu_id, sub_menu_name, sub_menu_code, sub_menu_url, sub_menu_order, is_active) "
                + "SELECT m.menu_id, ?, ?, ?, ?, true "
                + "FROM menu_master m WHERE m.menu_code = ? "
                + "AND NOT EXISTS (SELECT 1 FROM sub_menu_master WHERE sub_menu_code = ?)";
        try {
            jdbcTemplate.execute("SET LOCAL search_path TO \"" + schema + "\"");
            String parentCode = parentMenuName.toUpperCase().replace(" ", "_");
            jdbcTemplate.update(sql, name, code, url, order, parentCode, code);
        } catch (Exception e) {
            log.warn("SubMenu {} failed: {}", name, e.getMessage());
        } finally {
            safeResetSchema();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 4. Seed Role -> SubMenu -> Privilege Mappings
    // ──────────────────────────────────────────────────────────────────────
    private void seedMatrixMappings(String schema) {
        log.debug("Seeding Role-SubMenu-Privilege mappings...");

        // ALL SubMenus
        List<String> allSubMenus = Arrays.asList(
                "DASHBOARD_OVERVIEW", "ACADEMIC_STRUCTURE", "MANAGE_COURSES", "MANAGE_SUBJECTS", "MANAGE_SYLLABUS",
                "STUDENT_ADMISSIONS", "STUDENT_DIRECTORY", "STUDENT_ATTENDANCE",
                "STAFF_DIRECTORY", "STAFF_LEAVE", "STAFF_PAYROLL",
                "MANAGE_ROLES", "MANAGE_MENUS", "TENANT_SETTINGS");

        List<String> academicSubMenus = Arrays.asList("ACADEMIC_STRUCTURE", "MANAGE_COURSES", "MANAGE_SUBJECTS",
                "MANAGE_SYLLABUS");
        List<String> studentSubMenus = Arrays.asList("STUDENT_ADMISSIONS", "STUDENT_DIRECTORY", "STUDENT_ATTENDANCE");
        List<String> adminSubMenus = Arrays.asList("MANAGE_ROLES", "MANAGE_MENUS", "TENANT_SETTINGS");

        List<String> fullCrud = Arrays.asList("VIEW", "ADD", "EDIT", "DELETE", "APPROVE");
        List<String> viewOnly = Arrays.asList("VIEW");
        List<String> teacherEdit = Arrays.asList("VIEW", "ADD", "EDIT");

        // 1. SUPER_ADMIN & ADMIN: Full CRUD on everything
        for (String sm : allSubMenus) {
            assignMapping(schema, "SUPER_ADMIN", sm, fullCrud);
            assignMapping(schema, "ADMIN", sm, fullCrud);
        }

        // 2. IT_SUPPORT: Full CRUD on Admin, VIEW only on Academics/Students/Staff
        for (String sm : adminSubMenus) {
            assignMapping(schema, "IT_SUPPORT", sm, fullCrud);
        }
        for (String sm : academicSubMenus) {
            assignMapping(schema, "IT_SUPPORT", sm, viewOnly);
        }
        for (String sm : studentSubMenus) {
            assignMapping(schema, "IT_SUPPORT", sm, viewOnly);
        }
        assignMapping(schema, "IT_SUPPORT", "DASHBOARD_OVERVIEW", viewOnly);

        // 3. TEACHER: View Academics, Edit Syllabus & Attendance
        assignMapping(schema, "TEACHER", "DASHBOARD_OVERVIEW", viewOnly);
        assignMapping(schema, "TEACHER", "ACADEMIC_STRUCTURE", viewOnly);
        assignMapping(schema, "TEACHER", "MANAGE_COURSES", viewOnly);
        assignMapping(schema, "TEACHER", "MANAGE_SUBJECTS", viewOnly);
        assignMapping(schema, "TEACHER", "STUDENT_DIRECTORY", viewOnly);
        assignMapping(schema, "TEACHER", "MANAGE_SYLLABUS", teacherEdit); // Teachers can add/edit syllabus
        assignMapping(schema, "TEACHER", "STUDENT_ATTENDANCE", teacherEdit); // Teachers mark attendance
        assignMapping(schema, "TEACHER", "STAFF_LEAVE", Arrays.asList("VIEW", "ADD")); // Teachers can apply for leave

        // 4. STAFF: Office tasks
        assignMapping(schema, "STAFF", "DASHBOARD_OVERVIEW", viewOnly);
        assignMapping(schema, "STAFF", "STUDENT_ADMISSIONS", teacherEdit);
        assignMapping(schema, "STAFF", "STUDENT_DIRECTORY", viewOnly);
        for (String sm : academicSubMenus) {
            assignMapping(schema, "STAFF", sm, viewOnly);
        }

        // 5. STUDENT & PARENT: View personal data (Requires backend filtering to ensure
        // 'own data' only)
        List<String> standardStudentOrParent = Arrays.asList("DASHBOARD_OVERVIEW", "MANAGE_COURSES", "MANAGE_SUBJECTS",
                "MANAGE_SYLLABUS", "STUDENT_ATTENDANCE");
        for (String sm : standardStudentOrParent) {
            assignMapping(schema, "STUDENT", sm, viewOnly);
            assignMapping(schema, "PARENT", sm, viewOnly);
        }
    }

    private void assignMapping(String schema, String roleCode, String subMenuCode, List<String> privileges) {
        String sql = "INSERT INTO role_submenu_privilege_mapping (role_id, sub_menu_id, privilege_id, created_by, created_date, updated_by, updated_date) "
                + "SELECT r.role_id, sm.sub_menu_id, p.privilege_id, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP "
                + "FROM role_master r "
                + "CROSS JOIN sub_menu_master sm "
                + "CROSS JOIN privilege_master p "
                + "WHERE r.role_code = ? AND sm.sub_menu_code = ? AND p.privilege_name = ? "
                + "AND NOT EXISTS ("
                + "  SELECT 1 FROM role_submenu_privilege_mapping map "
                + "  WHERE map.role_id = r.role_id AND map.sub_menu_id = sm.sub_menu_id AND map.privilege_id = p.privilege_id"
                + ")";

        for (String privilege : privileges) {
            try {
                jdbcTemplate.execute("SET LOCAL search_path TO \"" + schema + "\"");
                jdbcTemplate.update(sql, "SYSTEM", "SYSTEM", roleCode, subMenuCode, privilege);
            } catch (Exception e) {
                log.warn("Mapping {} -> {} -> {} failed: {}", roleCode, subMenuCode, privilege, e.getMessage());
            } finally {
                safeResetSchema();
            }
        }
    }

    private void safeResetSchema() {
        try {
            jdbcTemplate.execute("SET LOCAL search_path TO public");
        } catch (Exception ex) {
            log.warn("Could not reset search_path to public: {}", ex.getMessage());
        }
    }

    /** Role definition helper. */
    private record RoleDefinition(String name, String description, String type) {
    }
}
