package com.thinkerscave.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Issues and clears the HttpOnly refresh-token cookie.
 * Prefer cookie over query/body so browser JavaScript never needs the refresh token.
 */
@Component
public class RefreshTokenCookieHelper {

    @Value("${app.auth.refresh-cookie.enabled:true}")
    private boolean enabled;

    @Value("${app.auth.refresh-cookie.name:tc_refresh_token}")
    private String cookieName;

    @Value("${app.auth.refresh-cookie.path:/api/auth}")
    private String cookiePath;

    @Value("${app.auth.refresh-cookie.max-age-seconds:86400}")
    private long maxAgeSeconds;

    /** Persistent cookie lifetime when "Remember this device" is checked (default 30 days). */
    @Value("${app.auth.refresh-cookie.remember-me-max-age-seconds:2592000}")
    private long rememberMeMaxAgeSeconds;

    @Value("${app.auth.refresh-cookie.secure:false}")
    private boolean secure;

    @Value("${app.auth.refresh-cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.auth.refresh-cookie.domain:}")
    private String domain;

    public boolean isEnabled() {
        return enabled;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        setRefreshTokenCookie(response, refreshToken, null);
    }

    /**
     * @param rememberMe {@code true} = persistent cookie, {@code false} = browser-session cookie,
     *                   {@code null} = configured default max-age (legacy refresh tokens).
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, Boolean rememberMe) {
        if (!enabled || !StringUtils.hasText(refreshToken)) {
            return;
        }
        long maxAge;
        if (Boolean.TRUE.equals(rememberMe)) {
            maxAge = rememberMeMaxAgeSeconds;
        } else if (Boolean.FALSE.equals(rememberMe)) {
            // Session cookie: discarded when the browser closes.
            maxAge = -1;
        } else {
            maxAge = maxAgeSeconds;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshToken, maxAge).toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        if (!enabled) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", 0).toString());
    }

    /**
     * Resolves refresh token from HttpOnly cookie first, then optional request param (legacy clients).
     */
    public String resolveRefreshToken(HttpServletRequest request, String requestParamToken) {
        String fromCookie = readCookie(request);
        if (StringUtils.hasText(fromCookie)) {
            return fromCookie;
        }
        return StringUtils.hasText(requestParamToken) ? requestParamToken : null;
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, value == null ? "" : value)
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(maxAge)
                .sameSite(sameSite);

        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }
        return builder.build();
    }
}
