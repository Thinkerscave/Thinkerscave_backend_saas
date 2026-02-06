package com.thinkerscave.common.filter;

import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.usrm.service.impl.JwtServiceImpl;
import com.thinkerscave.common.usrm.service.impl.UserUserInfoDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * JwtAuthFilter - Validates JWT tokens and sets Spring Security context.
 * 
 * SECURITY: This filter validates that the tenant in the JWT matches
 * the tenant in the X-Tenant-ID header to prevent cross-tenant attacks.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtServiceImpl jwtService;

    @Autowired
    private UserUserInfoDetailsService userInfoDetailsService;

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtService.extractUsername(token);

            // SECURITY: Validate tenant consistency between token and header
            String tokenTenant = jwtService.extractTenantId(token);
            String headerTenant = request.getHeader(TENANT_HEADER);

            // If token has tenant claim, it MUST match the header
            if (tokenTenant != null && !tokenTenant.isBlank()) {
                if (!Objects.equals(tokenTenant, headerTenant)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter()
                            .write("{\"error\": \"Tenant mismatch: token tenant does not match request tenant\"}");
                    return;
                }
                // Use tenant from token (more trusted than header)
                TenantContext.setTenant(tokenTenant);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userInfoDetailsService.loadUserByUsername(username);

            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
