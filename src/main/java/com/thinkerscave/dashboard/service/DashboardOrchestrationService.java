package com.thinkerscave.dashboard.service;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.repository.UserRoleRepository;
import com.thinkerscave.dashboard.dto.response.DashboardResponse;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.enums.DashboardType;
import com.thinkerscave.dashboard.service.provider.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the caller's {@link DashboardType} from their active
 * {@link RoleType}s (fixed precedence, first match wins) and delegates to
 * the matching widget provider. Never lets a provider failure bubble up as
 * an HTTP error — falls back to the {@code DEFAULT} dashboard instead so
 * the frontend never renders a blank screen.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardOrchestrationService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    private final SuperAdminDashboardProvider superAdminDashboardProvider;
    private final OrgOwnerDashboardProvider orgOwnerDashboardProvider;
    private final OrgAdminDashboardProvider orgAdminDashboardProvider;
    private final StaffDashboardProvider staffDashboardProvider;
    private final StudentDashboardProvider studentDashboardProvider;
    private final ParentDashboardProvider parentDashboardProvider;
    private final DefaultDashboardProvider defaultDashboardProvider;

    public DashboardResponse getWorkspace() {
        User user = currentUser();
        DashboardType type = resolveDashboardType(user);

        List<WidgetDTO<?>> widgets;
        try {
            widgets = providerFor(type).getWidgets(user);
        } catch (Exception e) {
            log.error("Dashboard provider for type {} failed, falling back to DEFAULT: {}", type, e.getMessage(), e);
            type = DashboardType.DEFAULT;
            widgets = defaultDashboardProvider.getWidgets(user);
        }

        return DashboardResponse.builder()
                .dashboardType(type)
                .generatedAt(Instant.now())
                .widgets(widgets)
                .build();
    }

    private DashboardWidgetProvider providerFor(DashboardType type) {
        return switch (type) {
            case SUPER_ADMIN -> superAdminDashboardProvider;
            case ORG_OWNER -> orgOwnerDashboardProvider;
            case ORG_ADMIN -> orgAdminDashboardProvider;
            case STAFF -> staffDashboardProvider;
            case STUDENT -> studentDashboardProvider;
            case PARENT -> parentDashboardProvider;
            case DEFAULT -> defaultDashboardProvider;
        };
    }

    private DashboardType resolveDashboardType(User user) {
        if (user == null) {
            return DashboardType.DEFAULT;
        }

        Set<RoleType> roleTypes;
        try {
            roleTypes = userRoleRepository.findActiveRolesWithDetails(user.getId()).stream()
                    .map(UserRole::getRole)
                    .filter(r -> r != null && r.getRoleType() != null)
                    .map(r -> r.getRoleType())
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleType.class)));
        } catch (Exception e) {
            log.warn("Failed to resolve roles for user {}: {}", user.getId(), e.getMessage());
            return DashboardType.DEFAULT;
        }

        if (roleTypes.contains(RoleType.SUPER_ADMIN)) return DashboardType.SUPER_ADMIN;
        if (roleTypes.contains(RoleType.ORGANIZATION_OWNER)) return DashboardType.ORG_OWNER;
        if (roleTypes.contains(RoleType.ORGANIZATION_ADMIN)) return DashboardType.ORG_ADMIN;
        if (roleTypes.contains(RoleType.STAFF)) return DashboardType.STAFF;
        if (roleTypes.contains(RoleType.PARENT)) return DashboardType.PARENT;
        if (roleTypes.contains(RoleType.STUDENT)) return DashboardType.STUDENT;
        return DashboardType.DEFAULT;
    }

    private User currentUser() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
