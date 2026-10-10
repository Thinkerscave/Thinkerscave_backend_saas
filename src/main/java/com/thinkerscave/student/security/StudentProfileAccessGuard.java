package com.thinkerscave.student.security;

import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.MenuRepository;
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
 * Student 360 includes medical notes. Role type STAFF is not enough:
 * the caller must be allowed to open a students menu.
 */
@Component
@RequiredArgsConstructor
public class StudentProfileAccessGuard {

    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;

    public void requireStudentProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Student profile access required");
        }
        boolean elevated = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "SUPER_ADMIN".equals(role) || "ORGANIZATION_ADMIN".equals(role) || "ORGANIZATION_OWNER".equals(role));
        if (elevated) {
            return;
        }
        Long orgId = OrganizationContext.getOrganizationId();
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || orgId == null) {
            throw new AccessDeniedException("Student profile access required");
        }
        boolean allowed = permissionService.getEffectivePermissions(user.getId(), orgId).stream()
                .filter(row -> Boolean.TRUE.equals(row.getCanView()))
                .map(EffectivePermissionResponse::getMenuCode)
                .anyMatch(this::isStudentMenu);
        if (!allowed) {
            throw new AccessDeniedException("Student profile access required");
        }
    }

    private boolean isStudentMenu(String menuCode) {
        if (menuCode == null || menuCode.startsWith("FRONT_DESK")) {
            return false;
        }
        Menu menu = menuRepository.findByMenuCode(menuCode).orElse(null);
        if (menu != null && menu.getRoute() != null && menu.getRoute().startsWith("/app/students")) {
            return true;
        }
        String code = menuCode.toUpperCase();
        return code.contains("STUDENT") && !code.contains("FRONT_DESK");
    }
}
