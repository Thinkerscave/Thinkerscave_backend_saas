package com.thinkerscave.common.filter;

import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.orgm.repository.OrganizationUserRepository;
import com.thinkerscave.common.usrm.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * OrganizationFilter — sets OrganizationContext for the current request.
 *
 * IMPORTANT: Runs AFTER JwtAuthFilter so Authentication is available.
 * Uses JPA repositories (not raw JdbcTemplate) so all queries execute
 * inside the current tenant schema (set by TenantFilter + Hibernate).
 *
 * Order: 3 (TenantFilter=1, JwtAuthFilter=2, OrganizationFilter=3)
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class OrganizationFilter extends OncePerRequestFilter {

    private static final String ORG_HEADER = "X-Organization-ID";

    // JPA repositories — these queries run in the current TENANT schema
    // automatically
    private final UserRepository userRepository;
    private final OrganizationUserRepository orgUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // Only process for authenticated requests
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = auth.getName();
            String orgHeader = request.getHeader(ORG_HEADER);

            if (orgHeader != null && !orgHeader.trim().isEmpty()) {
                // Client explicitly specified an org — validate membership then set context
                try {
                    Long orgId = Long.parseLong(orgHeader.trim());
                    boolean isSuperAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("SUPER_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

                    if (isSuperAdmin) {
                        OrganizationContext.setOrganizationId(orgId);
                        log.debug("Organization context set: orgId={} via SUPER_ADMIN override", orgId);
                    } else if (!validateAndSetOrgContext(username, orgId)) {
                        log.warn("User '{}' attempted to access org {} without permission", username, orgId);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                                "Access denied to organization: " + orgId);
                        return;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-Organization-ID header value: '{}'", orgHeader);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid organization ID format");
                    return;
                }
            } else {
                // No header — auto-detect if user belongs to exactly one org
                autoDetectSingleOrg(username);
            }

            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: always clear to prevent context leaking across thread pool requests
            OrganizationContext.clear();
        }
    }

    /**
     * Validates that the given user belongs to the org, then sets context.
     * All DB queries run inside the current tenant schema via Hibernate.
     *
     * @return true if access granted and context set; false if access denied
     */
    private boolean validateAndSetOrgContext(String username, Long orgId) {
        try {
            return userRepository.findByUserName(username).map(user -> {
                boolean belongs = orgUserRepository.userBelongsToOrganization(orgId, user.getId());
                if (belongs) {
                    OrganizationContext.setOrganizationId(orgId);
                    log.debug("Organization context set: orgId={} for user='{}'", orgId, username);
                }
                return belongs;
            }).orElse(false);
        } catch (Exception e) {
            log.error("Error validating org membership for user '{}', orgId={}: {}", username, orgId, e.getMessage());
            return false;
        }
    }

    /**
     * Auto-detects org context when no X-Organization-ID header is present.
     * Sets context only if the user belongs to exactly one active org.
     */
    private void autoDetectSingleOrg(String username) {
        try {
            userRepository.findByUserName(username).ifPresent(user -> {
                List<com.thinkerscave.common.orgm.domain.OrganizationUser> orgs = orgUserRepository
                        .findByUserIdAndIsActive(user.getId(), true);

                if (orgs.size() == 1) {
                    Long autoOrgId = orgs.get(0).getOrganizationId();
                    OrganizationContext.setOrganizationId(autoOrgId);
                    log.debug("Auto-detected org context: orgId={} for user='{}'", autoOrgId, username);
                } else if (orgs.isEmpty()) {
                    log.debug("User '{}' has no active org memberships — context not set", username);
                } else {
                    log.debug("User '{}' belongs to {} orgs — awaiting explicit X-Organization-ID header", username,
                            orgs.size());
                }
            });
        } catch (Exception e) {
            log.error("Error during org auto-detection for user '{}': {}", username, e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Mirror exactly what SecurityConfig.permitAll() allows — no need to run for
        // these
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/users/login") ||
                path.startsWith("/api/v1/users/register") ||
                path.startsWith("/api/v1/users/refreshToken") ||
                path.startsWith("/api/v1/users/logout") ||
                path.startsWith("/api/v1/users/generateKey") ||
                path.startsWith("/api/password/") ||
                path.startsWith("/api/v1/public/") || // public inquiry + config endpoints
                path.startsWith("/api/v1/admissions/") || // admissions form submission (public-facing)
                path.startsWith("/api/schema/init") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/actuator/");
    }
}
