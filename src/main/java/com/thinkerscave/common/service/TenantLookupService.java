package com.thinkerscave.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for looking up tenant information based on user identifier (email or
 * username).
 * This enables automatic tenant detection during login without requiring manual
 * tenant input.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantLookupService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Finds the tenant ID associated with a given email or username.
     * 
     * @param identifier Either email address or username
     * @return tenant ID (database schema name) or "public" if not found
     */
    public String findTenantByEmailOrUsername(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            log.warn("Empty identifier provided for tenant lookup");
            return "public";
        }

        String sql = """
                SELECT tenant_id
                FROM public.user_tenant_mapping
                WHERE (LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?))
                AND is_active = true
                LIMIT 1
                """;

        try {
            String tenantId = jdbcTemplate.queryForObject(sql, String.class, identifier, identifier);
            log.info("Found tenant '{}' for identifier '{}'", tenantId, identifier);
            return tenantId;
        } catch (EmptyResultDataAccessException e) {
            log.warn("No tenant found for identifier: '{}'. Using default tenant 'public'", identifier);
            return "public";
        } catch (Exception e) {
            log.error("Error looking up tenant for identifier '{}': {}", identifier, e.getMessage(), e);
            return "public";
        }
    }

    /**
     * Checks if a user exists in the tenant mapping.
     * 
     * @param identifier Either email address or username
     * @return true if user exists in mapping, false otherwise
     */
    public boolean userExistsInMapping(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) > 0
                FROM public.user_tenant_mapping
                WHERE (LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?))
                AND is_active = true
                """;

        try {
            Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, identifier, identifier);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking user existence for '{}': {}", identifier, e.getMessage());
            return false;
        }
    }

    /**
     * Updates or creates a mapping for a user.
     * This is used when users are created or updated in tenant schemas.
     * 
     * @param email    User's email
     * @param username User's username
     * @param tenantId Tenant/schema ID
     */
    public void upsertUserTenantMapping(String email, String username, String tenantId) {
        String sql = """
                INSERT INTO public.user_tenant_mapping (email, username, tenant_id)
                VALUES (?, ?, ?)
                ON CONFLICT (email) DO UPDATE
                SET username = EXCLUDED.username,
                    tenant_id = EXCLUDED.tenant_id,
                    updated_at = CURRENT_TIMESTAMP
                """;

        try {
            jdbcTemplate.update(sql, email, username, tenantId);
            log.info("Updated user-tenant mapping: email={}, username={}, tenant={}", email, username, tenantId);
        } catch (Exception e) {
            log.error("Failed to upsert user-tenant mapping for email '{}': {}", email, e.getMessage(), e);
        }
    }
}
