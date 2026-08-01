package com.thinkerscave.security.service.impl;

import com.thinkerscave.security.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;
    private final long rememberMeRefreshTokenExpiry;

    public JwtServiceImpl(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:900000}") long accessTokenExpiry,
            @Value("${refresh.token.expiration:86400000}") long refreshTokenExpiry,
            @Value("${refresh.token.remember-me-expiration:2592000000}") long rememberMeRefreshTokenExpiry) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
        this.rememberMeRefreshTokenExpiry = rememberMeRefreshTokenExpiry;
    }

    @Override
    public String generateAccessToken(String username, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        return buildToken(claims, username, accessTokenExpiry);
    }

    @Override
    public String generateRefreshToken(String username) {
        return generateRefreshToken(username, false);
    }

    @Override
    public String generateRefreshToken(String username, boolean rememberMe) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rememberMe", rememberMe);
        long expiry = rememberMe ? rememberMeRefreshTokenExpiry : refreshTokenExpiry;
        return buildToken(claims, username, expiry);
    }

    @Override
    public Boolean extractRememberMe(String token) {
        try {
            return extractAllClaims(token).get("rememberMe", Boolean.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiry) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    @Override
    public Long extractOrganizationId(String token) {
        return extractClaim(token, claims -> claims.get("orgId", Long.class));
    }

    @Override
    public String extractRoleType(String token) {
        return extractClaim(token, claims -> claims.get("roleType", String.class));
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            log.warn("JWT parsing failed: {}", ex.getMessage());
            throw ex;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
