package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.dto.request.ChangePasswordRequest;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.LoginHistoryRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.security.repository.UserSessionRepository;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicSchemaPasswordChangeTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private TenantRegistryRepository tenantRegistryRepository;
    @Mock private UserSessionRepository sessionRepository;
    @Mock private LoginHistoryRepository loginHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private PublicSchemaUserLookupService service;

    @BeforeEach
    void setUp() {
        service = new PublicSchemaUserLookupService(
                userRepository, organizationRepository, tenantRegistryRepository,
            sessionRepository, loginHistoryRepository, passwordEncoder);
    }

    @Test
    void changesPasswordForPublicOwnerOfSelectedOrganization() {
        User owner = owner(26L, 268L);
        when(tenantRegistryRepository.findBySchemaName("tenant_demo"))
            .thenReturn(Optional.of(tenant("tenant_demo", 268L)));
        when(userRepository.findByUsername(owner.getUsername())).thenReturn(Optional.of(owner));
        when(passwordEncoder.matches("temporary", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.changeInstitutionUserPassword(owner.getUsername(), "tenant_demo",
                new ChangePasswordRequest("temporary", "new-password", "new-password"));

        assertEquals("new-hash", owner.getPassword());
        assertFalse(owner.getFirstTimeLogin());
        assertNotNull(owner.getPasswordChangedAt());
        verify(userRepository).save(owner);
    }

    @Test
    void refusesPublicAccountThatDoesNotBelongToSelectedOrganization() {
        User owner = owner(26L, 268L);
        when(tenantRegistryRepository.findBySchemaName("tenant_other"))
            .thenReturn(Optional.of(tenant("tenant_other", 269L)));
        when(userRepository.findByUsername(owner.getUsername())).thenReturn(Optional.of(owner));

        assertThrows(ResourceNotFoundException.class, () -> service.changeInstitutionUserPassword(
            owner.getUsername(), "tenant_other",
                new ChangePasswordRequest("temporary", "new-password", "new-password")));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    private User owner(Long id, Long organizationId) {
        User user = new User();
        user.setId(id);
        user.setUsername("owner@example.test");
        user.setOrganizationId(organizationId);
        user.setPassword("stored-hash");
        user.setFirstTimeLogin(true);
        user.setUserRoles(new HashSet<>());
        return user;
    }

    private TenantRegistry tenant(String schemaName, Long organizationId) {
        Organization organization = new Organization();
        organization.setId(organizationId);
        TenantRegistry tenant = new TenantRegistry();
        tenant.setSchemaName(schemaName);
        tenant.setActive(true);
        tenant.setOrganization(organization);
        return tenant;
    }
}