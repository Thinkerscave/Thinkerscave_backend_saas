package com.thinkerscave.retention;

import com.thinkerscave.retention.config.RetentionProperties;
import com.thinkerscave.retention.service.RetentionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionScheduler {

    private final RetentionService retentionService;
    private final java.util.List<RetentionTask> tasks;
    private final RetentionProperties retentionProperties;

    @Scheduled(cron = "${app.retention.cron:0 30 2 * * *}", zone = "UTC")
    public void runNightly() {
        if (!retentionProperties.isEnabled()) {
            log.info("Nightly retention is disabled (app.retention.enabled=false) — skipping");
            return;
        }
        var tasks = this.tasks.stream()
                .filter(task -> retentionProperties.configOf(task.key()) == null
                        || retentionProperties.configOf(task.key()).getEnabled() == null
                        || retentionProperties.configOf(task.key()).getEnabled())
                .toList();
        log.info("Starting nightly archival purge for {} task(s)", tasks.size());
        for (RetentionTask task : tasks) {
            try {
                retentionService.run(task.key(), RetentionTrigger.SCHEDULED, null, "system");
            } catch (Exception ex) {
                log.warn("Nightly retention failed for {}: {}", task.key(), ex.getMessage());
            }
        }
    }
}