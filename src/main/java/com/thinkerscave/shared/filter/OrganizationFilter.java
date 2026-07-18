package com.thinkerscave.shared.filter;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.security.service.JwtService;
import com.thinkerscave.shared.context.OrganizationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * OrganizationFilter — Order 3.
 *
 * Runs AFTER {@link TenantFilter} (1) and Spring Security JWT filter (2).
 * Resolves the active organization ID for the authenticated user and stores
 * it in {@link OrganizationContext} for downstream service use.
 *
 * <p>Resolution strategy:
 * <ol>
 *   <li>{@code X-Organization-ID} header — allowed for SUPER_ADMIN overrides.</li>
 *   <li>User's own {@code organizationId} from the {@code access.users} table.</li>
 * </ol>
 */
@Component
@Order(3)
@Slf4j
@RequiredArgsConstructor
public class OrganizationFilter extends OncePerRequestFilter {

    private static final String ORG_HEADER = "X-Organization-ID";
    private static final Long DEV_DEFAULT_ORG_ID = 1L;

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.multi-tenancy.enabled:true}")
    private boolean multiTenancyEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!multiTenancyEnabled) {
                OrganizationContext.setOrganizationId(DEV_DEFAULT_ORG_ID);
                filterChain.doFilter(request, response);
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = auth.getName();

            // Header override for super-admin
            String orgHeader = request.getHeader(ORG_HEADER);
            if (orgHeader != null && !orgHeader.isBlank()) {
                boolean isSuperAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("SUPER_ADMIN") ||
                                       a.getAuthority().equals("ROLE_SUPER_ADMIN"));
                boolean isOrgOwner = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ORGANIZATION_OWNER") ||
                                a.getAuthority().equals("ROLE_ORGANIZATION_OWNER"));
                if (isSuperAdmin || isOrgOwner) {
                    try {
                        Long orgId = Long.parseLong(orgHeader.trim());
                        if (!isSuperAdmin && isOrgOwner && !isOrgAllowedByToken(request, orgId)) {
                            log.warn("Owner org override denied: requestedOrg={} user={}", orgId, username);
                            filterChain.doFilter(request, response);
                            return;
                        }
                        OrganizationContext.setOrganizationId(orgId);
                        log.debug("Org override accepted: org={} user={} superAdmin={}", orgId, username, isSuperAdmin);
                        filterChain.doFilter(request, response);
                        return;
                    } catch (NumberFormatException e) {
                        log.warn("Invalid X-Organization-ID header value: {}", orgHeader);
                    }
                }
            }

            // Resolve from the user's record
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                Long orgId = userOpt.get().getOrganizationId();
                OrganizationContext.setOrganizationId(orgId);
                log.trace("Org context set: {} for user {}", orgId, username);
            } else {
                log.warn("Authenticated user '{}' not found in access.users — no org context set", username);
            }

            filterChain.doFilter(request, response);
        } finally {
            OrganizationContext.clear();
        }
    }

    private boolean isOrgAllowedByToken(HttpServletRequest request, Long organizationId) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            Object raw = jwtService.extractAllClaims(token).get("switchableOrgIds");
            if (!(raw instanceof Collection<?> values)) {
                return false;
            }
            Set<Long> allowed = new HashSet<>();
            for (Object value : values) {
                if (value == null) {
                    continue;
                }
                try {
                    allowed.add(Long.parseLong(value.toString()));
                } catch (NumberFormatException ignored) {
                    // ignore malformed claim values
                }
            }
            return allowed.contains(organizationId);
        } catch (Exception ex) {
            return false;
        }
    }
}
