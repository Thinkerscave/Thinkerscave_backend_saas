package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.response.LoginHistoryResponse;
import com.thinkerscave.access.entity.LoginHistory;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.repository.LoginHistoryRepository;
import com.thinkerscave.access.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    static final int MAX_RETENTION_DAYS = 30;
    static final int DEFAULT_WINDOW_DAYS = 7;

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
    public Page<LoginHistoryResponse> getOrganizationLoginHistory(
            Long organizationId,
            LoginStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {
        LocalDateTime[] window = clampWindow(from, to);
        Page<LoginHistory> page = loginHistoryRepository.findByOrganizationIdAndWindow(
                organizationId, status, window[0], window[1], pageable);
        return page.map(this::toResponse);
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purgeExpiredLoginHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(MAX_RETENTION_DAYS);
        int deleted = loginHistoryRepository.deleteByLoginTimeBefore(cutoff);
        if (deleted > 0) {
            log.info("Purged {} login history rows older than {} days", deleted, MAX_RETENTION_DAYS);
        }
    }

    private LocalDateTime[] clampWindow(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliest = now.minusDays(MAX_RETENTION_DAYS);
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
                .browser(lh.getBrowser())
                .operatingSystem(lh.getOperatingSystem())
                .failureReason(lh.getFailureReason())
                .build();
    }
}
