package com.thinkerscave.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.shared.exceptions.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for authentication endpoints.
 * Suitable for single-instance VPS; replace with Redis/gateway limits for multi-node.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${app.security.auth-rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.auth-rate-limit.max-requests:20}")
    private int maxRequests;

    @Value("${app.security.auth-rate-limit.window-seconds:60}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String key = resolveClientKey(request);
        long now = System.currentTimeMillis();
        pruneExpired(now);

        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(now));
        if (now - counter.windowStartMs > windowSeconds * 1000L) {
            counter.windowStartMs = now;
            counter.count.set(0);
        }

        if (counter.count.incrementAndGet() > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            ApiError error = ApiError.builder()
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .code("RATE_LIMITED")
                    .message("Too many authentication attempts. Please try again later.")
                    .timestamp(LocalDateTime.now())
                    .build();
            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void pruneExpired(long now) {
        if (counters.size() < 10_000) {
            return;
        }
        long ttl = windowSeconds * 1000L * 2;
        Iterator<Map.Entry<String, WindowCounter>> it = counters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, WindowCounter> entry = it.next();
            if (now - entry.getValue().windowStartMs > ttl) {
                it.remove();
            }
        }
    }

    private static final class WindowCounter {
        volatile long windowStartMs;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }
    }
}
