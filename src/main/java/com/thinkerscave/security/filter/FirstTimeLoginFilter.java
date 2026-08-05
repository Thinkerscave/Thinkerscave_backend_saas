package com.thinkerscave.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.security.service.JwtService;
import com.thinkerscave.shared.exceptions.ApiError;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Blocks protected business APIs while {@code firstTimeLogin=true} on the access token.
 * Only password change, current-user profile read, and auth/logout flows remain available.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirstTimeLoginFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> ALLOWED_PATTERNS = List.of(
            "/api/auth/**",
            "/api/v1/public/**",
            "/api/v1/profile/me",
            "/api/v1/profile/me/change-password",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isAlwaysAllowed(path) || isAllowedWhileFirstTimeLogin(request.getMethod(), path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length());
        try {
            if (jwtService.extractFirstTimeLogin(jwt)) {
                log.warn("Blocked API call for first-time-login user: {} {}", request.getMethod(), path);
                sendForbidden(response, "Password change required before accessing the application");
                return;
            }
        } catch (JwtException ex) {
            // JwtAuthenticationFilter owns invalid-token responses.
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAlwaysAllowed(String path) {
        return ALLOWED_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isAllowedWhileFirstTimeLogin(String method, String path) {
        if ("GET".equalsIgnoreCase(method) && "/api/v1/profile/me".equals(path)) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) && "/api/v1/profile/me/change-password".equals(path);
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .code("PASSWORD_CHANGE_REQUIRED")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
