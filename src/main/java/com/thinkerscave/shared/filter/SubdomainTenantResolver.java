package com.thinkerscave.shared.filter;

import com.thinkerscave.shared.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Extracts the tenant slug from the request subdomain.
 *
 * Examples:
 *   sjcollege.thinkerscave.com  → "sjcollege"
 *   localhost / 127.0.0.1       → null (no subdomain)
 */
@Component
@Slf4j
public class SubdomainTenantResolver {

    private static final String BASE_DOMAIN = "thinkerscave.com";
    private static final String LOCALHOST = "localhost";
    private static final String WWW = "www";

    /**
     * Returns the tenant slug extracted from the subdomain, or {@code null}
     * if the request is from localhost / an IP address / the bare base domain.
     */
    public String extractTenantFromSubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null || host.isBlank()) return null;

        // Skip localhost and raw IP addresses
        if (LOCALHOST.equalsIgnoreCase(host) || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return null;
        }

        // Must end with base domain
        if (!host.endsWith("." + BASE_DOMAIN) && !host.equals(BASE_DOMAIN)) {
            return null;
        }

        if (host.equals(BASE_DOMAIN) || host.equals("www." + BASE_DOMAIN)) {
            return null;
        }

        // Extract subdomain (first label before base domain)
        String subdomain = host.substring(0, host.length() - BASE_DOMAIN.length() - 1);

        // Remove nested sub-subdomains — take only last segment before base domain
        int dotIdx = subdomain.lastIndexOf('.');
        String slug = dotIdx >= 0 ? subdomain.substring(dotIdx + 1) : subdomain;

        if (WWW.equalsIgnoreCase(slug) || slug.isBlank()) {
            return null;
        }

        // Normalise: replace hyphens with underscores (URL safe → DB safe)
        return slug.toLowerCase().replace('-', '_');
    }
}
