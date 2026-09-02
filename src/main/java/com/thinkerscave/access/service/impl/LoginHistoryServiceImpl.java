package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.entity.LoginHistory;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.repository.LoginHistoryRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.LoginHistoryService;
import com.thinkerscave.retention.LoginHistoryRetentionTask;
import com.thinkerscave.retention.config.RetentionProperties;
import com.thinkerscave.security.dto.ClientEnvironment;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    static final int DEFAULT_WINDOW_DAYS = 7;
    static final int ABSOLUTE_MAX_WINDOW_DAYS = 365;

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final RetentionProperties retentionProperties;

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getUserLoginHistory(
            Long userId,
            LoginStatus status,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            Pageable pageable) {
        assertUserInScope(userId);
        LocalDateTime[] window = clampWindow(from, to);
        return loginHistoryRepository.findByUserIdAndWindow(
                        userId, status, window[0], window[1], normalizeSearch(search), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getOrganizationLoginHistory(
            Long organizationId,
            LoginStatus status,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            Pageable pageable) {
        LocalDateTime[] window = clampWindow(from, to);
        return loginHistoryRepository.findByOrganizationIdAndWindow(
                        organizationId, status, window[0], window[1], normalizeSearch(search), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void markLogout(Long userId, ClientEnvironment client) {
        if (userId == null) {
            return;
        }
        loginHistoryRepository.findTopByUser_IdAndStatusAndLogoutTimeIsNullOrderByLoginTimeDesc(userId, LoginStatus.SUCCESS)
                .ifPresent(history -> {
                    history.setLogoutTime(LocalDateTime.now());
                    if (client != null && StringUtils.hasText(client.ipAddress())) {
                        history.setLogoutIpAddress(client.ipAddress());
                    }
                    loginHistoryRepository.save(history);
                });
    }

    private String normalizeSearch(String search) {
        return StringUtils.hasText(search) ? search.trim() : null;
    }

    private void assertUserInScope(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Long contextOrg = OrganizationContext.getOrganizationId();
        if (contextOrg != null && user.getOrganizationId() != null
                && !contextOrg.equals(user.getOrganizationId())) {
            throw new AccessDeniedException("Not authorized for this user's login history");
        }
    }

    private int retentionWindowDays() {
        RetentionProperties.TaskConfig config = retentionProperties.configOf(LoginHistoryRetentionTask.KEY);
        Integer days = config != null ? config.getRetentionDays() : null;
        if (days == null) {
            days = LoginHistoryRetentionTask.RETENTION_DAYS;
        }
        return Math.max(1, Math.min(days, ABSOLUTE_MAX_WINDOW_DAYS));
    }

    private LocalDateTime[] clampWindow(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliest = now.minusDays(retentionWindowDays());
        LocalDateTime start = from != null ? from : now.minusDays(DEFAULT_WINDOW_DAYS);
        if (start.isBefore(earliest)) {
            start = earliest;
        }
        LocalDateTime end = to != null ? to : now;
        if (end.isBefore(start)) {
            end = now;
        }
        return new LocalDateTime[] { start, end };
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
                .logoutIpAddress(lh.getLogoutIpAddress())
                .deviceName(lh.getDeviceName())
                .browser(lh.getBrowser())
                .operatingSystem(lh.getOperatingSystem())
                .failureReason(lh.getFailureReason())
                .build();
    }
}
