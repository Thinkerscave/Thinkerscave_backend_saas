package com.thinkerscave.common.communication.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.communication.domain.Message;
import com.thinkerscave.common.communication.domain.MessageThread;
import com.thinkerscave.common.communication.dto.MessageDTO;
import com.thinkerscave.common.communication.dto.MessageThreadDTO;
import com.thinkerscave.common.communication.repository.MessageRepository;
import com.thinkerscave.common.communication.repository.MessageThreadRepository;
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

/**
 * Direct-message threads & their messages. Threads are lightweight (CSV of
 * participant user ids) — fine for the scaffold; can be normalized later.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MessageService {

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final AuditPublisher auditPublisher;

    public Page<MessageThreadDTO> listThreads(Pageable pageable) {
        return threadRepository.findByOrganizationIdOrderByLastMessageAtDesc(currentOrgId(), pageable)
                .map(this::toDto);
    }

    public MessageThreadDTO getThread(Long threadId) {
        return toDto(loadThread(threadId));
    }

    public Page<MessageDTO> listMessages(Long threadId, Pageable pageable) {
        return messageRepository.findByMessageThreadIdOrderBySentAtAsc(threadId, pageable).map(this::toDto);
    }

    @Transactional
    public MessageThreadDTO createThread(MessageThreadDTO dto) {
        Long orgId = currentOrgId();
        MessageThread t = new MessageThread();
        t.setOrganizationId(orgId);
        t.setSubject(dto.getSubject());
        t.setParticipantUserIdsCsv(dto.getParticipantUserIdsCsv());
        t.setContextRef(dto.getContextRef());
        t.setClosed(false);
        MessageThread saved = threadRepository.save(t);
        auditPublisher.publish(AuditEventType.CREATE, "message_thread.create",
                "MessageThread", saved.getId(), "Thread '" + saved.getSubject() + "' opened");
        return toDto(saved);
    }

    @Transactional
    public MessageDTO postMessage(Long threadId, MessageDTO dto) {
        MessageThread t = loadThread(threadId);
        if (t.isClosed()) throw new BadRequestException("Thread is closed");
        Message m = new Message();
        m.setMessageThreadId(threadId);
        m.setSenderUserId(dto.getSenderUserId());
        m.setBody(dto.getBody());
        m.setAttachmentUrl(dto.getAttachmentUrl());
        m.setSentAt(Instant.now());
        m.setDeleted(false);
        Message saved = messageRepository.save(m);

        t.setLastMessageAt(saved.getSentAt());
        t.setLastMessageByUserId(saved.getSenderUserId());
        threadRepository.save(t);

        auditPublisher.publish(AuditEventType.CREATE, "message.post",
                "Message", saved.getId(), "Message posted in thread " + threadId);
        return toDto(saved);
    }

    @Transactional
    public MessageThreadDTO closeThread(Long threadId) {
        MessageThread t = loadThread(threadId);
        t.setClosed(true);
        MessageThread saved = threadRepository.save(t);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "message_thread.close",
                "MessageThread", threadId, "Thread closed");
        return toDto(saved);
    }

    @Transactional
    public void softDeleteMessage(Long messageId) {
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        m.setDeleted(true);
        messageRepository.save(m);
        auditPublisher.publish(AuditEventType.DELETE, "message.delete",
                "Message", messageId, "Message soft-deleted");
    }

    private MessageThread loadThread(Long id) {
        return threadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MessageThread not found: " + id));
    }

    private MessageThreadDTO toDto(MessageThread t) {
        return MessageThreadDTO.builder()
                .id(t.getId())
                .subject(t.getSubject())
                .participantUserIdsCsv(t.getParticipantUserIdsCsv())
                .contextRef(t.getContextRef())
                .lastMessageAt(t.getLastMessageAt())
                .lastMessageByUserId(t.getLastMessageByUserId())
                .closed(t.isClosed())
                .build();
    }

    private MessageDTO toDto(Message m) {
        return MessageDTO.builder()
                .id(m.getId())
                .messageThreadId(m.getMessageThreadId())
                .senderUserId(m.getSenderUserId())
                .body(m.getBody())
                .attachmentUrl(m.getAttachmentUrl())
                .sentAt(m.getSentAt())
                .editedAt(m.getEditedAt())
                .deleted(m.isDeleted())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
