package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.enums.LoginStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Login history tracking.
 */
public interface LoginHistoryService {

    Page<LoginHistoryResponse> getUserLoginHistory(Long userId, LoginStatus status, Pageable pageable);

    Page<LoginHistoryResponse> getOrganizationLoginHistory(Long organizationId, LoginStatus status, Pageable pageable);
}
