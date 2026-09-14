package com.thinkerscave.finance.security;

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
 * Privilege checks for Finance APIs. No elevated-role bypass —
 * Organization Admin receives access only via the platform permission matrix.
 */
@Component
@RequiredArgsConstructor
public class FinanceAccessGuard {

    public static final String RESOURCE_MANAGEMENT = "FEES_MANAGEMENT";
    public static final String RESOURCE_HEADS = "FEES_HEADS";
    public static final String RESOURCE_STRUCTURES = "FEES_STRUCTURES";
    public static final String RESOURCE_RECEIPTS = "FEES_RECEIPTS";
    public static final String RESOURCE_OUTSTANDING = "FEES_OUTSTANDING";
    public static final String RESOURCE_COLLECTION = "FEES_COLLECTION";
    public static final String RESOURCE_STUDENT_DETAILS = "FEES_STUDENT_DETAILS";
    public static final String RESOURCE_SETTINGS = "FEES_SETTINGS";

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public void requireView(String resource) {
        require(resource, "VIEW");
    }

    public void requireManage(String resource) {
        require(resource, "MANAGE");
    }

    public boolean canManage(String resource) {
        return hasPermission(resource, "MANAGE");
    }

    public boolean canView(String resource) {
        return hasPermission(resource, "VIEW");
    }

    public Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
    }

    public String currentUsernameOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }

    private boolean hasPermission(String resource, String privilege) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return false;
        }
        Long orgId = OrganizationContext.getOrganizationId();
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || orgId == null) {
            return false;
        }
        return permissionService.hasPermission(user.getId(), orgId, resource, privilege);
    }

    private void require(String resource, String privilege) {
        if (!hasPermission(resource, privilege)) {
            throw new AccessDeniedException(resource + ":" + privilege + " required");
        }
    }
}
