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

    String extractUsername(String token);

    Long extractUserId(String token);

    Long extractOrganizationId(String token);

    String extractRoleType(String token);

    Date extractExpiration(String token);

    Claims extractAllClaims(String token);

    boolean isTokenValid(String token, UserDetails userDetails);

    boolean isTokenExpired(String token);
}
