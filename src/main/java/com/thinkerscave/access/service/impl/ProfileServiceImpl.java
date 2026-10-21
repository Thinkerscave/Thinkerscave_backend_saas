package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.ChangePasswordRequest;
import com.thinkerscave.access.dto.request.UpdateUserRequest;
import com.thinkerscave.access.dto.response.UserSummaryResponse;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.ProfileService;
import com.thinkerscave.access.service.UserManagementService;
import com.thinkerscave.security.service.impl.PublicSchemaUserLookupService;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UserManagementService userManagementService;
    private final PublicSchemaUserLookupService publicSchemaUserLookupService;

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser() {
        User user = resolveCurrentUser();
        return userManagementService.getUserById(user.getOrganizationId(), user.getId());
    }

    @Override
    @Transactional
    public UserSummaryResponse updateCurrentUser(UpdateUserRequest request) {
        User user = resolveCurrentUser();
        return userManagementService.updateUser(user.getOrganizationId(), user.getId(), request);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(ChangePasswordRequest request) {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        User tenantUser = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElse(null);
        if (tenantUser != null) {
            userManagementService.changePassword(tenantUser.getOrganizationId(), tenantUser.getId(), request);
            return;
        }

        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            publicSchemaUserLookupService.changeInstitutionUserPassword(principal, previousTenant, request);
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenant(previousTenant);
            }
        }
    }

    private User resolveCurrentUser() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new ResourceNotFoundException("Signed-in user not found"));
    }
}
