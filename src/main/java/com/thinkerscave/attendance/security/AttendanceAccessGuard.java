package com.thinkerscave.attendance.security;

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

import java.util.Map;

/**
 * Privilege checks for Staff Attendance APIs.
 * Uses menu privileges VIEW / MANAGE / APPROVE and falls back to elevated
 * org roles (SUPER_ADMIN / ORGANIZATION_OWNER / ORGANIZATION_ADMIN) so the
 * attendance module keeps working while menu permissions are provisioned.
 */
@Component
@RequiredArgsConstructor
public class AttendanceAccessGuard {

    public static final String ATTENDANCE = "ATTENDANCE";
    public static final String ATTENDANCE_STAFF = "ATTENDANCE_STAFF";

    private static final Map<String, String> PARENT_RESOURCE = Map.of(
            ATTENDANCE_STAFF, ATTENDANCE
    );

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public void requireView(String resource) {
        require(resource, "VIEW");
    }

    public void requireManage(String resource) {
        require(resource, "MANAGE");
    }

    public void requireApprove(String resource) {
        require(resource, "APPROVE");
    }

    public boolean canView(String resource) {
        return has(resource, "VIEW");
    }

    public boolean canManage(String resource) {
        return has(resource, "MANAGE");
    }

    public boolean canApprove(String resource) {
        return has(resource, "APPROVE");
    }

    public String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }

    public User requireCurrentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByUsername(a.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    public boolean hasElevatedRole() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) {
            return false;
        }
        return a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> "SUPER_ADMIN".equals(r)
                        || "ORGANIZATION_OWNER".equals(r)
                        || "ORGANIZATION_ADMIN".equals(r));
    }

    private boolean has(String resource, String privilege) {
        if (hasElevatedRole()) {
            return true;
        }
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null || OrganizationContext.getOrganizationId() == null) {
            return false;
        }
        User u = userRepository.findByUsername(a.getName()).orElse(null);
        if (u == null) {
            return false;
        }
        Long orgId = OrganizationContext.getOrganizationId();
        if (permissionService.hasPermission(u.getId(), orgId, resource, privilege)) {
            return true;
        }
        String parent = PARENT_RESOURCE.get(resource);
        return parent != null && permissionService.hasPermission(u.getId(), orgId, parent, privilege);
    }

    private void require(String resource, String privilege) {
        if (!has(resource, privilege)) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
    }
}
