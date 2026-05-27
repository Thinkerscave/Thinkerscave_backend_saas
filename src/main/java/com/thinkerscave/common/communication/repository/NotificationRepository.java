package com.thinkerscave.common.communication.repository;

import com.thinkerscave.common.communication.domain.Notification;
import com.thinkerscave.common.communication.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Page<Notification> findByOrganizationId(Long organizationId, Pageable pageable);

    List<Notification> findByStatusAndScheduledAtBefore(NotificationStatus status, Instant cutoff);
}
