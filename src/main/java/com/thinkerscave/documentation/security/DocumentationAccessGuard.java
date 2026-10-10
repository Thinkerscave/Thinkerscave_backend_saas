package com.thinkerscave.documentation.security;

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

@Component
@RequiredArgsConstructor
public class DocumentationAccessGuard {

    public static final String DOCUMENTATION = "DOCUMENTATION";
    public static final String COLLECTION_MASTER = "DOC_COLLECTION_MASTER";
    public static final String MATERIAL_MASTER = "DOC_MATERIAL_MASTER";
    public static final String COLLECTIONS = "DOC_COLLECTIONS";
    public static final String HANDOVERS = "DOC_HANDOVERS";

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

    public Long organizationId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new AccessDeniedException("Organization context is required");
        }
        return orgId;
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
                || permissionService.hasPermission(user.getId(), orgId, DOCUMENTATION, privilege);
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
