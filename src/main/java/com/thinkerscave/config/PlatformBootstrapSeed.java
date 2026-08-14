package com.thinkerscave.config;

import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.access.enums.PrivilegeType;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.Feature;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.SubscriptionPlan;
import com.thinkerscave.platform.entity.SubscriptionPlanFeature;
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.FeatureRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanFeatureRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanRepository;
import com.thinkerscave.shared.entity.CodeSequence;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.repository.CodeSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Idempotent bootstrap for an empty remote PostgreSQL database (test/prod).
 * Creates platform host org, system roles, privileges, Super Admin menus,
 * role_permissions, and superadmin / Password@123 when the DB has no superadmin.
 */
@Component
@Profile({"test", "prod"})
@Order(100)
@Slf4j
@RequiredArgsConstructor
public class PlatformBootstrapSeed implements ApplicationRunner {

    public static final String SUPERADMIN_USERNAME = "superadmin";
    public static final String SUPERADMIN_PASSWORD = "admin@123";

    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final CodeSequenceRepository codeSequenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final FeatureRepository featureRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
                log.info("Ensuring platform bootstrap baseline (roles, menus, permissions, and Super Admin access)...");

        Customer customer = ensurePlatformCustomer();
        Organization organization = ensurePlatformOrganization(customer);
        seedPrivileges();
        Role superAdminRole = seedRoles();
        seedMenusAndPermissions(organization, superAdminRole);
        List<Feature> subscriptionFeatures = seedOrganizationFacingCatalog();
        seedSubscriptionPlanFeatures(subscriptionFeatures);
        User superAdmin = userRepository.findByUsername(SUPERADMIN_USERNAME)
                                .orElseGet(() -> seedSuperAdmin(organization));
        ensureUserRole(superAdmin, superAdminRole);
        seedCodeSequences();

