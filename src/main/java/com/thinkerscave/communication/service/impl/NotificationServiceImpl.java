package com.thinkerscave.communication.service.impl;

import com.thinkerscave.communication.dto.request.NotificationRequest;
import com.thinkerscave.communication.dto.response.NotificationResponse;
import com.thinkerscave.communication.entity.Notification;
import com.thinkerscave.communication.entity.NotificationRecipient;
import com.thinkerscave.communication.enums.NotificationChannel;
import com.thinkerscave.communication.enums.NotificationStatus;
import com.thinkerscave.communication.repository.NotificationRepository;
import com.thinkerscave.communication.service.NotificationService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    @Override
    @Transactional
    public NotificationResponse send(NotificationRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Notification notification = new Notification();
        notification.setOrganizationId(orgId);
        notification.setSubject(request.getSubject());
        notification.setBody(request.getBody());
        notification.setCategory(request.getCategory());
        notification.setChannelsCsv(request.getChannelsCsv());
        notification.setScheduledAt(request.getScheduledAt());
        notification.setTotalRecipients(request.getRecipientUserIds().size());
        notification.setDeliveredCount(0);
        notification.setFailedCount(0);

        if (request.getScheduledAt() == null || request.getScheduledAt().isBefore(Instant.now())) {
            notification.setStatus(NotificationStatus.QUEUED);
            notification.setSentAt(Instant.now());
        } else {
            notification.setStatus(NotificationStatus.PENDING);
        }

        notification = repository.save(notification);
        log.info("Notification {} queued for {} recipients", notification.getNotificationId(),
                request.getRecipientUserIds().size());
        return toResponse(notification);
    }

    @Override
    public NotificationResponse getById(Long notificationId) {
        Long orgId = OrganizationContext.getOrganizationId();
        return toResponse(repository.findByNotificationIdAndOrganizationId(notificationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId)));
    }

    @Override
    public Page<NotificationResponse> getAll(Pageable pageable) {
        return repository.findByOrganizationIdOrderByCreatedOnDesc(OrganizationContext.getOrganizationId(), pageable)
                .map(this::toResponse);
    }

    @Override
    public Page<NotificationResponse> getByStatus(NotificationStatus status, Pageable pageable) {
        return repository.findByOrganizationIdAndStatusOrderByCreatedOnDesc(
                OrganizationContext.getOrganizationId(), status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public NotificationResponse cancel(Long notificationId) {
        Long orgId = OrganizationContext.getOrganizationId();
        Notification notification = repository.findByNotificationIdAndOrganizationId(notificationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (notification.getStatus() == NotificationStatus.SENT || notification.getStatus() == NotificationStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a notification that has already been sent");
        }
        notification.setStatus(NotificationStatus.CANCELLED);
        return toResponse(repository.save(notification));
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .subject(n.getSubject())
                .body(n.getBody())
                .category(n.getCategory())
                .channelsCsv(n.getChannelsCsv())
                .scheduledAt(n.getScheduledAt())
                .sentAt(n.getSentAt())
                .status(n.getStatus())
                .totalRecipients(n.getTotalRecipients())
                .deliveredCount(n.getDeliveredCount())
                .failedCount(n.getFailedCount())
                .createdOn(n.getCreatedOn())
                .createdBy(n.getCreatedBy())
                .build();
    }
}
