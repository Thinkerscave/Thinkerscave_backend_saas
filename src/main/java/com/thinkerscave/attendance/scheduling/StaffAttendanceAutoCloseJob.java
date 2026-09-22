package com.thinkerscave.attendance.scheduling;

import com.thinkerscave.attendance.service.StaffAttendanceService;
import com.thinkerscave.finance.scheduling.FinanceTenantJobRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically auto-closes staff attendance sessions that remain open past 24 hours.
 * Expiry is also enforced on attendance read/write paths so a delayed scheduler
 * cannot leave an invalid active session visible.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffAttendanceAutoCloseJob {

    private final FinanceTenantJobRunner tenantJobRunner;
    private final StaffAttendanceService staffAttendanceService;

    @Scheduled(cron = "${app.attendance.auto-close.cron:0 */15 * * * *}", zone = "UTC")
    public void autoCloseExpiredSessions() {
        tenantJobRunner.forEachTenant("StaffAttendanceAutoCloseJob", tenant -> {
            int closed = staffAttendanceService.autoCloseExpiredSessions();
            if (closed > 0) {
                log.info("StaffAttendanceAutoCloseJob closed {} session(s) for tenant {}",
                        closed, tenant.getTenantIdentifier());
            }
        });
    }
}
