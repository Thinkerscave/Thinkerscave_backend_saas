package com.thinkerscave.shared.filter;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
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
import java.util.Optional;

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
                if (isSuperAdmin) {
                    try {
                        Long orgId = Long.parseLong(orgHeader.trim());
                        OrganizationContext.setOrganizationId(orgId);
                        log.debug("SUPER_ADMIN org override: {}", orgId);
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
}
