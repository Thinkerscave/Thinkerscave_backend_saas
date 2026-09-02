package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.security.dto.ClientEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Login history tracking. Rows older than 30 days are deleted by the retention process.
 */
public interface LoginHistoryService {

    Page<LoginHistoryResponse> getUserLoginHistory(
            Long userId,
            LoginStatus status,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            Pageable pageable);

    Page<LoginHistoryResponse> getOrganizationLoginHistory(
            Long organizationId,
            LoginStatus status,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            Pageable pageable);

    void markLogout(Long userId, ClientEnvironment client);
}
