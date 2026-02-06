package com.thinkerscave.common.usrm.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.usrm.service.JwtService;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtServiceImpl - Handles JWT token generation and validation.
 * 
 * IMPORTANT: This implementation embeds the tenant_id in the JWT claims
 * to prevent cross-tenant token hijacking attacks.
 */
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    private static final String TENANT_CLAIM = "tenant_id";

    @Override
    public String generateToken(String userName) {
        return generateToken(userName, 0);
    }

    @Override
    public String generateToken(String userName, int userId) {
        Map<String, Object> claims = new HashMap<>();

        // Include userId if provided
        if (userId > 0) {
            claims.put("userId", userId);
        }

        // CRITICAL: Embed current tenant in JWT for security validation
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !currentTenant.isBlank() && !"public".equals(currentTenant)) {
            claims.put(TENANT_CLAIM, currentTenant);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(userName)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    @Override
    public Integer extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Integer.class));
    }

    /**
     * Extracts the tenant_id claim from the JWT token.
     * This is used to validate that the request's X-Tenant-ID header matches the
     * token.
     */
    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get(TENANT_CLAIM, String.class));
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && isTokenSignatureValid(token));
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private boolean isTokenSignatureValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}