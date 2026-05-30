package com.thinkerscave.common.audit.repository;

import com.thinkerscave.common.audit.domain.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findByOrganizationIdOrderByPerformedAtDesc(Long organizationId, Pageable pageable);

    List<ActivityLog> findByOrganizationIdAndEntityTypeOrderByPerformedAtDesc(
            Long organizationId, String entityType);

    List<ActivityLog> findByOrganizationIdAndPerformedAtAfterOrderByPerformedAtDesc(
            Long organizationId, Instant since);

    Page<ActivityLog> findByOrganizationIdAndPerformedByOrderByPerformedAtDesc(
            Long organizationId, String performedBy, Pageable pageable);
}
