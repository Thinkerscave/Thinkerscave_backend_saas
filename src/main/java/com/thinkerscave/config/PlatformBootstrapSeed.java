package com.thinkerscave.config;

import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.access.enums.PrivilegeType;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
                log.info("Ensuring platform bootstrap baseline (roles, menus, permissions, and Super Admin access)...");

        Customer customer = ensurePlatformCustomer();
        Organization organization = ensurePlatformOrganization(customer);
        seedPrivileges();
        Role superAdminRole = seedRoles();
        seedMenusAndPermissions(organization, superAdminRole);
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
                "Commercial subscription management", null, "credit_card", MenuType.MODULE, null, 4);
        Menu platform = ensureMenu("PLATFORM_GROUP", "Platform",
                "Platform operations and infrastructure", null, "server", MenuType.MODULE, null, 5);

        Menu dashboard = ensureMenu("PLATFORM_DASHBOARD", "Dashboard", "Platform overview",
                "/app/tenant-management/dashboard", "dashboard", MenuType.PAGE, null, 1);
        Menu customers = ensureMenu("CUSTOMERS", "Customers", "Commercial customer accounts",
                "/app/tenant-management/customers", "groups", MenuType.PAGE, null, 2);
        Menu organizations = ensureMenu("TM_ORGANIZATIONS", "Organizations", "Tenant organizations",
                "/app/tenant-management/organizations", "business", MenuType.PAGE, null, 3);
        Menu audit = ensureMenu("AUDIT_CENTER", "Audit Center", "Platform audit center",
                "/app/tenant-management/audit-center", "history", MenuType.PAGE, null, 6);

        Menu plans = ensureMenu("SUBSCRIPTION_PLANS", "Subscription Plans", "Platform subscription plans",
                "/app/tenant-management/subscription-plans", "credit_card", MenuType.PAGE, subscriptions, 1);
        Menu promotions = ensureMenu("PROMOTIONS", "Promotions", "Platform promotions",
                "/app/tenant-management/promotions", "local_offer", MenuType.PAGE, subscriptions, 2);

        Menu features = ensureMenu("FEATURE_CATALOG", "Feature Catalog", "Platform feature catalogue",
                "/app/tenant-management/feature-catalog", "apps", MenuType.PAGE, platform, 1);
        Menu templates = ensureMenu("PROVISIONING_TEMPLATES", "Provisioning Templates",
                "Provisioning templates and onboarding",
                "/app/tenant-management/organizations/create", "sliders-h", MenuType.PAGE, platform, 2);
        Menu health = ensureMenu("TENANT_HEALTH", "Tenant Health", "Tenant health monitoring",
                "/app/tenant-management/tenant-health", "monitor_heart", MenuType.PAGE, platform, 3);
        Menu migration = ensureMenu("MIGRATION_CENTER", "Migration Center", "Tenant migration jobs",
                "/app/tenant-management/migration-center", "sync", MenuType.PAGE, platform, 4);

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
                            MenuType type, Menu parent, int order) {
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
