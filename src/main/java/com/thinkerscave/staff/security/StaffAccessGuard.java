package com.thinkerscave.staff.security;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Staff module privileges, checked against the STAFF_DIRECTORY menu permission.
 * VIEW reads staff; MANAGE or APPROVE create, edit, activate and manage documents, photos and bank details.
 */
@Component
@RequiredArgsConstructor
public class StaffAccessGuard {

    public static final String RESOURCE_DIRECTORY = "STAFF_DIRECTORY";

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public void requireView() {
        require("VIEW");
    }

    public void requireWrite() {
        if (!hasPermission("MANAGE") && !hasPermission("APPROVE")) {
            throw new AccessDeniedException(RESOURCE_DIRECTORY + ": MANAGE or APPROVE required");
        }
    }

    private void require(String privilege) {
        if (!hasPermission(privilege)) {
            throw new AccessDeniedException(RESOURCE_DIRECTORY + ":" + privilege + " required");
        }
    }

    private boolean hasPermission(String privilege) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return false;
        }
        if (auth.getAuthorities().stream().anyMatch(a -> SUPER_ADMIN.equals(a.getAuthority()))) {
            return true;
        }
        Long orgId = OrganizationContext.getOrganizationId();
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || orgId == null) {
            return false;
        }
        return permissionService.hasPermission(user.getId(), orgId, RESOURCE_DIRECTORY, privilege);
    }
}