                log.info("Platform bootstrap baseline verified. Login: {} / {}", SUPERADMIN_USERNAME, SUPERADMIN_PASSWORD);
    }

    private Customer ensurePlatformCustomer() {
        return customerRepository.findByCustomerCode("CUS000001").orElseGet(() ->
                customerRepository.save(Customer.builder()
                        .customerCode("CUS000001")
                        .customerName("ThinkersCave Platform")
                        .businessEmail("platform@thinkerscave.com")
                        .mobileNumber("9000000001")
                        .status(CustomerStatus.ACTIVE)
                        .active(true)
                        .notes("Internal platform host account")
                        .build()));
    }

    private Organization ensurePlatformOrganization(Customer customer) {
        return organizationRepository.findByOrganizationCode("ORG000001").orElseGet(() ->
                organizationRepository.save(Organization.builder()
                        .organizationCode("ORG000001")
                        .customer(customer)
                        .organizationName("ThinkersCave Platform")
                        .shortName("TCP")
                        .institutionType(InstitutionType.OTHER)
                        .email("platform@thinkerscave.com")
                        .mobileNumber("9000000001")
                        .city("Bhubaneswar")
                        .state("Odisha")
                        .country("India")
                        .postalCode("751001")
                        .timeZone("Asia/Kolkata")
                        .currency("INR")
                        .language("en-IN")
                        .status(OrganizationStatus.ACTIVE)
                        .active(true)
                        .onboardingCompleted(true)
                        .remarks("Platform host organization for Super Admin")
                        .build()));
    }

    private void seedPrivileges() {
        if (privilegeRepository.count() > 0) {
            return;
        }
        privilegeRepository.save(Privilege.builder()
                .privilegeCode("VIEW").privilegeName("View").description("View / read access")
                .privilegeType(PrivilegeType.VIEW).displayOrder(1).active(true).build());
        privilegeRepository.save(Privilege.builder()
                .privilegeCode("MANAGE").privilegeName("Manage").description("Create / update / delete")
                .privilegeType(PrivilegeType.MANAGE).displayOrder(2).active(true).build());
        privilegeRepository.save(Privilege.builder()
                .privilegeCode("APPROVE").privilegeName("Approve").description("Approval / workflow actions")
                .privilegeType(PrivilegeType.APPROVE).displayOrder(3).active(true).build());
    }

    private Role seedRoles() {
        ensureRole("ROLE_OWNER", "Organization Owner", "Campus owner with full access",
                RoleType.ORGANIZATION_OWNER, "ADMIN", 1);
        ensureRole("ROLE_ADMIN", "Organization Admin", "Campus administrator",
                RoleType.ORGANIZATION_ADMIN, "ADMIN", 2);
        ensureRole("ROLE_STAFF", "Staff", "Teaching / non-teaching staff",
                RoleType.STAFF, "STAFF", 3);
        ensureRole("ROLE_STUDENT", "Student", "Student portal access",
                RoleType.STUDENT, "STUDENT", 4);
        ensureRole("ROLE_PARENT", "Parent", "Parent portal access",
                RoleType.PARENT, "PARENT", 5);
        return ensureRole("ROLE_SUPER_ADMIN", "ThinkersCave Super Admin",
                "Platform control tower and tenant administration",
                RoleType.SUPER_ADMIN, "PLATFORM", 0);
    }

    private Role ensureRole(String code, String name, String description,
                            RoleType type, String dashboard, int order) {
        return roleRepository.findByRoleCode(code).orElseGet(() ->
                roleRepository.save(Role.builder()
                        .roleCode(code)
                        .roleName(name)
                        .description(description)
                        .roleType(type)
                        .dashboardCode(dashboard)
                        .systemRole(true)
                        .active(true)
                        .displayOrder(order)
                        .build()));
    }

    private void seedMenusAndPermissions(Organization organization, Role superAdminRole) {
        Menu subscriptions = ensureMenu("SUBSCRIPTIONS_GROUP", "Subscriptions",
                "Commercial subscription management", null, "credit_card", MenuType.MODULE, null, 4,
                MenuScope.PLATFORM, null);
        Menu platform = ensureMenu("PLATFORM_GROUP", "Platform",
                "Platform operations and infrastructure", null, "server", MenuType.MODULE, null, 5,
                MenuScope.PLATFORM, null);

        Menu dashboard = ensureMenu("PLATFORM_DASHBOARD", "Dashboard", "Platform overview",
                "/app/tenant-management/dashboard", "dashboard", MenuType.PAGE, null, 1,
                MenuScope.PLATFORM, null);
        Menu customers = ensureMenu("CUSTOMERS", "Customers", "Commercial customer accounts",
                "/app/tenant-management/customers", "groups", MenuType.PAGE, null, 2,
                MenuScope.PLATFORM, null);
        Menu organizations = ensureMenu("TM_ORGANIZATIONS", "Organizations", "Tenant organizations",
                "/app/tenant-management/organizations", "business", MenuType.PAGE, null, 3,
                MenuScope.PLATFORM, null);
        Menu audit = ensureMenu("AUDIT_CENTER", "Audit Center", "Platform audit center",
                "/app/tenant-management/audit-center", "history", MenuType.PAGE, null, 6,
                MenuScope.PLATFORM, null);

        Menu plans = ensureMenu("SUBSCRIPTION_PLANS", "Subscription Plans", "Platform subscription plans",
                "/app/tenant-management/subscription-plans", "credit_card", MenuType.PAGE, subscriptions, 1,
                MenuScope.PLATFORM, null);
        Menu promotions = ensureMenu("PROMOTIONS", "Promotions", "Platform promotions",
                "/app/tenant-management/promotions", "local_offer", MenuType.PAGE, subscriptions, 2,
                MenuScope.PLATFORM, null);

        Menu features = ensureMenu("FEATURE_CATALOG", "Feature Catalog", "Platform feature catalogue",
                "/app/tenant-management/feature-catalog", "apps", MenuType.PAGE, platform, 1,
                MenuScope.PLATFORM, null);
        Menu templates = ensureMenu("PROVISIONING_TEMPLATES", "Provisioning Templates",
                "Provisioning templates and onboarding",
                "/app/tenant-management/organizations/create", "sliders-h", MenuType.PAGE, platform, 2,
                MenuScope.PLATFORM, null);
        Menu health = ensureMenu("TENANT_HEALTH", "Tenant Health", "Tenant health monitoring",
                "/app/tenant-management/tenant-health", "monitor_heart", MenuType.PAGE, platform, 3,
                MenuScope.PLATFORM, null);
        Menu migration = ensureMenu("MIGRATION_CENTER", "Migration Center", "Tenant migration jobs",
                "/app/tenant-management/migration-center", "sync", MenuType.PAGE, platform, 4,
                MenuScope.PLATFORM, null);

        grant(organization, superAdminRole, dashboard, true, true, false);
        grant(organization, superAdminRole, customers, true, true, true);
        grant(organization, superAdminRole, organizations, true, true, true);
        grant(organization, superAdminRole, subscriptions, true, true, false);
        grant(organization, superAdminRole, plans, true, true, false);
        grant(organization, superAdminRole, promotions, true, true, false);
        grant(organization, superAdminRole, platform, true, true, false);
        grant(organization, superAdminRole, features, true, true, false);
        grant(organization, superAdminRole, templates, true, true, false);
        grant(organization, superAdminRole, health, true, true, false);
        grant(organization, superAdminRole, migration, true, true, false);
        grant(organization, superAdminRole, audit, true, true, false);
    }

    private Menu ensureMenu(String code, String name, String description, String route, String icon,
                            MenuType type, Menu parent, int order, MenuScope scope, Feature feature) {
        Menu menu = menuRepository.findByMenuCode(code).orElseGet(() ->
                Menu.builder()
                        .menuCode(code)
                        .defaultPage(false)
                        .build());

        menu.setMenuName(name);
        menu.setDescription(description);
        menu.setRoute(route);
        menu.setIcon(icon);
        menu.setMenuType(type);
        menu.setParentMenu(parent);
        menu.setDisplayOrder(order);
        menu.setShowInSidebar(true);
        menu.setActive(true);
        menu.setMenuScope(scope);
        menu.setFeature(feature);

        return menuRepository.save(menu);
    }

    private void grant(Organization org, Role role, Menu menu,
                       boolean view, boolean manage, boolean approve) {
        RolePermission rp = rolePermissionRepository
                .findByRole_IdAndMenu_IdAndOrganization_Id(role.getId(), menu.getId(), org.getId())
                .orElseGet(() -> RolePermission.builder()
                        .organization(org)
                        .role(role)
                        .menu(menu)
                        .build());

        rp.setCanView(view);
        rp.setCanManage(manage);
        rp.setCanApprove(approve);
        rolePermissionRepository.save(rp);
    }

    /**
     * Seeds the full organization-facing menu catalog (CORE + SUBSCRIPTION scope),
     * derived from the actual Angular route tree — see
     * docs/workflows/MENU_HIERARCHY_REFERENCE.md. Unlike Tenant Management menus,
     * these are never granted a role_permission here; they become available to an
     * organization only via provisioning (organization_modules + Owner/Admin
     * role_permissions), keeping Subscription and Permission concerns separate.
     *
     * @return the SUBSCRIPTION-scope features created, for plan-feature linking.
     */
    private List<Feature> seedOrganizationFacingCatalog() {
                deactivateObsoleteOrganizationMenus();

        // ── CORE (always available, no subscription gating) ──────────────────
        ensureMenu("DASHBOARD", "Dashboard", "Organization dashboard", "/app", "dashboard",
                MenuType.PAGE, null, 1, MenuScope.CORE, null);
        Menu orgProfile = ensureMenu("ORG_PROFILE", "Organization Profile", "Organization profile",
                "/app/organization-profile", "business", MenuType.PAGE, null, 2, MenuScope.CORE, null);
        ensureMenu("ORG_ACTIVITY_LOGS", "Activity Logs", "Organization activity logs",
                "/app/organization/activity-logs", "history", MenuType.PAGE, orgProfile, 1, MenuScope.CORE, null);

        Menu access = ensureMenu("ACCESS_MANAGEMENT", "Access & Security", "Roles, permissions and security",
                null, "shield", MenuType.MODULE, null, 3, MenuScope.CORE, null);
        ensureMenu("ACCESS_DASHBOARD", "Access Dashboard", "Access management overview",
                "/app/access-management/dashboard", "dashboard", MenuType.PAGE, access, 1, MenuScope.CORE, null);
        ensureMenu("ACCESS_ROLES", "Roles", "Manage roles",
                "/app/access-management/roles", "badge", MenuType.PAGE, access, 2, MenuScope.CORE, null);
        ensureMenu("ACCESS_MENUS", "Menu Catalog", "Manage menu catalog",
                "/app/access-management/menus", "list", MenuType.PAGE, access, 3, MenuScope.CORE, null);
        ensureMenu("ACCESS_USERS", "Users", "Manage users",
                "/app/access-management/users", "groups", MenuType.PAGE, access, 4, MenuScope.CORE, null);
        ensureMenu("ACCESS_SECURITY_POLICY", "Security Policy", "Security policy configuration",
                "/app/access-management/security-policy", "lock", MenuType.PAGE, access, 5, MenuScope.CORE, null);
        ensureMenu("ACCESS_LOGIN_HISTORY", "Login History", "Login history",
                "/app/access-management/login-history", "history", MenuType.PAGE, access, 6, MenuScope.CORE, null);

        ensureMenu("ONBOARDING_CHECKLIST", "Setup Checklist", "First-time setup checklist",
                "/app/onboarding", "checklist", MenuType.PAGE, null, 4, MenuScope.CORE, null);
        ensureMenu("USER_PROFILE", "My Profile", "My profile", "/app/profile", "person",
                MenuType.PAGE, null, 90, MenuScope.CORE, null);
        ensureMenu("GLOBAL_SETTINGS", "Settings", "Application settings", "/app/settings", "settings",
                MenuType.PAGE, null, 91, MenuScope.CORE, null);

        // ── SUBSCRIPTION (gated by the organization's subscription plan) ─────
        Feature academicsFeature = ensureFeature("FEAT_ACADEMICS", "ACADEMICS_MODULE", "Academics",
                "Academics", "ACADEMIC", 1);
        Menu academics =         ensureMenu("ACADEMICS", "Academics", "Academic setup and scheduling",
                "/app/academics", "school", MenuType.MODULE, null, 10, MenuScope.SUBSCRIPTION, academicsFeature);
        ensureMenu("ACADEMICS_ACADEMIC_YEAR", "Academic Year", "Academic year lifecycle and history",
                "/app/academics/academic-year", "calendar_month", MenuType.PAGE, academics, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ACADEMICS_CLASSES", "Classes & Sections", "Manage classes, sections and class teachers",
                "/app/academics/classes-sections", "class", MenuType.PAGE, academics, 2, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ACADEMICS_SUBJECTS", "Subjects & Mapping", "Configure subjects and class subject mapping",
                "/app/academics/subjects-mapping", "menu_book", MenuType.PAGE, academics, 3, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ACADEMICS_TIMETABLE", "Timetable", "Timetable",
                "/app/academics/timetable", "calendar", MenuType.PAGE, academics, 4, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ACADEMICS_TEACHER_ARRANGEMENT", "Teacher Arrangement", "Teacher arrangement",
                "/app/academics/teacher-arrangement", "person", MenuType.PAGE, academics, 5, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ACADEMICS_CALENDAR", "Academic Calendar", "Academic calendar",
                "/app/academics/academic-calendar", "calendar", MenuType.PAGE, academics, 6, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ACADEMICS_SYLLABUS_TRACKER", "Syllabus Tracker", "Syllabus tracker",
                "/app/academics/syllabus-tracker", "book", MenuType.PAGE, academics, 7, MenuScope.SUBSCRIPTION, null);

        Feature studentsFeature = ensureFeature("FEAT_STUDENTS", "STUDENTS_MODULE", "Students",
                "Students", "CORE", 2);
        Menu students = ensureMenu("STUDENTS", "Students", "Student directory and lifecycle",
                "/app/students", "groups", MenuType.MODULE, null, 11, MenuScope.SUBSCRIPTION, studentsFeature);
        ensureMenu("STUDENTS_DIRECTORY", "Directory", "Student directory",
                "/app/students/directory", "list", MenuType.PAGE, students, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("STUDENTS_ALUMNI", "Alumni", "Student alumni",
                "/app/students/alumni", "school", MenuType.PAGE, students, 2, MenuScope.SUBSCRIPTION, null);

        Feature staffFeature = ensureFeature("FEAT_STAFF", "STAFF_MODULE", "Staff",
                "Staff", "ADMINISTRATION", 3);
        Menu staff = ensureMenu("STAFF", "Staff", "Staff directory and HR",
                "/app/staff", "badge", MenuType.MODULE, null, 12, MenuScope.SUBSCRIPTION, staffFeature);
        ensureMenu("STAFF_DIRECTORY", "Directory", "Staff directory",
                "/app/staff/directory", "list", MenuType.PAGE, staff, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("STAFF_RESPONSIBILITIES", "Responsibilities", "Staff responsibilities",
                "/app/staff/responsibilities", "assignment", MenuType.PAGE, staff, 2, MenuScope.SUBSCRIPTION, null);
        ensureMenu("STAFF_PAYROLL", "Payroll", "Staff payroll",
                "/app/staff/payroll", "credit_card", MenuType.PAGE, staff, 3, MenuScope.SUBSCRIPTION, null);
        ensureMenu("STAFF_LEAVE", "Leave & Availability", "Staff leave and availability",
                "/app/staff/leave-availability", "event", MenuType.PAGE, staff, 4, MenuScope.SUBSCRIPTION, null);

        Feature attendanceFeature = ensureFeature("FEAT_ATTENDANCE", "ATTENDANCE_MODULE", "Attendance",
                "Attendance", "ACADEMIC", 4);
        Menu attendance = ensureMenu("ATTENDANCE", "Attendance", "Student and staff attendance",
                "/app/attendance", "check_circle", MenuType.MODULE, null, 13, MenuScope.SUBSCRIPTION, attendanceFeature);
        ensureMenu("ATTENDANCE_STUDENTS", "Student Attendance", "Student attendance",
                "/app/attendance/students", "groups", MenuType.PAGE, attendance, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ATTENDANCE_STAFF", "Staff Attendance", "Staff attendance",
                "/app/attendance/staff", "badge", MenuType.PAGE, attendance, 2, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ATTENDANCE_REPORTS", "Reports", "Attendance reports",
                "/app/attendance/reports", "bar_chart", MenuType.PAGE, attendance, 3, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ATTENDANCE_CALENDAR", "Calendar", "Attendance calendar",
                "/app/attendance/calendar", "calendar", MenuType.PAGE, attendance, 4, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ATTENDANCE_SETTINGS", "Settings", "Attendance settings",
                "/app/attendance/settings", "settings", MenuType.PAGE, attendance, 5, MenuScope.SUBSCRIPTION, null);

        Feature admissionsFeature = ensureFeature("FEAT_ADMISSIONS", "ADMISSIONS_MODULE", "Admissions",
                "Admissions", "ADMINISTRATION", 5);
        Menu admissions = ensureMenu("ADMISSIONS", "Admissions", "Admissions CRM",
                "/app/admissions", "person_add", MenuType.MODULE, null, 14, MenuScope.SUBSCRIPTION, admissionsFeature);
        ensureMenu("ADMISSIONS_LEADS", "Leads", "Admissions leads",
                "/app/admissions/leads", "list", MenuType.PAGE, admissions, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ADMISSIONS_FOLLOW_UPS", "Follow-ups", "Admissions follow-ups",
                "/app/admissions/follow-ups", "event", MenuType.PAGE, admissions, 2, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ADMISSIONS_APPLICATIONS", "Applications", "Admissions applications",
                "/app/admissions/applications", "description", MenuType.PAGE, admissions, 3, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ADMISSIONS_REPORTS", "Reports", "Admissions reports",
                "/app/admissions/reports", "bar_chart", MenuType.PAGE, admissions, 4, MenuScope.SUBSCRIPTION, null);
        ensureMenu("ADMISSIONS_SETTINGS", "Settings", "Admissions settings",
                "/app/admissions/settings", "settings", MenuType.PAGE, admissions, 5, MenuScope.SUBSCRIPTION, null);

        Feature promotionTransferFeature = ensureFeature("FEAT_PROMOTION_TRANSFER", "PROMOTION_TRANSFER_MODULE",
                "Promotion & Transfer", "Promotion & Transfer", "ACADEMIC", 8);
        Menu promotionTransfer = ensureMenu("PROMOTION_TRANSFER", "Promotion & Transfer", "Student promotion and transfer",
                null, "trending_up", MenuType.MODULE, null, 17, MenuScope.SUBSCRIPTION, promotionTransferFeature);
        ensureMenu("PROMOTIONS_STUDENT", "Promotions", "Student promotions",
                "/app/promotions", "trending_up", MenuType.PAGE, promotionTransfer, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("STUDENT_TRANSFERS", "Transfers", "Student transfers",
                "/app/transfers", "sync", MenuType.PAGE, promotionTransfer, 2, MenuScope.SUBSCRIPTION, null);

        Feature responsibilitiesFeature = ensureFeature("FEAT_RESPONSIBILITIES", "RESPONSIBILITIES_MODULE",
                "Responsibilities", "Responsibilities", "ADMINISTRATION", 9);
        ensureMenu("RESPONSIBILITIES", "Responsibilities", "Staff responsibilities",
                "/app/responsibilities", "assignment", MenuType.PAGE, null, 18, MenuScope.SUBSCRIPTION,
                responsibilitiesFeature);

        Feature communicationFeature = ensureFeature("FEAT_COMMUNICATION", "COMMUNICATION_MODULE",
                "Communication", "Communication", "CORE", 10);
        Menu communication = ensureMenu("COMMUNICATION", "Communication", "Notices, announcements and messaging",
                "/app/communication", "chat", MenuType.MODULE, null, 19, MenuScope.SUBSCRIPTION, communicationFeature);
        ensureMenu("COMMUNICATION_NOTICES", "Notices", "Notices",
                "/app/communication/notices", "notifications", MenuType.PAGE, communication, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("COMMUNICATION_ANNOUNCEMENTS", "Announcements", "Announcements",
                "/app/communication/announcements", "campaign", MenuType.PAGE, communication, 2, MenuScope.SUBSCRIPTION, null);
        ensureMenu("COMMUNICATION_CONVERSATIONS", "Conversations", "Conversations",
                "/app/communication/conversations", "chat", MenuType.PAGE, communication, 3, MenuScope.SUBSCRIPTION, null);
        ensureMenu("COMMUNICATION_TEMPLATES", "Templates", "Message templates",
                "/app/communication/templates", "description", MenuType.PAGE, communication, 4, MenuScope.SUBSCRIPTION, null);
        ensureMenu("COMMUNICATION_DELIVERY_LOGS", "Delivery Logs", "Delivery logs",
                "/app/communication/delivery-logs", "history", MenuType.PAGE, communication, 5, MenuScope.SUBSCRIPTION, null);

        Feature feeManagementFeature = ensureFeature("FEAT_FEE_MANAGEMENT", "FEE_MANAGEMENT_MODULE",
                "Fee Management", "Fee Management", "FINANCE", 11);
        Menu feeManagement = ensureMenu("FEE_MANAGEMENT", "Fee Management", "Fee collection and accounting",
                "/app/fees", "credit_card", MenuType.MODULE, null, 20, MenuScope.SUBSCRIPTION, feeManagementFeature);
        ensureMenu("FEE_DASHBOARD", "Dashboard", "Fee dashboard",
                "/app/fees/dashboard", "dashboard", MenuType.PAGE, feeManagement, 1, MenuScope.SUBSCRIPTION, null);
        ensureMenu("FEE_PAYMENTS", "Payments", "Fee payments",
                "/app/fees/payments", "credit_card", MenuType.PAGE, feeManagement, 2, MenuScope.SUBSCRIPTION, null);
        ensureMenu("FEE_RECEIPTS", "Receipts", "Fee receipts",
                "/app/fees/receipts", "receipt", MenuType.PAGE, feeManagement, 3, MenuScope.SUBSCRIPTION, null);
        ensureMenu("FEE_REPORTS", "Reports", "Fee reports",
                "/app/fees/reports", "bar_chart", MenuType.PAGE, feeManagement, 4, MenuScope.SUBSCRIPTION, null);
        ensureMenu("FEE_MY_FEES", "My Fees", "My fees",
                "/app/fees/my-fees", "credit_card", MenuType.PAGE, feeManagement, 5, MenuScope.SUBSCRIPTION, null);

        return List.of(academicsFeature, studentsFeature, staffFeature, attendanceFeature, admissionsFeature,
                promotionTransferFeature, responsibilitiesFeature,
                communicationFeature, feeManagementFeature);
    }

    private void deactivateObsoleteOrganizationMenus() {
        List<String> obsoleteCodes = List.of(
                "STUDENTS_TRANSFERS",
                "STUDENTS_DOCUMENTS",
                "STAFF_DOCUMENTS",
                "STAFF_ALUMNI",
                "ADMISSIONS_OVERVIEW",
                "ADMISSIONS_ENROLLMENT",
                "EXAMS",
                "ENROLLMENTS",
                "FEE_SETUP",
                "FEE_SETUP_POLICY",
                "FEE_SETUP_HEADS",
                "FEE_SETUP_GROUPS",
                "FEE_SETUP_STRUCTURE",
                "FEE_CONTRACTS",
                "FEE_LEDGER",
                "FEE_ADJUSTMENTS",
                "FEE_CONTROLS",
                "FEE_AUDIT"
        );
        menuRepository.findByMenuCodeInAndActiveTrue(obsoleteCodes)
                .forEach(menu -> {
                    menu.setActive(false);
                    menu.setShowInSidebar(false);
                    menuRepository.save(menu);
                });
    }

    private Feature ensureFeature(String code, String key, String name, String module, String category, int order) {
        return featureRepository.findByFeatureCode(code).orElseGet(() -> featureRepository.save(Feature.builder()
                .featureCode(code)
                .featureKey(key)
                .featureName(name)
                .displayName(name)
                .module(module)
                .category(category)
                .displayOrder(order)
                .visible(true)
                .defaultEnabled(true)
                .active(true)
                .build()));
    }

    /**
     * Every existing subscription plan includes every organization-facing feature by
     * default, until Product defines real tier differentiation via the Feature Catalog UI.
     */
    private void seedSubscriptionPlanFeatures(List<Feature> features) {
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        for (SubscriptionPlan plan : plans) {
            for (Feature feature : features) {
                if (subscriptionPlanFeatureRepository.existsBySubscriptionPlan_IdAndFeature_Id(plan.getId(), feature.getId())) {
                    continue;
                }
                subscriptionPlanFeatureRepository.save(SubscriptionPlanFeature.builder()
                        .subscriptionPlan(plan)
                        .feature(feature)
                        .enabled(true)
                        .mandatory(false)
                        .displayOrder(feature.getDisplayOrder())
                        .active(true)
                        .build());
            }
        }
    }

    private User seedSuperAdmin(Organization organization) {
        return userRepository.save(User.builder()
                .organizationId(organization.getId())
                .userCode("USR000001")
                .username(SUPERADMIN_USERNAME)
                .email("superadmin@thinkerscave.com")
                .mobileNumber("9777000001")
                .password(passwordEncoder.encode(SUPERADMIN_PASSWORD))
                .firstName("Super")
                .lastName("Admin")
                .displayName("Super Admin")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mobileVerified(true)
                .firstTimeLogin(false)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .build());
    }

    private void ensureUserRole(User user, Role role) {
        boolean exists = userRoleRepository.findActiveRolesWithDetails(user.getId()).stream()
                .anyMatch(ur -> ur.getRole().getId().equals(role.getId()));
        if (exists) {
            return;
        }
        userRoleRepository.save(UserRole.builder()
                .user(user)
                .role(role)
                .primaryRole(true)
                .active(true)
                .build());
    }

    private void seedCodeSequences() {
        upsertSequence(CodeType.CUSTOMER, 1);
        upsertSequence(CodeType.ORGANIZATION, 1);
        upsertSequence(CodeType.USER, 1);
        upsertSequence(CodeType.CONTACT, 0);
        upsertSequence(CodeType.PROVISION_JOB, 0);
        upsertSequence(CodeType.TENANT, 0);
        upsertSequence(CodeType.PROMOTION, 0);
        upsertSequence(CodeType.TEMPLATE, 0);
    }

    private void upsertSequence(CodeType type, long value) {
        CodeSequence seq = codeSequenceRepository.findByCodeType(type).orElseGet(CodeSequence::new);
        seq.setCodeType(type);
        if (seq.getLastValue() == null || seq.getLastValue() < value) {
            seq.setLastValue(value);
        }
        codeSequenceRepository.save(seq);
    }
}
