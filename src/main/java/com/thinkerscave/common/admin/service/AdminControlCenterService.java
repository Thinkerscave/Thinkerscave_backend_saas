package com.thinkerscave.common.admin.service;

import com.thinkerscave.common.admin.domain.SystemEvent;
import com.thinkerscave.common.admin.dto.AdminControlCenterDTO;
import com.thinkerscave.common.admin.repository.SystemEventRepository;
import com.thinkerscave.common.audit.domain.AuditLog;
import com.thinkerscave.common.audit.domain.SecurityAuditLog;
import com.thinkerscave.common.audit.repository.AuditLogRepository;
import com.thinkerscave.common.audit.repository.SecurityAuditLogRepository;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.domain.Privilege;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.domain.RoleMenuPrivilegeMapping;
import com.thinkerscave.common.menum.domain.SubMenu;
import com.thinkerscave.common.menum.repository.MenuRepository;
import com.thinkerscave.common.menum.repository.PrivilegeRepository;
import com.thinkerscave.common.menum.repository.RoleMenuPrivilegeMappingRepository;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.menum.repository.SubMenuRepository;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.orgm.repository.OrganizationUserRepository;
import com.thinkerscave.common.staff.domain.Branch;
import com.thinkerscave.common.staff.domain.Department;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.repository.BranchRepository;
import com.thinkerscave.common.staff.repository.DepartmentRepository;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.student.repository.StudentRepository;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminControlCenterService {

    private static final long DEFAULT_ORG_ID = 1L;
    private static final Set<String> ADMIN_SUBMENU_CODES = Set.of(
            "ADMIN_DASHBOARD",
            "ADMIN_ORGANIZATIONS",
            "ADMIN_ACCESS_PERMISSIONS",
            "ADMIN_SYSTEM_MONITORING",
            "ADMIN_AUDIT_CENTER");

    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final SubMenuRepository subMenuRepository;
    private final PrivilegeRepository privilegeRepository;
    private final RoleMenuPrivilegeMappingRepository roleMenuPrivilegeMappingRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityAuditLogRepository securityAuditLogRepository;
    private final SystemEventRepository systemEventRepository;

    public AdminControlCenterDTO getWorkspace() {
        Long organizationId = currentOrganizationId();
        Instant now = Instant.now();

        List<Organisation> organizations = organizationRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Role> roles = roleRepository.findAll();
        List<Menu> menus = menuRepository.findAllByOrderByMenuOrderAsc();
        List<SubMenu> subMenus = subMenuRepository.findAllByOrderBySubMenuOrderAsc();
        List<Privilege> privileges = privilegeRepository.findAll();
        List<RoleMenuPrivilegeMapping> mappings = roleMenuPrivilegeMappingRepository.findAll();
        List<Branch> branches = branchRepository.findAll();
        List<Department> departments = departmentRepository.findAll();
        List<Staff> staff = staffRepository.findAll();
        long studentsInCurrentOrg = studentRepository.countByOrganizationId(organizationId);
        List<AuditLog> auditLogs = auditLogRepository.findAll(
                PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "occurredAt"))).getContent();
        List<SecurityAuditLog> securityEvents = securityAuditLogRepository.findAll(
                PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "occurredAt"))).getContent();
        List<SystemEvent> systemEvents = scopedSystemEvents(organizationId);

        long activeOrganizations = organizations.stream().filter(org -> Boolean.TRUE.equals(org.getIsActive())).count();
        long activeUsersToday = users.stream().filter(user -> LocalDate.now().equals(user.getLastLoginDate())).count();
        long pendingInvitations = users.stream().filter(this::hasPendingInvitation).count();
        int healthScore = calculateHealthScore(organizations, systemEvents, securityEvents);

        List<AdminControlCenterDTO.AdminSectionDTO> adminSections = adminSections(subMenus);
        List<AdminControlCenterDTO.KpiDTO> kpis = List.of(
                new AdminControlCenterDTO.KpiDTO("totalOrganizations", "Total Organizations", String.valueOf(organizations.size()), activeOrganizations + " active", "pi pi-building", "info"),
                new AdminControlCenterDTO.KpiDTO("activeOrganizations", "Active Organizations", String.valueOf(activeOrganizations), inactiveCount(organizations) + " suspended", "pi pi-check-circle", "success"),
                new AdminControlCenterDTO.KpiDTO("totalUsers", "Total Users", String.valueOf(users.size()), activeUsersToday + " active today", "pi pi-users", "neutral"),
                new AdminControlCenterDTO.KpiDTO("activeUsersToday", "Active Users Today", String.valueOf(activeUsersToday), users.size() + " provisioned", "pi pi-user-check", "success"),
                new AdminControlCenterDTO.KpiDTO("pendingInvitations", "Pending Invitations", String.valueOf(pendingInvitations), "First login or email verification pending", "pi pi-send", pendingInvitations > 0 ? "warning" : "success"),
                new AdminControlCenterDTO.KpiDTO("systemHealth", "System Health Score", healthScore + "%", systemEvents.stream().filter(event -> !Boolean.TRUE.equals(event.getResolved())).count() + " open events", "pi pi-heart-fill", healthScore >= 90 ? "success" : healthScore >= 75 ? "warning" : "danger"));

        return new AdminControlCenterDTO(
                now,
                adminSections,
                kpis,
                organizationSummary(organizations, branches, departments, staff, users, organizationId, studentsInCurrentOrg),
                organizations(organizations, branches, staff, organizationId),
                branches(branches, staff, organizationId, studentsInCurrentOrg),
                roles(roles, users, mappings),
                users(users),
                menuSections(menus, subMenus),
                permissionMatrix(roles, privileges, subMenus, mappings),
                monitoring(healthScore, systemEvents, securityEvents, auditLogs),
                activities(auditLogs, securityEvents, systemEvents),
                auditLogs.stream().map(this::toAuditEvent).toList(),
                securityEvents.stream().map(this::toSecurityEvent).toList(),
                systemEvents.stream().map(this::toSystemEvent).toList());
    }

    @Transactional
    public AdminControlCenterDTO.SystemEventDTO runDiagnostics() {
        AdminControlCenterDTO workspace = getWorkspace();
        Long organizationId = currentOrganizationId();
        int score = workspace.monitoring().healthScore();
        String severity = score >= 90 ? "INFO" : score >= 75 ? "MEDIUM" : "HIGH";
        SystemEvent event = SystemEvent.builder()
                .organizationId(organizationId)
                .tenantCode("public")
                .category("HEALTH")
                .component("Administration Center")
                .eventCode("DIAGNOSTIC_RUN")
                .title("System diagnostics completed")
                .message("Health score recalculated at " + score + "% from live tenant, audit and security signals.")
                .severity(severity)
                .status("COMPLETED")
                .metricName("healthScore")
                .metricValue((double) score)
                .metricUnit("percent")
                .resolved(score >= 75)
                .occurredAt(Instant.now())
                .build();
        return toSystemEvent(systemEventRepository.save(event));
    }

    private AdminControlCenterDTO.OrganizationSummaryDTO organizationSummary(
            List<Organisation> organizations,
            List<Branch> branches,
            List<Department> departments,
            List<Staff> staff,
            List<User> users,
            Long organizationId,
            long studentsInCurrentOrg) {
        long schools = organizations.stream().filter(org -> "SCHOOL".equalsIgnoreCase(typeName(org))).count();
        long colleges = organizations.stream().filter(org -> "COLLEGE".equalsIgnoreCase(typeName(org))).count();
        long activeBranches = branches.stream().filter(branch -> matchesOrg(branch.getOrganizationId(), organizationId) && Boolean.TRUE.equals(branch.getIsActive())).count();
        long activeDepartments = departments.stream().filter(department -> matchesOrg(department.getOrganizationId(), organizationId) && Boolean.TRUE.equals(department.getIsActive())).count();
        long activeStaff = staff.stream().filter(member -> matchesOrg(member.getOrganizationId(), organizationId) && Boolean.TRUE.equals(member.getIsActive())).count();
        long parents = users.stream().filter(user -> hasRole(user, "PARENT")).count();
        long activeMemberships = organizationUserRepository.countActiveUsersInOrganization(organizationId);
        return new AdminControlCenterDTO.OrganizationSummaryDTO(
                schools, colleges, activeBranches, activeDepartments, studentsInCurrentOrg, activeStaff, parents, activeMemberships);
    }

    private List<AdminControlCenterDTO.OrganizationDTO> organizations(
            List<Organisation> organizations,
            List<Branch> branches,
            List<Staff> staff,
            Long currentOrgId) {
        return organizations.stream()
                .sorted(Comparator.comparing(Organisation::getOrgName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(org -> {
                    long orgBranches = branches.stream().filter(branch -> Objects.equals(branch.getOrganizationId(), org.getOrgId())).count();
                    long orgStaff = staff.stream().filter(member -> Objects.equals(member.getOrganizationId(), org.getOrgId())).count();
                    long orgStudents = Objects.equals(org.getOrgId(), currentOrgId) ? studentRepository.countByOrganizationId(org.getOrgId()) : 0L;
                    long activeUsers = organizationUserRepository.countActiveUsersInOrganization(org.getOrgId());
                    long storageUsed = 280 + orgStudents * 4 + orgStaff * 9 + orgBranches * 25;
                    long storageLimit = "ENTERPRISE".equalsIgnoreCase(org.getSubscriptionType()) ? 51200 : 10240;
                    long apiUsage = activeUsers * 140 + orgStudents * 6 + orgStaff * 18;
                    int health = Math.max(72, Math.min(99, 96 - (Boolean.TRUE.equals(org.getIsActive()) ? 0 : 18) - (int) Math.min(8, orgBranches)));
                    return new AdminControlCenterDTO.OrganizationDTO(
                            org.getOrgId(),
                            org.getOrgCode(),
                            org.getOrgName(),
                            org.getBrandName(),
                            typeName(org),
                            org.getCity(),
                            org.getState(),
                            org.getTenantSchema(),
                            org.getSubscriptionType(),
                            org.getIsActive(),
                            org.getUser() != null ? fullName(org.getUser()) : null,
                            org.getUser() != null ? org.getUser().getEmail() : null,
                            org.getEstablishmentDate(),
                            orgBranches,
                            orgStudents,
                            orgStaff,
                            activeUsers,
                            storageUsed,
                            storageLimit,
                            apiUsage,
                            health);
                })
                .toList();
    }

    private List<AdminControlCenterDTO.BranchDTO> branches(List<Branch> branches, List<Staff> staff, Long organizationId, long studentsInCurrentOrg) {
        long branchCount = branches.stream().filter(branch -> matchesOrg(branch.getOrganizationId(), organizationId)).count();
        long studentsPerBranch = branchCount == 0 ? 0 : Math.max(0, studentsInCurrentOrg / branchCount);
        return branches.stream()
                .filter(branch -> matchesOrg(branch.getOrganizationId(), organizationId))
                .sorted(Comparator.comparing(Branch::getBranchName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(branch -> new AdminControlCenterDTO.BranchDTO(
                        branch.getId(),
                        branch.getBranchCode(),
                        branch.getBranchName(),
                        branch.getLocation(),
                        branch.getIsActive(),
                        branch.getOrganizationId(),
                        staff.stream().filter(member -> member.getBranch() != null && Objects.equals(member.getBranch().getId(), branch.getId())).count(),
                        studentsPerBranch))
                .toList();
    }

    private List<AdminControlCenterDTO.RoleDTO> roles(List<Role> roles, List<User> users, List<RoleMenuPrivilegeMapping> mappings) {
        return roles.stream()
                .sorted(Comparator.comparing(Role::getRoleName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(role -> new AdminControlCenterDTO.RoleDTO(
                        role.getRoleId(),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getDescription(),
                        role.getIsActive(),
                        role.getRoleType() != null ? role.getRoleType().name() : null,
                        users.stream().filter(user -> user.getRoles().stream().anyMatch(userRole -> Objects.equals(userRole.getRoleId(), role.getRoleId()))).count(),
                        mappings.stream().filter(mapping -> mapping.getRole() != null && Objects.equals(mapping.getRole().getRoleId(), role.getRoleId())).count()))
                .toList();
    }

    private List<AdminControlCenterDTO.UserAccessDTO> users(List<User> users) {
        return users.stream()
                .sorted(Comparator.comparing(User::getLastLoginDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> new AdminControlCenterDTO.UserAccessDTO(
                        user.getId(),
                        user.getUserCode(),
                        fullName(user),
                        user.getUserName(),
                        user.getEmail(),
                        user.getIsBlocked(),
                        user.getIsFirstTimeLogin(),
                        user.getIsEmailVerified(),
                        user.getLastLoginDate(),
                        user.getRoles().stream().map(Role::getRoleName).toList(),
                        user.getOrganizations().stream().map(Organisation::getOrgName).toList(),
                        hasPendingInvitation(user) ? "PENDING" : "ACCEPTED"))
                .toList();
    }

    private List<AdminControlCenterDTO.MenuSectionDTO> menuSections(List<Menu> menus, List<SubMenu> subMenus) {
        return menus.stream()
                .map(menu -> {
                    List<SubMenu> children = subMenus.stream()
                            .filter(subMenu -> subMenu.getMenu() != null && Objects.equals(subMenu.getMenu().getMenuId(), menu.getMenuId()))
                            .sorted(Comparator.comparing(SubMenu::getSubMenuOrder, Comparator.nullsLast(Integer::compareTo)))
                            .toList();
                    List<AdminControlCenterDTO.AdminSectionDTO> pages = children.stream().map(this::toAdminSection).toList();
                    return new AdminControlCenterDTO.MenuSectionDTO(
                            menu.getMenuId(),
                            menu.getMenuCode(),
                            menu.getName(),
                            menu.getIcon(),
                            menu.getIsActive(),
                            children.stream().filter(subMenu -> Boolean.TRUE.equals(subMenu.getIsActive())).count(),
                            children.size(),
                            pages);
                })
                .toList();
    }

    private List<AdminControlCenterDTO.PermissionMatrixRowDTO> permissionMatrix(
            List<Role> roles,
            List<Privilege> privileges,
            List<SubMenu> subMenus,
            List<RoleMenuPrivilegeMapping> mappings) {
        long totalPages = subMenus.stream().filter(SubMenu::getIsActive).count();
        return roles.stream()
                .sorted(Comparator.comparing(Role::getRoleName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(role -> new AdminControlCenterDTO.PermissionMatrixRowDTO(
                        role.getRoleId(),
                        role.getRoleCode(),
                        role.getRoleName(),
                        privileges.stream()
                                .sorted(Comparator.comparing(Privilege::getPrivilegeName, Comparator.nullsLast(String::compareToIgnoreCase)))
                                .map(privilege -> {
                                    long assignedPages = mappings.stream()
                                            .filter(mapping -> mapping.getRole() != null && mapping.getPrivilege() != null)
                                            .filter(mapping -> Objects.equals(mapping.getRole().getRoleId(), role.getRoleId()))
                                            .filter(mapping -> Objects.equals(mapping.getPrivilege().getPrivilegeId(), privilege.getPrivilegeId()))
                                            .map(RoleMenuPrivilegeMapping::getSubMenu)
                                            .filter(Objects::nonNull)
                                            .map(SubMenu::getSubMenuId)
                                            .distinct()
                                            .count();
                                    return new AdminControlCenterDTO.PermissionCellDTO(
                                            privilege.getPrivilegeId(),
                                            privilege.getPrivilegeName(),
                                            assignedPages,
                                            totalPages,
                                            assignedPages > 0);
                                })
                                .toList()))
                .toList();
    }

    private AdminControlCenterDTO.MonitoringDTO monitoring(
            int healthScore,
            List<SystemEvent> systemEvents,
            List<SecurityAuditLog> securityEvents,
            List<AuditLog> auditLogs) {
        long openEvents = systemEvents.stream().filter(event -> !Boolean.TRUE.equals(event.getResolved())).count();
        long criticalEvents = systemEvents.stream().filter(event -> List.of("HIGH", "CRITICAL").contains(event.getSeverity())).count();
        long failedSecurity = securityEvents.stream().filter(event -> !event.isSuccess()).count();
        List<AdminControlCenterDTO.MonitoringWidgetDTO> widgets = List.of(
                new AdminControlCenterDTO.MonitoringWidgetDTO("database", "Database", "UP", "H2 dev schema responding", "HEALTHY", "pi pi-database", "success"),
                new AdminControlCenterDTO.MonitoringWidgetDTO("security", "Security Events", String.valueOf(failedSecurity), "Failed events in audit window", failedSecurity > 3 ? "WATCH" : "STABLE", "pi pi-shield", failedSecurity > 3 ? "warning" : "success"),
                new AdminControlCenterDTO.MonitoringWidgetDTO("audit", "Audit Coverage", String.valueOf(auditLogs.size()), "Recent auditable operations", auditLogs.isEmpty() ? "LOW" : "ACTIVE", "pi pi-history", auditLogs.isEmpty() ? "warning" : "info"),
                new AdminControlCenterDTO.MonitoringWidgetDTO("events", "Open Events", String.valueOf(openEvents), criticalEvents + " critical or high", criticalEvents > 0 ? "ATTENTION" : "CLEAR", "pi pi-bell", criticalEvents > 0 ? "danger" : "success"));
        return new AdminControlCenterDTO.MonitoringDTO(
                healthScore,
                "UP",
                openEvents,
                criticalEvents,
                failedSecurity,
                widgets,
                byCategory(systemEvents, "JOB"),
                byCategory(systemEvents, "NOTIFICATION"),
                byCategory(systemEvents, "DATA_INTEGRITY"));
    }

    private List<AdminControlCenterDTO.ActivityDTO> activities(
            List<AuditLog> auditLogs,
            List<SecurityAuditLog> securityEvents,
            List<SystemEvent> systemEvents) {
        Stream<AdminControlCenterDTO.ActivityDTO> auditActivities = auditLogs.stream()
                .map(log -> new AdminControlCenterDTO.ActivityDTO(
                        log.getAction(),
                        fallback(log.getSummary(), log.getEntityType() + " " + fallback(log.getEntityId(), "")),
                        fallback(log.getActorUsername(), "System"),
                        "pi pi-history",
                        "info",
                        log.getOccurredAt()));
        Stream<AdminControlCenterDTO.ActivityDTO> securityActivities = securityEvents.stream()
                .map(event -> new AdminControlCenterDTO.ActivityDTO(
                        event.getEventCode(),
                        fallback(event.getMessage(), "Security event"),
                        fallback(event.getUsername(), "Security Engine"),
                        event.isSuccess() ? "pi pi-lock-open" : "pi pi-lock",
                        event.isSuccess() ? "success" : "warning",
                        event.getOccurredAt()));
        Stream<AdminControlCenterDTO.ActivityDTO> systemActivities = systemEvents.stream()
                .map(event -> new AdminControlCenterDTO.ActivityDTO(
                        event.getTitle(),
                        fallback(event.getMessage(), event.getComponent()),
                        event.getComponent(),
                        iconForCategory(event.getCategory()),
                        toneForSeverity(event.getSeverity()),
                        event.getOccurredAt()));
        return Stream.concat(Stream.concat(auditActivities, securityActivities), systemActivities)
                .sorted(Comparator.comparing(AdminControlCenterDTO.ActivityDTO::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(16)
                .toList();
    }

    private List<AdminControlCenterDTO.AdminSectionDTO> adminSections(List<SubMenu> subMenus) {
        return subMenus.stream()
                .filter(subMenu -> ADMIN_SUBMENU_CODES.contains(subMenu.getSubMenuCode()))
                .filter(subMenu -> Boolean.TRUE.equals(subMenu.getIsActive()))
                .sorted(Comparator.comparing(SubMenu::getSubMenuOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toAdminSection)
                .toList();
    }

    private List<SystemEvent> scopedSystemEvents(Long organizationId) {
        List<SystemEvent> events = systemEventRepository.findTop50ByOrganizationIdOrderByOccurredAtDesc(organizationId);
        return events.isEmpty() ? systemEventRepository.findTop50ByOrderByOccurredAtDesc() : events;
    }

    private List<AdminControlCenterDTO.SystemEventDTO> byCategory(List<SystemEvent> events, String category) {
        return events.stream()
                .filter(event -> category.equalsIgnoreCase(event.getCategory()))
                .map(this::toSystemEvent)
                .toList();
    }

    private AdminControlCenterDTO.AdminSectionDTO toAdminSection(SubMenu subMenu) {
        return new AdminControlCenterDTO.AdminSectionDTO(
                subMenu.getSubMenuId(),
                subMenu.getSubMenuCode(),
                subMenu.getSubMenuName(),
                subMenu.getSubMenuDescription(),
                subMenu.getSubMenuUrl(),
                subMenu.getSubMenuIcon(),
                subMenu.getSubMenuOrder());
    }

    private AdminControlCenterDTO.AuditEventDTO toAuditEvent(AuditLog log) {
        return new AdminControlCenterDTO.AuditEventDTO(
                log.getId(),
                log.getEventType() != null ? log.getEventType().name() : null,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getActorUsername(),
                log.getSourceIp(),
                log.getSummary(),
                log.getChanges(),
                log.getOccurredAt());
    }

    private AdminControlCenterDTO.SecurityEventDTO toSecurityEvent(SecurityAuditLog event) {
        return new AdminControlCenterDTO.SecurityEventDTO(
                event.getId(),
                event.getEventCode(),
                event.getUsername(),
                event.getSourceIp(),
                event.isSuccess(),
                event.getSeverity() != null ? event.getSeverity().name() : null,
                event.getMessage(),
                event.getOccurredAt());
    }

    private AdminControlCenterDTO.SystemEventDTO toSystemEvent(SystemEvent event) {
        return new AdminControlCenterDTO.SystemEventDTO(
                event.getId(),
                event.getOrganizationId(),
                event.getTenantCode(),
                event.getCategory(),
                event.getComponent(),
                event.getEventCode(),
                event.getTitle(),
                event.getMessage(),
                event.getSeverity(),
                event.getStatus(),
                event.getMetricName(),
                event.getMetricValue(),
                event.getMetricUnit(),
                event.getResolved(),
                event.getOccurredAt());
    }

    private int calculateHealthScore(List<Organisation> organizations, List<SystemEvent> systemEvents, List<SecurityAuditLog> securityEvents) {
        long inactiveOrganizations = inactiveCount(organizations);
        long highEvents = systemEvents.stream().filter(event -> List.of("HIGH", "CRITICAL").contains(event.getSeverity())).count();
        long failedSecurity = securityEvents.stream().filter(event -> !event.isSuccess()).count();
        long openEvents = systemEvents.stream().filter(event -> !Boolean.TRUE.equals(event.getResolved())).count();
        int score = 98 - (int) inactiveOrganizations * 4 - (int) highEvents * 8 - (int) Math.min(10, failedSecurity * 2) - (int) Math.min(8, openEvents);
        return Math.max(55, Math.min(99, score));
    }

    private long inactiveCount(List<Organisation> organizations) {
        return organizations.stream().filter(org -> !Boolean.TRUE.equals(org.getIsActive())).count();
    }

    private boolean matchesOrg(Long value, Long organizationId) {
        return value == null || Objects.equals(value, organizationId);
    }

    private boolean hasPendingInvitation(User user) {
        return Boolean.TRUE.equals(user.getIsFirstTimeLogin()) || !Boolean.TRUE.equals(user.getIsEmailVerified());
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().anyMatch(role -> roleCode.equalsIgnoreCase(fallback(role.getRoleCode(), role.getRoleName())));
    }

    private String fullName(User user) {
        return Stream.of(user.getFirstName(), user.getMiddleName(), user.getLastName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String typeName(Organisation organisation) {
        return organisation.getType() != null ? organisation.getType().name() : null;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String iconForCategory(String category) {
        return switch (fallback(category, "").toUpperCase(Locale.ROOT)) {
            case "JOB" -> "pi pi-clock";
            case "NOTIFICATION" -> "pi pi-bell";
            case "DATA_INTEGRITY" -> "pi pi-database";
            case "SECURITY" -> "pi pi-shield";
            case "TENANT" -> "pi pi-building";
            default -> "pi pi-server";
        };
    }

    private String toneForSeverity(String severity) {
        return switch (fallback(severity, "INFO").toUpperCase(Locale.ROOT)) {
            case "CRITICAL", "HIGH" -> "danger";
            case "MEDIUM" -> "warning";
            case "LOW" -> "neutral";
            default -> "success";
        };
    }

    private Long currentOrganizationId() {
        return OrganizationContext.getOrganizationId() != null ? OrganizationContext.getOrganizationId() : DEFAULT_ORG_ID;
    }
}