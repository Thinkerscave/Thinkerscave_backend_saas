package com.thinkerscave.common.orgm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.orgm.dto.TenantOnboardingRequest;
import com.thinkerscave.common.orgm.dto.TenantOnboardingResponse;
import com.thinkerscave.common.orgm.dto.TenantStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive tenant onboarding service.
 * Handles complete tenant provisioning with validation, security, and audit
 * logging.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantOnboardingService {

    private final SchemaInitializer schemaInitializer;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final DefaultDataSeeder defaultDataSeeder;
    private final OrganizationService organizationService;

    private static final List<String> RESERVED_TENANT_NAMES = Arrays.asList(
            "public", "admin", "system", "api", "www", "app", "dashboard",
            "pg_catalog", "information_schema", "pg_toast");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Complete tenant onboarding workflow.
     * 
     * Steps:
     * 1. Validate request
     * 2. Create schema
     * 3. Seed admin user
     * 4. Seed default data (roles, privileges, menus)
     * 5. Configure tenant settings
     * 6. Enable subdomain (if requested)
     * 7. Audit log
     */
    @Transactional
    public TenantOnboardingResponse onboardNewTenant(TenantOnboardingRequest request) {
        String tenantId = sanitizeTenantId(request.getTenantName());
        long startTime = System.currentTimeMillis();
        boolean schemaCreated = false;

        log.info("🚀 Starting onboarding for tenant: {}", tenantId);

        try {
            // Step 1: Validate request
            validateTenantRequest(request);
            log.debug("✅ Validation passed for tenant: {}", tenantId);

            // Step 2: Create schema
            boolean created = schemaInitializer.createSchemaIfNotExists(tenantId);
            if (!created) {
                throw new TenantAlreadyExistsException("Tenant '" + tenantId + "' already exists");
            }
            schemaCreated = true;
            log.info("✅ Schema created: {}", tenantId);

            // Step 3: Seed admin user & IT Support user (includes user_tenant_mapping sync)
            String hashedPassword = passwordEncoder.encode(request.getAdminPassword());
            schemaInitializer.seedTenantUser(tenantId, request.getAdminEmail(), hashedPassword, "ADMIN",
                    request.getAdminFirstName(), request.getAdminLastName());
            log.info("✅ Admin user created: {}", request.getAdminEmail());

            String supportEmail = "support@" + (request.getEnableSubdomain() && request.getSubdomainPrefix() != null
                    ? request.getSubdomainPrefix()
                    : tenantId) + ".thinkerscave.com";
            String supportRandomPassword = passwordEncoder.encode("Support@123"); // In production, this should be a
                                                                                  // secure random password sent via
                                                                                  // email
            schemaInitializer.seedTenantUser(tenantId, supportEmail, supportRandomPassword, "IT_SUPPORT", "IT",
                    "Support");
            log.info("✅ IT Support auto-provisioned: {}", supportEmail);

            // Step 4: Seed default data (roles, privileges, menus)
            defaultDataSeeder.seedDefaultData(tenantId);
            log.info("✅ Default data seeded for tenant");

            // Step 5: Configure tenant in tenant_config
            configureTenantSettings(tenantId, request);
            log.info("✅ Tenant configuration saved");

            // Step 6: Create public Organization record for directory listing
            com.thinkerscave.common.orgm.dto.OrgRequestDTO orgRequest = com.thinkerscave.common.orgm.dto.OrgRequestDTO
                    .builder()
                    .orgName(request.getDisplayName())
                    .brandName(request.getDisplayName())
                    .city(request.getCity())
                    .state(request.getState())
                    .establishDate(request.getEstablishDate())
                    .orgType(request.getOrganizationType())
                    .ownerName(request.getAdminFirstName() + " " + request.getAdminLastName())
                    .ownerEmail(request.getAdminEmail())
                    .ownerMobile(request.getAdminMobile())
                    .isAGroup(false)
                    .subscriptionType(request.getSubscriptionType() != null ? request.getSubscriptionType() : "Free")
                    .tenantSchema(tenantId) // Link org to its tenant schema
                    .build();

            // Step 6: Create public Organization record for directory listing
            String previousTenantId = TenantContext.getTenant();
            try {
                TenantContext.setTenant("public");
                organizationService.saveOrganization(orgRequest);
                log.info("✅ Organization record created in public schema");

                // Step 7: Audit log
                long duration = System.currentTimeMillis() - startTime;
                auditTenantCreation(tenantId, request, "SUCCESS", null, duration);
                log.info("🎉 Successfully onboarded tenant: {} in {}ms", tenantId, duration);

            } finally {
                if (previousTenantId != null) {
                    TenantContext.setTenant(previousTenantId);
                } else {
                    TenantContext.clear();
                }
            }

            return buildSuccessResponse(tenantId, request);

        } catch (TenantAlreadyExistsException e) {
            long duration = System.currentTimeMillis() - startTime;
            auditTenantCreation(tenantId, request, "FAILED", e.getMessage(), duration);
            log.error("❌ Tenant already exists: {}", tenantId);
            throw e;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            auditTenantCreation(tenantId, request, "FAILED", e.getMessage(), duration);

            // Compensating cleanup: drop the schema if it was created
            if (schemaCreated) {
                try {
                    log.warn("🧹 Rolling back: dropping orphaned schema {}", tenantId);
                    schemaInitializer.dropSchema(tenantId);
                    log.info("🧹 Orphaned schema {} dropped successfully", tenantId);
                } catch (Exception cleanupEx) {
                    log.error("🧹 Failed to clean up orphaned schema {}: {}", tenantId, cleanupEx.getMessage());
                }
            }

            log.error("❌ Failed to onboard tenant: {}", tenantId, e);
            throw new TenantOnboardingException("Failed to onboard tenant: " + e.getMessage(), e);
        }
    }

    /**
     * Validates tenant onboarding request.
     */
    private void validateTenantRequest(TenantOnboardingRequest request) {
        // Validate tenant name format
        if (!request.getTenantName().matches("^[a-z0-9_]{3,50}$")) {
            throw new InvalidTenantNameException(
                    "Tenant name must be 3-50 characters, lowercase, alphanumeric and underscores only");
        }

        // Check reserved names
        String tenantLower = request.getTenantName().toLowerCase();
        if (RESERVED_TENANT_NAMES.contains(tenantLower)) {
            throw new ReservedTenantNameException(
                    "Tenant name '" + request.getTenantName() + "' is reserved and cannot be used");
        }

        // Validate admin email
        if (!EMAIL_PATTERN.matcher(request.getAdminEmail()).matches()) {
            throw new InvalidEmailException("Invalid email format: " + request.getAdminEmail());
        }

        // Validate password strength
        if (request.getAdminPassword().length() < 8) {
            throw new WeakPasswordException("Password must be at least 8 characters");
        }

        // Validate subdomain prefix if provided
        if (request.getEnableSubdomain() && request.getSubdomainPrefix() != null) {
            if (!request.getSubdomainPrefix().matches("^[a-z0-9-]{3,50}$")) {
                throw new InvalidSubdomainException(
                        "Subdomain must be 3-50 characters, lowercase, alphanumeric and hyphens only");
            }
        }
    }

    /**
     * Configures tenant settings in tenant_config table.
     * Non-fatal: if the table does not exist yet, logs a warning and continues.
     */
    private void configureTenantSettings(String tenantId, TenantOnboardingRequest request) {
        String sql = """
                INSERT INTO public.tenant_config
                (tenant_id, tenant_name, subdomain, is_active, max_users, storage_limit_mb,
                 features, created_by, created_at, updated_at)
                VALUES (?, ?, ?, true, ?, ?, ?::jsonb, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;

        try {
            String subdomain = request.getEnableSubdomain()
                    ? (request.getSubdomainPrefix() != null ? request.getSubdomainPrefix() : tenantId)
                    : null;

            String featuresJson = objectMapper.writeValueAsString(
                    request.getCustomSettings() != null ? request.getCustomSettings() : new HashMap<>());

            jdbcTemplate.update(sql,
                    tenantId,
                    request.getDisplayName(),
                    subdomain,
                    request.getMaxUsers() != null ? request.getMaxUsers() : 100,
                    request.getStorageLimitMb() != null ? request.getStorageLimitMb() : 10240,
                    featuresJson,
                    request.getPerformedBy() != null ? request.getPerformedBy() : "SYSTEM");

        } catch (Exception e) {
            // Non-fatal: tenant_config is supplementary metadata.
            // If the table doesn't exist yet, log and continue — do not fail the
            // onboarding.
            log.warn("⚠️ Could not save tenant config for {} (table may not exist yet): {}", tenantId, e.getMessage());
        }
    }

    /**
     * Records tenant creation in audit log.
     */
    private void auditTenantCreation(String tenantId, TenantOnboardingRequest request,
            String status, String errorMessage, long durationMs) {
        String sql = """
                INSERT INTO public.tenant_audit_log
                (tenant_id, action, performed_by, details, status, error_message, ip_address, duration_ms)
                VALUES (?, 'TENANT_CREATED', ?, ?::jsonb, ?, ?, ?, ?)
                """;

        try {
            Map<String, Object> details = new HashMap<>();
            details.put("tenantName", request.getTenantName());
            details.put("displayName", request.getDisplayName());
            details.put("adminEmail", request.getAdminEmail());
            details.put("subdomainEnabled", request.getEnableSubdomain());
            details.put("subdomain", request.getSubdomainPrefix());
            details.put("organizationType", request.getOrganizationType());

            String detailsJson = objectMapper.writeValueAsString(details);

            jdbcTemplate.update(sql,
                    tenantId,
                    request.getPerformedBy() != null ? request.getPerformedBy() : "SYSTEM",
                    detailsJson,
                    status,
                    errorMessage,
                    request.getIpAddress(),
                    durationMs);

        } catch (Exception e) {
            log.error("Failed to create audit log entry for tenant: {}", tenantId, e);
            // Don't throw - audit failure shouldn't block onboarding
        }
    }

    /**
     * Builds success response DTO.
     */
    private TenantOnboardingResponse buildSuccessResponse(String tenantId, TenantOnboardingRequest request) {
        String subdomainUrl = null;
        if (request.getEnableSubdomain()) {
            String subdomain = request.getSubdomainPrefix() != null ? request.getSubdomainPrefix() : tenantId;
            subdomainUrl = "https://" + subdomain + ".thinkerscave.com";
        }

        return TenantOnboardingResponse.builder()
                .tenantId(tenantId)
                .tenantName(request.getTenantName())
                .displayName(request.getDisplayName())
                .adminUsername(request.getAdminEmail())
                .adminEmail(request.getAdminEmail())
                .subdomainUrl(subdomainUrl)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .message("Tenant onboarded successfully")
                .maxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 100)
                .storageLimitMb(request.getStorageLimitMb() != null ? request.getStorageLimitMb() : 10240)
                .build();
    }

    /**
     * Gets current status of a tenant.
     */
    public TenantStatusResponse getTenantStatus(String tenantId) {
        String sql = """
                SELECT
                    tc.tenant_id,
                    tc.tenant_name,
                    tc.is_active,
                    tc.subdomain,
                    tc.max_users,
                    tc.storage_limit_mb,
                    tc.features,
                    tc.created_at,
                    (SELECT COUNT(*) FROM public.user_tenant_mapping WHERE tenant_id = tc.tenant_id AND is_active = true) as user_count
                FROM public.tenant_config tc
                WHERE tc.tenant_id = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> TenantStatusResponse.builder()
                    .tenantId(rs.getString("tenant_id"))
                    .tenantName(rs.getString("tenant_name"))
                    .isActive(rs.getBoolean("is_active"))
                    .subdomain(rs.getString("subdomain"))
                    .currentUserCount(rs.getInt("user_count"))
                    .maxUsers(rs.getInt("max_users"))
                    .storageLimitMb(rs.getInt("storage_limit_mb"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .healthStatus(determineHealthStatus(rs.getInt("user_count"), rs.getInt("max_users")))
                    .build(),
                    tenantId);
        } catch (Exception e) {
            log.error("Failed to get status for tenant: {}", tenantId, e);
            throw new TenantNotFoundException("Tenant not found: " + tenantId);
        }
    }

    /**
     * Activates a tenant.
     */
    public void activateTenant(String tenantId) {
        String sql = "UPDATE public.tenant_config SET is_active = true, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ?";
        int updated = jdbcTemplate.update(sql, tenantId);

        if (updated == 0) {
            throw new TenantNotFoundException("Tenant not found: " + tenantId);
        }

        auditTenantAction(tenantId, "ACTIVATED", "SUCCESS", null);
        log.info("✅ Tenant activated: {}", tenantId);
    }

    /**
     * Deactivates a tenant.
     */
    public void deactivateTenant(String tenantId) {
        String sql = "UPDATE public.tenant_config SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ?";
        int updated = jdbcTemplate.update(sql, tenantId);

        if (updated == 0) {
            throw new TenantNotFoundException("Tenant not found: " + tenantId);
        }

        auditTenantAction(tenantId, "DEACTIVATED", "SUCCESS", null);
        log.info("✅ Tenant deactivated: {}", tenantId);
    }

    /**
     * Records tenant action in audit log.
     */
    private void auditTenantAction(String tenantId, String action, String status, String errorMessage) {
        String sql = """
                INSERT INTO public.tenant_audit_log
                (tenant_id, action, performed_by, status, error_message)
                VALUES (?, ?, 'SYSTEM', ?, ?)
                """;

        try {
            jdbcTemplate.update(sql, tenantId, action, status, errorMessage);
        } catch (Exception e) {
            log.error("Failed to audit action {} for tenant: {}", action, tenantId, e);
        }
    }

    /**
     * Determines health status based on resource usage.
     */
    private String determineHealthStatus(int currentUsers, int maxUsers) {
        double usage = (double) currentUsers / maxUsers;
        if (usage >= 0.9)
            return "CRITICAL";
        if (usage >= 0.75)
            return "WARNING";
        return "HEALTHY";
    }

    /**
     * Sanitizes tenant ID.
     */
    private String sanitizeTenantId(String tenantName) {
        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }
        return tenantName.toLowerCase().replaceAll("[^a-z0-9_]", "");
    }

    // Custom Exceptions
    public static class TenantOnboardingException extends RuntimeException {
        public TenantOnboardingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TenantAlreadyExistsException extends RuntimeException {
        public TenantAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InvalidTenantNameException extends RuntimeException {
        public InvalidTenantNameException(String message) {
            super(message);
        }
    }

    public static class ReservedTenantNameException extends RuntimeException {
        public ReservedTenantNameException(String message) {
            super(message);
        }
    }

    public static class InvalidEmailException extends RuntimeException {
        public InvalidEmailException(String message) {
            super(message);
        }
    }

    public static class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String message) {
            super(message);
        }
    }

    public static class InvalidSubdomainException extends RuntimeException {
        public InvalidSubdomainException(String message) {
            super(message);
        }
    }

    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String message) {
            super(message);
        }
    }
}
