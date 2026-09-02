package com.thinkerscave.retention.repository;

import com.thinkerscave.retention.entity.RetentionPurgeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RetentionPurgeLogRepository extends JpaRepository<RetentionPurgeLog, Long> {

    Optional<RetentionPurgeLog> findFirstByTaskKeyOrderByRanAtDesc(String taskKey);

    Optional<RetentionPurgeLog> findFirstByTaskKeyAndOrganizationIdOrderByRanAtDesc(String taskKey, Long organizationId);

    Page<RetentionPurgeLog> findByTaskKeyOrderByRanAtDesc(String taskKey, Pageable pageable);
}
