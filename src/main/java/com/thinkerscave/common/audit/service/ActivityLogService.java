package com.thinkerscave.common.audit.service;

import com.thinkerscave.common.audit.domain.ActivityLog;
import com.thinkerscave.common.audit.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service for recording and querying business activity events.
 * All record operations are asynchronous and fire-and-forget to avoid
 * impacting the main business transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Record a business activity event asynchronously.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long organizationId, String entityType, Long entityId,
                       String action, String description, String performedBy) {
        try {
            ActivityLog entry = ActivityLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .description(description)
                    .performedBy(performedBy)
                    .performedAt(Instant.now())
                    .build();
            entry.setOrganizationId(organizationId);
            activityLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to record activity: {} - {}", action, e.getMessage());
        }
    }

    /**
     * Get recent activities for an organization (paginated).
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getRecentActivities(Long organizationId, int page, int size) {
        return activityLogRepository.findByOrganizationIdOrderByPerformedAtDesc(
                organizationId, PageRequest.of(page, size));
    }

    /**
     * Get activities since a given time.
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getActivitiesSince(Long organizationId, Instant since) {
        return activityLogRepository.findByOrganizationIdAndPerformedAtAfterOrderByPerformedAtDesc(
                organizationId, since);
    }

    /**
     * Get activities by entity type.
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getActivitiesByType(Long organizationId, String entityType) {
        return activityLogRepository.findByOrganizationIdAndEntityTypeOrderByPerformedAtDesc(
                organizationId, entityType);
    }
}
