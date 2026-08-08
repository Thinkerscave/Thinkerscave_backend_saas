package com.thinkerscave.security.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.Map;

/**
 * Contract for JWT token operations.
 */
public interface JwtService {

    String generateAccessToken(String username, Map<String, Object> extraClaims);

    String generateRefreshToken(String username);

    /**
     * Issues a refresh token whose lifetime depends on "remember this device".
     */
    String generateRefreshToken(String username, boolean rememberMe);

    /**
     * Reads the remember-me claim from a refresh token. Returns null when absent (legacy tokens).
     */
    Boolean extractRememberMe(String token);

    String extractUsername(String token);

    Long extractUserId(String token);

    Long extractOrganizationId(String token);

    String extractRoleType(String token);

    /**
     * Returns true when the access token requires a mandatory first-login password change.
     * Absent or null claims are treated as false (legacy tokens).
     */
    boolean extractFirstTimeLogin(String token);

    Date extractExpiration(String token);

    Claims extractAllClaims(String token);

    boolean isTokenValid(String token, UserDetails userDetails);

    boolean isTokenExpired(String token);
}
