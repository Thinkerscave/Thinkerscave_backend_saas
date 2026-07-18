package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.security.dto.response.WorkspaceOrganizationResponse;
import com.thinkerscave.security.service.WorkspaceAccessService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceAccessServiceImpl implements WorkspaceAccessService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceOrganizationResponse> getOwnedOrganizations() {
        User currentUser = currentUser();
        List<Organization> organizations = organizationRepository.findActiveByOwnerUserId(currentUser.getId());
        return organizations.stream()
                .sorted(Comparator.comparing(Organization::getOrganizationName, String.CASE_INSENSITIVE_ORDER))
                .map(org -> WorkspaceOrganizationResponse.builder()
                        .organizationId(org.getId())
                        .organizationCode(org.getOrganizationCode())
                        .organizationName(org.getOrganizationName())
                        .tenantId(org.getTenantRegistry() != null ? org.getTenantRegistry().getTenantIdentifier() : null)
                        .domain(org.getTenantRegistry() != null ? org.getTenantRegistry().getTenantDomain() : null)
                        .logoUrl(org.getLogoUrl())
                        .current(org.getId().equals(currentUser.getOrganizationId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceOrganizationResponse switchWorkspace(Long organizationId) {
        User currentUser = currentUser();
        if (!organizationRepository.existsActiveOwnedOrganization(currentUser.getId(), organizationId)) {
            throw new BadRequestException("You do not have access to the selected organization");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));
        String tenantId = org.getTenantRegistry() != null ? org.getTenantRegistry().getTenantIdentifier() : null;
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("Selected organization is not linked to an active tenant");
        }

        return WorkspaceOrganizationResponse.builder()
            .organizationId(org.getId())
            .organizationCode(org.getOrganizationCode())
            .organizationName(org.getOrganizationName())
            .tenantId(tenantId)
            .domain(org.getTenantRegistry() != null ? org.getTenantRegistry().getTenantDomain() : null)
            .logoUrl(org.getLogoUrl())
            .current(true)
            .build();
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Unauthenticated workspace access");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
