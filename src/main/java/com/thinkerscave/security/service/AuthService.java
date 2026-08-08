package com.thinkerscave.security.service;

import com.thinkerscave.security.dto.LoginContext;
import com.thinkerscave.security.dto.request.LoginRequest;
import com.thinkerscave.security.dto.response.AuthResponse;
import com.thinkerscave.security.dto.response.SessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Authentication and session management.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request, LoginContext loginContext);

    /**
     * @param preferredTenant optional {@code X-Tenant-ID} from the refresh request — preserved when
     *                        valid for the user (avoids flipping Owners back to their first org)
     * @param preferredOrgId  optional {@code X-Organization-ID} from the refresh request
     */
    AuthResponse refreshToken(String refreshToken, String preferredTenant, Long preferredOrgId);

    void logout(String refreshToken);

    void logoutAllSessions(Long userId);

    Page<SessionResponse> getUserSessions(Long userId, Pageable pageable);

    void terminateSession(Long sessionId);
}
