package com.thinkerscave.common.orgm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.common.orgm.dto.TenantOnboardingRequest;
import com.thinkerscave.common.orgm.dto.TenantOnboardingResponse;
import com.thinkerscave.common.orgm.dto.TenantStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantOnboardingServiceTest {

    @Mock
    private SchemaInitializer schemaInitializer;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DefaultDataSeeder defaultDataSeeder;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private TenantOnboardingService tenantOnboardingService;

    private TenantOnboardingRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = TenantOnboardingRequest.builder()
                .tenantName("test_tenant")
                .displayName("Test Tenant")
                .adminEmail("admin@test.com")
                .adminPassword("password123")
                .organizationType("SCHOOL")
                .enableSubdomain(true)
                .subdomainPrefix("test-school")
                .build();
    }

    @Test
    void onboardNewTenant_Success() throws Exception {
        // Mocks
        when(schemaInitializer.createSchemaIfNotExists(anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1); // For
                                                                                                               // config
        // Audit log update mock is tricky as it has different params.
        // We can just verify calls or use flexible matching for jdbcTemplate.update

        TenantOnboardingResponse response = tenantOnboardingService.onboardNewTenant(validRequest);

        assertNotNull(response);
        assertEquals("test_tenant", response.getTenantId());
        assertEquals("ACTIVE", response.getStatus());

        verify(schemaInitializer, times(1)).seedTenantUser(
                eq("test_tenant"), eq("admin@test.com"), eq("hashedPassword"),
                eq("ADMIN"), any(), any());
        verify(schemaInitializer, times(1)).seedTenantUser(
                eq("test_tenant"), eq("support@test-school.thinkerscave.com"), eq("hashedPassword"),
                eq("IT_SUPPORT"), any(), any());
        verify(defaultDataSeeder, times(1)).seedDefaultData("test_tenant");
    }

    @Test
    void onboardNewTenant_InvalidName_ThrowsException() {
        validRequest.setTenantName("Invalid Name");

        TenantOnboardingService.TenantOnboardingException exception = assertThrows(
                TenantOnboardingService.TenantOnboardingException.class, () -> {
                    tenantOnboardingService.onboardNewTenant(validRequest);
                });

        assertTrue(exception.getCause() instanceof TenantOnboardingService.InvalidTenantNameException);
    }

    @Test
    void onboardNewTenant_ReservedName_ThrowsException() {
        validRequest.setTenantName("admin");

        TenantOnboardingService.TenantOnboardingException exception = assertThrows(
                TenantOnboardingService.TenantOnboardingException.class, () -> {
                    tenantOnboardingService.onboardNewTenant(validRequest);
                });

        assertTrue(exception.getCause() instanceof TenantOnboardingService.ReservedTenantNameException);
    }

    @Test
    void onboardNewTenant_WeakPassword_ThrowsException() {
        validRequest.setAdminPassword("123");

        TenantOnboardingService.TenantOnboardingException exception = assertThrows(
                TenantOnboardingService.TenantOnboardingException.class, () -> {
                    tenantOnboardingService.onboardNewTenant(validRequest);
                });

        assertTrue(exception.getCause() instanceof TenantOnboardingService.WeakPasswordException);
    }

    @Test
    void activateTenant_Success() {
        when(jdbcTemplate.update(anyString(), eq("test_tenant"))).thenReturn(1);

        assertDoesNotThrow(() -> tenantOnboardingService.activateTenant("test_tenant"));

        verify(jdbcTemplate, atLeastOnce()).update(anyString(), eq("test_tenant"));
    }

    @Test
    void activateTenant_NotFound_ThrowsException() {
        when(jdbcTemplate.update(anyString(), eq("unknown_tenant"))).thenReturn(0);

        assertThrows(TenantOnboardingService.TenantNotFoundException.class, () -> {
            tenantOnboardingService.activateTenant("unknown_tenant");
        });
    }
}
