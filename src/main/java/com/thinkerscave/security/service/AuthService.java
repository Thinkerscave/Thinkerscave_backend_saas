package com.thinkerscave.security.service;

import com.thinkerscave.security.dto.request.LoginRequest;
import com.thinkerscave.security.dto.response.AuthResponse;
import com.thinkerscave.security.dto.response.SessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Authentication and session management.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void logoutAllSessions(Long userId);

    Page<SessionResponse> getUserSessions(Long userId, Pageable pageable);

    void terminateSession(Long sessionId);
}
