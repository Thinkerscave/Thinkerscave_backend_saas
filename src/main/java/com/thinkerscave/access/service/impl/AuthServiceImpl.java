package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.LoginRequest;
import com.thinkerscave.access.dto.response.AuthResponse;
import com.thinkerscave.access.dto.response.SessionResponse;
import com.thinkerscave.access.dto.response.UserSummaryResponse;
import com.thinkerscave.access.entity.*;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.enums.SessionStatus;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.mapper.UserMapper;
import com.thinkerscave.access.repository.*;
import com.thinkerscave.access.security.JwtService;
import com.thinkerscave.access.service.AuthService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            recordLoginFailure(user, "Invalid password");
            incrementFailedAttempts(user);
            throw new BadRequestException("Invalid credentials");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("orgId", user.getOrganizationId());
        claims.put("userCode", user.getUserCode());

        UserRole primaryRole = user.getUserRoles().stream()
                .filter(ur -> Boolean.TRUE.equals(ur.getPrimaryRole()) && Boolean.TRUE.equals(ur.getActive()))
                .findFirst().orElse(null);
        if (primaryRole != null) {
            claims.put("roleType", primaryRole.getRole().getRoleType().name());
        }

        String accessToken = jwtService.generateAccessToken(user.getUsername(), claims);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        // Create session
        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .deviceName(request.getDeviceName())
                .ipAddress("") // Resolved from request in controller if needed
                .loginAt(LocalDateTime.now())
                .status(SessionStatus.ACTIVE)
                .build();
        sessionRepository.save(session);

        recordLoginSuccess(user);

        UserSummaryResponse userResponse = userMapper.toSummary(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .firstTimeLogin(user.getFirstTimeLogin())
                .requirePasswordChange(Boolean.TRUE.equals(user.getFirstTimeLogin()))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        UserSession session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BadRequestException("Session is no longer active");
        }

        User user = session.getUser();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("orgId", user.getOrganizationId());
        claims.put("userCode", user.getUserCode());

        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), claims);
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());

        session.setRefreshToken(newRefreshToken);
        sessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userMapper.toSummary(user))
                .firstTimeLogin(user.getFirstTimeLogin())
                .requirePasswordChange(Boolean.TRUE.equals(user.getFirstTimeLogin()))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        sessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
            session.setStatus(SessionStatus.LOGGED_OUT);
            session.setLogoutAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    @Override
    @Transactional
    public void logoutAllSessions(Long userId) {
        sessionRepository.terminateAllActiveSessions(userId, LocalDateTime.now());
        log.info("All sessions terminated for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SessionResponse> getUserSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUser_IdOrderByLoginAtDesc(userId, pageable)
                .map(this::mapSession);
    }

    @Override
    @Transactional
    public void terminateSession(Long sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        session.setStatus(SessionStatus.LOGGED_OUT);
        session.setLogoutAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private void recordLoginSuccess(User user) {        LoginHistory history = LoginHistory.builder()
                .user(user)
                .status(LoginStatus.SUCCESS)
                .loginTime(LocalDateTime.now())
                .build();
        loginHistoryRepository.save(history);
    }

    private void recordLoginFailure(User user, String reason) {
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .status(LoginStatus.FAILED)
                .loginTime(LocalDateTime.now())
                .failureReason(reason)
                .build();
        loginHistoryRepository.save(history);
    }

    private void incrementFailedAttempts(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        userRepository.save(user);
    }

    private Page<SessionResponse> toSessionResponse(Page<UserSession> page) {
        return page.map(this::mapSession);
    }

    private SessionResponse mapSession(UserSession s) {
        return SessionResponse.builder()
                .id(s.getId())
                .userId(s.getUser().getId())
                .username(s.getUser().getUsername())
                .deviceName(s.getDeviceName())
                .browser(s.getBrowser())
                .operatingSystem(s.getOperatingSystem())
                .ipAddress(s.getIpAddress())
                .status(s.getStatus())
                .loginAt(s.getLoginAt())
                .logoutAt(s.getLogoutAt())
                .build();
    }
}
