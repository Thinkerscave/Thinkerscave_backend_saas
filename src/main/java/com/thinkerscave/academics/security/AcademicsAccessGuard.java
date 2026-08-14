package com.thinkerscave.academics.security;

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
 * Privilege checks for Academics APIs (resource + VIEW/MANAGE/APPROVE).
 * Falls back to elevated org roles when menu permissions are not yet provisioned.
 */
@Component
@RequiredArgsConstructor
public class AcademicsAccessGuard {

    public static final String RESOURCE_ACADEMIC_YEAR = "ACADEMICS_ACADEMIC_YEAR";
    public static final String RESOURCE_CLASSES = "ACADEMICS_CLASSES";
    public static final String RESOURCE_SUBJECTS = "ACADEMICS_SUBJECTS";
    public static final String RESOURCE_TEACHER_ALLOCATION = "ACADEMICS_TEACHER_ALLOCATION";
    public static final String RESOURCE_TIMETABLE = "ACADEMICS_TIMETABLE";

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

    public Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .map(User::getId)
                .orElse(null);
    }

    private void require(String resource, String privilege) {
        if (hasElevatedRole()) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        Long orgId = OrganizationContext.getOrganizationId();
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || orgId == null) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
        if (!permissionService.hasPermission(user.getId(), orgId, resource, privilege)) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
    }

    private boolean hasElevatedRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "SUPER_ADMIN".equals(a)
                        || "ORGANIZATION_OWNER".equals(a)
                        || "ORGANIZATION_ADMIN".equals(a));
    }
}
