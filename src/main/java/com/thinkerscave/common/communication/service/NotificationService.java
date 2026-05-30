package com.thinkerscave.common.communication.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.communication.domain.Notification;
import com.thinkerscave.common.communication.domain.NotificationChannel;
import com.thinkerscave.common.communication.domain.NotificationRecipient;
import com.thinkerscave.common.communication.domain.NotificationStatus;
import com.thinkerscave.common.communication.dto.NotificationDTO;
import com.thinkerscave.common.communication.dto.NotificationRecipientDTO;
import com.thinkerscave.common.communication.mapper.CommunicationMapper;
import com.thinkerscave.common.communication.repository.NotificationRecipientRepository;
import com.thinkerscave.common.communication.repository.NotificationRepository;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Notification creation, recipient fan-out, dispatch tracking, and per-user
 * inbox queries. Actual transport (email/SMS/push) is delegated — this
 * service maintains state and audit trail.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final AuditPublisher auditPublisher;
    private final CommunicationMapper communicationMapper;

    public Page<NotificationDTO> list(Pageable pageable) {
        return notificationRepository.findByOrganizationId(currentOrgId(), pageable).map(communicationMapper::toNotificationDto);
    }

    public NotificationDTO get(Long id) {
        return communicationMapper.toNotificationDto(load(id));
    }

    public List<NotificationRecipientDTO> recipientsOf(Long notificationId) {
        return recipientRepository.findByNotificationId(notificationId).stream().map(communicationMapper::toRecipientDto).toList();
    }

    public Page<NotificationRecipientDTO> inbox(Long userId, NotificationStatus status, Pageable pageable) {
        NotificationStatus effective = status != null ? status : NotificationStatus.DELIVERED;
        return recipientRepository.findByUserIdAndStatus(userId, effective, pageable).map(communicationMapper::toRecipientDto);
    }

    public long unreadCount(Long userId) {
        return recipientRepository.countByUserIdAndStatus(userId, NotificationStatus.DELIVERED);
    }

    @Transactional
    public NotificationDTO create(NotificationDTO dto, List<NotificationRecipientDTO> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new BadRequestException("At least one recipient is required");
        }
        Long orgId = currentOrgId();
        Notification n = new Notification();
        n.setOrganizationId(orgId);
        n.setSubject(dto.getSubject());
        n.setBody(dto.getBody());
        n.setChannelsCsv(dto.getChannelsCsv());
        n.setCategory(dto.getCategory());
        n.setSeverity(dto.getSeverity());
        n.setScheduledAt(dto.getScheduledAt());
        n.setStatus(dto.getScheduledAt() != null ? NotificationStatus.QUEUED : NotificationStatus.PENDING);
        n.setTriggeredByUserId(dto.getTriggeredByUserId());
        n.setSourceRef(dto.getSourceRef());
        n.setTotalRecipients(recipients.size());
        n.setDeliveredCount(0);
        n.setFailedCount(0);
        Notification saved = notificationRepository.save(n);

        for (NotificationRecipientDTO r : recipients) {
            NotificationRecipient nr = new NotificationRecipient();
            nr.setNotificationId(saved.getId());
            nr.setUserId(r.getUserId());
            nr.setAddress(r.getAddress());
            nr.setChannel(r.getChannel());
            nr.setStatus(NotificationStatus.PENDING);
            nr.setAttemptCount(0);
            recipientRepository.save(nr);
        }

        auditPublisher.publish(AuditEventType.CREATE, "notification.create",
                "Notification", saved.getId(),
                "Notification '" + saved.getSubject() + "' for " + recipients.size() + " recipients");
        return communicationMapper.toNotificationDto(saved);
    }

    @Transactional
    public NotificationRecipientDTO markSent(Long recipientId, String providerMessageId) {
        NotificationRecipient r = loadRecipient(recipientId);
        r.setStatus(NotificationStatus.SENT);
        r.setSentAt(Instant.now());
        r.setProviderMessageId(providerMessageId);
        r.setAttemptCount((r.getAttemptCount() == null ? 0 : r.getAttemptCount()) + 1);
        return communicationMapper.toRecipientDto(recipientRepository.save(r));
    }

    @Transactional
    public NotificationRecipientDTO markDelivered(Long recipientId) {
        NotificationRecipient r = loadRecipient(recipientId);
        r.setStatus(NotificationStatus.DELIVERED);
        r.setDeliveredAt(Instant.now());
        incrementParent(r.getNotificationId(), true);
        return communicationMapper.toRecipientDto(recipientRepository.save(r));
    }

    @Transactional
    public NotificationRecipientDTO markRead(Long recipientId) {
        NotificationRecipient r = loadRecipient(recipientId);
        r.setStatus(NotificationStatus.READ);
        r.setReadAt(Instant.now());
        return communicationMapper.toRecipientDto(recipientRepository.save(r));
    }

    @Transactional
    public NotificationRecipientDTO markFailed(Long recipientId, String reason) {
        NotificationRecipient r = loadRecipient(recipientId);
        r.setStatus(NotificationStatus.FAILED);
        r.setFailureReason(reason);
        r.setAttemptCount((r.getAttemptCount() == null ? 0 : r.getAttemptCount()) + 1);
        incrementParent(r.getNotificationId(), false);
        return communicationMapper.toRecipientDto(recipientRepository.save(r));
    }

    @Transactional
    public NotificationDTO cancel(Long id) {
        Notification n = load(id);
        if (n.getStatus() == NotificationStatus.SENT || n.getStatus() == NotificationStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel notification already dispatched");
        }
        n.setStatus(NotificationStatus.CANCELLED);
        Notification saved = notificationRepository.save(n);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "notification.cancel",
                "Notification", id, "Notification cancelled");
        return communicationMapper.toNotificationDto(saved);
    }

    private void incrementParent(Long notificationId, boolean delivered) {
        Notification n = load(notificationId);
        if (delivered) {
            n.setDeliveredCount((n.getDeliveredCount() == null ? 0 : n.getDeliveredCount()) + 1);
        } else {
            n.setFailedCount((n.getFailedCount() == null ? 0 : n.getFailedCount()) + 1);
        }
        if (n.getDeliveredCount() != null && n.getTotalRecipients() != null
                && n.getDeliveredCount() + (n.getFailedCount() == null ? 0 : n.getFailedCount())
                    >= n.getTotalRecipients()) {
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(Instant.now());
        }
        notificationRepository.save(n);
    }

    private Notification load(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }

    private NotificationRecipient loadRecipient(Long id) {
        return recipientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationRecipient not found: " + id));
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
