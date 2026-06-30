package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.entity.LoginHistory;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.repository.LoginHistoryRepository;
import com.thinkerscave.access.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getUserLoginHistory(Long userId, LoginStatus status, Pageable pageable) {
        Page<LoginHistory> page = (status != null)
                ? loginHistoryRepository.findByUser_IdAndStatusOrderByLoginTimeDesc(userId, status, pageable)
                : loginHistoryRepository.findByUser_IdOrderByLoginTimeDesc(userId, pageable);
        return page.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getOrganizationLoginHistory(Long organizationId, LoginStatus status, Pageable pageable) {
        Page<LoginHistory> page = (status != null)
                ? loginHistoryRepository.findByOrganizationIdAndStatus(organizationId, status, pageable)
                : loginHistoryRepository.findByOrganizationId(organizationId, pageable);
        return page.map(this::toResponse);
    }

    private LoginHistoryResponse toResponse(LoginHistory lh) {
        return LoginHistoryResponse.builder()
                .id(lh.getId())
                .userId(lh.getUser() != null ? lh.getUser().getId() : null)
                .username(lh.getUser() != null ? lh.getUser().getUsername() : null)
                .displayName(lh.getUser() != null ? lh.getUser().getDisplayName() : null)
                .status(lh.getStatus())
                .loginTime(lh.getLoginTime())
                .logoutTime(lh.getLogoutTime())
                .ipAddress(lh.getIpAddress())
                .browser(lh.getBrowser())
                .operatingSystem(lh.getOperatingSystem())
                .failureReason(lh.getFailureReason())
                .build();
    }
}
