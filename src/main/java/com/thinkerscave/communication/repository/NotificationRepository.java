package com.thinkerscave.communication.repository;

import com.thinkerscave.communication.entity.Notification;
import com.thinkerscave.communication.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByOrganizationIdOrderByCreatedOnDesc(Long orgId, Pageable pageable);

    Page<Notification> findByOrganizationIdAndStatusOrderByCreatedOnDesc(Long orgId, NotificationStatus status, Pageable pageable);

    Optional<Notification> findByNotificationIdAndOrganizationId(Long notificationId, Long orgId);
}
