package com.thinkerscave.frontdesk.security;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Menu-permission checks for Front Desk APIs. A receptionist is role type STAFF,
 * so {@code @PreAuthorize("STAFF")} is not a boundary.
 */
@Component
@RequiredArgsConstructor
public class FrontDeskAccessGuard {

    public static final String FRONT_DESK = "FRONT_DESK";
    public static final String VISITORS = "FRONT_DESK_VISITORS";
    public static final String APPOINTMENTS = "FRONT_DESK_APPOINTMENTS";
    public static final String POSTAL = "FRONT_DESK_POSTAL";
    public static final String CALLS = "FRONT_DESK_CALLS";
    public static final String GRIEVANCES = "FRONT_DESK_GRIEVANCES";
    public static final String CERTIFICATES = "FRONT_DESK_CERTIFICATES";
    public static final String GATE_PASS = "FRONT_DESK_GATE_PASS";

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public void requireView(String resource) {
        require(resource, "VIEW");
    }

    public void requireManage(String resource) {
        require(resource, "MANAGE");
    }

    public Long organizationId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new AccessDeniedException("Organization context is required");
        }
        return orgId;
    }

    public String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private void require(String resource, String privilege) {
        if (isSuperAdmin()) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long orgId = OrganizationContext.getOrganizationId();
        if (auth == null || auth.getName() == null || orgId == null) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
        boolean allowed = permissionService.hasPermission(user.getId(), orgId, resource, privilege)
                || permissionService.hasPermission(user.getId(), orgId, FRONT_DESK, privilege);
        if (!allowed) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("SUPER_ADMIN"::equals);
    }
}
