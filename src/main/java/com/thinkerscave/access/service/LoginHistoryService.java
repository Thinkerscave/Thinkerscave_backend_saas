package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.enums.LoginStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Login history tracking. Rows older than 30 days are not kept.
 */
public interface LoginHistoryService {

    Page<LoginHistoryResponse> getUserLoginHistory(Long userId, LoginStatus status, Pageable pageable);

    Page<LoginHistoryResponse> getOrganizationLoginHistory(
            Long organizationId,
            LoginStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);
}
