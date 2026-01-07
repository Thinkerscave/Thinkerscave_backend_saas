package com.thinkerscave.common.filter;

import com.thinkerscave.common.multitenancy.TenantContext;
import com.thinkerscave.common.security.UserInfoUserDetails;
import com.thinkerscave.common.usrm.service.JwtService;
import com.thinkerscave.common.usrm.service.impl.UserUserInfoDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Filter to handle JWT authentication.
 * Extends OncePerRequestFilter to ensure it is executed once per request.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserUserInfoDetailsService userInfoDetailsService;

    /**
     * This method is called once per request to process JWT authentication.
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain to pass the request and response to the next filter
     * @throws ServletException if an error occurs during request processing
     * @throws IOException if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Extract the "Authorization" header from the HTTP request
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // Check if the "Authorization" header is present and starts with "Bearer"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract the token by removing the "Bearer " prefix
            token = authHeader.substring(7);
            // Extract the username from the token
            username = jwtService.extractUsername(token);
        }

        // If a username is found in the token and no authentication is currently set in the security context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load user details from the username
            UserInfoUserDetails userDetails = userInfoDetailsService.loadUserByUsername(username);

            if(Objects.nonNull(userDetails) && Objects.nonNull(userDetails.getSchemaName())){
                TenantContext.setCurrentTenant(userDetails.getSchemaName());
            }
            // Validate the token and check if it is associated with the user details
            if (jwtService.validateToken(token, userDetails)) {
                // Create an authentication token for the user with their authorities
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                		userDetails, null, userDetails.getAuthorities());

                // Set additional details for the authentication token, such as the current request details
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication token in the security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Pass the request and response to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}
