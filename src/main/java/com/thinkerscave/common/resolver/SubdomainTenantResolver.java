package com.thinkerscave.common.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SubdomainTenantResolver - Extracts tenant identifier from subdomain.
 * 
 * Supports subdomain-based multi-tenancy for production deployments.
 * Examples:
 * - sjcollege.thinkerscave.com -> "sjcollege"
 * - mumbai-school.thinkerscave.com -> "mumbai_school"
 * - localhost -> null (no subdomain)
 * - 192.168.1.1 -> null (IP address)
 * 
 * This enables the best user experience:
 * - Each tenant has their own subdomain
 * - No manual tenant selection needed
 * - Professional appearance
 * - Easy to brand per tenant
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SubdomainTenantResolver {

    private final JdbcTemplate jdbcTemplate;

    private static final String LOCALHOST = "localhost";
    private static final String WWW = "www";

    // Base domain configuration (should be externalized to application.properties)
    private static final String BASE_DOMAIN = "thinkerscave.com";

    // Cache for subdomain -> tenant_id mappings (5 min TTL in production)
    private final Map<String, CachedTenant> subdomainCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Extracts tenant ID from the subdomain of the request hostname.
     * First checks tenant_config table for registered subdomain mappings.
     * 
     * @param request HTTP request
     * @return tenant ID from subdomain, or null if not found
     */
    public String extractTenantFromSubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        log.debug("Extracting tenant from host: {}", host);

        // Skip localhost and IP addresses
        if (isLocalOrIpAddress(host)) {
            log.debug("Host is localhost or IP address, skipping subdomain extraction");
            return null;
        }

        // Extract subdomain
        String subdomain = extractSubdomain(host);

        if (subdomain == null || WWW.equals(subdomain)) {
            log.debug("No valid subdomain found");
            return null;
        }

        // Check cache first
        CachedTenant cached = subdomainCache.get(subdomain);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for subdomain: {} -> {}", subdomain, cached.tenantId);
            return cached.tenantId;
        }

        // Look up in tenant_config table
        String tenantId = lookupTenantBySubdomain(subdomain);

        if (tenantId != null) {
            log.info("Found tenant mapping in tenant_config: {} -> {}", subdomain, tenantId);
            subdomainCache.put(subdomain, new CachedTenant(tenantId));
            return tenantId;
        }

        // Fallback: Convert subdomain to tenant ID format (replace hyphens with
        // underscores)
        tenantId = normalizeSubdomainToTenantId(subdomain);
        log.info("Using normalized subdomain as tenant: {} -> {}", subdomain, tenantId);

        return tenantId;
    }

    /**
     * Looks up tenant_id from tenant_config table by subdomain.
     */
    private String lookupTenantBySubdomain(String subdomain) {
        try {
            String sql = """
                    SELECT tenant_id FROM public.tenant_config
                    WHERE subdomain = ? AND is_active = true
                    """;
            return jdbcTemplate.queryForObject(sql, String.class, subdomain);
        } catch (Exception e) {
            log.debug("No tenant found for subdomain {} in tenant_config: {}", subdomain, e.getMessage());
            return null;
        }
    }

    /**
     * Clears the subdomain cache. Call when tenant_config is updated.
     */
    public void clearCache() {
        subdomainCache.clear();
        log.info("Subdomain cache cleared");
    }

    /**
     * Cached tenant entry with TTL.
     */
    private static class CachedTenant {
        final String tenantId;
        final long timestamp;

        CachedTenant(String tenantId) {
            this.tenantId = tenantId;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    /**
     * Checks if the host is localhost or an IP address.
     */
    private boolean isLocalOrIpAddress(String host) {
        if (host == null || host.isEmpty()) {
            return true;
        }

        // Check for localhost variants
        if (host.equals(LOCALHOST) ||
                host.equals("127.0.0.1") ||
                host.equals("0.0.0.0") ||
                host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.startsWith("172.")) {
            return true;
        }

        // Check if it's an IP address pattern
        return host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    }

    /**
     * Extracts the subdomain from a hostname.
     * 
     * Examples:
     * - sjcollege.thinkerscave.com -> "sjcollege"
     * - api.sjcollege.thinkerscave.com -> "api.sjcollege"
     * - thinkerscave.com -> null
     * - sjcollege.localhost -> "sjcollege"
     */
    private String extractSubdomain(String host) {
        if (host == null || host.isEmpty()) {
            return null;
        }

        String[] parts = host.split("\\.");

        // For *.localhost pattern (local development)
        if (host.endsWith("." + LOCALHOST)) {
            // sjcollege.localhost -> parts[0] = "sjcollege"
            return parts.length >= 2 ? parts[0] : null;
        }

        // For production domains (*.thinkerscave.com)
        if (parts.length >= 3) {
            // sjcollege.thinkerscave.com -> "sjcollege"
            // api.sjcollege.thinkerscave.com -> "api.sjcollege"

            // Get everything except the last two parts (domain.tld)
            StringBuilder subdomain = new StringBuilder();
            for (int i = 0; i < parts.length - 2; i++) {
                if (i > 0) {
                    subdomain.append(".");
                }
                subdomain.append(parts[i]);
            }
            return subdomain.toString();
        }

        // Only 2 parts (domain.tld) - no subdomain
        return null;
    }

    /**
     * Normalizes subdomain to tenant ID format.
     * Converts hyphens to underscores for database schema naming.
     * 
     * Examples:
     * - "sjcollege" -> "sjcollege"
     * - "mumbai-school" -> "mumbai_school"
     * - "test.sub" -> "test_sub"
     */
    private String normalizeSubdomainToTenantId(String subdomain) {
        if (subdomain == null || subdomain.isEmpty()) {
            return null;
        }

        // Replace hyphens and dots with underscores
        return subdomain.toLowerCase()
                .replace("-", "_")
                .replace(".", "_")
                .replaceAll("[^a-z0-9_]", ""); // Remove any invalid characters
    }

    /**
     * Validates if a subdomain is valid for tenant resolution.
     * 
     * @param subdomain subdomain to validate
     * @return true if valid tenant subdomain
     */
    public boolean isValidTenantSubdomain(String subdomain) {
        if (subdomain == null || subdomain.isEmpty()) {
            return false;
        }

        // Exclude common non-tenant subdomains
        String[] excludedSubdomains = { "www", "api", "admin", "portal", "app", "mail", "ftp" };
        for (String excluded : excludedSubdomains) {
            if (subdomain.equalsIgnoreCase(excluded)) {
                return false;
            }
        }

        // Must match tenant naming pattern (alphanumeric, hyphens, underscores)
        return subdomain.matches("^[a-zA-Z0-9_-]+$");
    }
}
