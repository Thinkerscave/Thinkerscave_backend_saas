package com.thinkerscave.finance.reports.security;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FinanceReportsAccessGuard {
    public static final String RESOURCE = "FINANCE_REPORTS";

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public void requireView() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long organizationId = OrganizationContext.getOrganizationId();
        if (authentication == null || authentication.getName() == null || organizationId == null) {
            throw new AccessDeniedException(RESOURCE + ":VIEW required");
        }
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !permissionService.hasPermission(user.getId(), organizationId, RESOURCE, "VIEW")) {
            throw new AccessDeniedException(RESOURCE + ":VIEW required");
        }
    }
}
