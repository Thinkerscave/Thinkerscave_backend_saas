package com.thinkerscave.communication.service.impl;

import com.thinkerscave.communication.dto.request.MessageRequest;
import com.thinkerscave.communication.dto.request.MessageThreadRequest;
import com.thinkerscave.communication.dto.response.MessageResponse;
import com.thinkerscave.communication.dto.response.MessageThreadResponse;
import com.thinkerscave.communication.entity.Message;
import com.thinkerscave.communication.entity.MessageThread;
import com.thinkerscave.communication.repository.MessageRepository;
import com.thinkerscave.communication.repository.MessageThreadRepository;
import com.thinkerscave.communication.service.MessageService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public MessageThreadResponse createThread(MessageThreadRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        MessageThread thread = new MessageThread();
        thread.setOrganizationId(orgId);
        thread.setSubject(request.getSubject());
        thread.setParticipantUserIdsCsv(
                request.getParticipantUserIds().stream()
                        .map(String::valueOf).collect(Collectors.joining(",")));
        thread.setClosed(false);
        return toThreadResponse(threadRepository.save(thread));
    }

    @Override
    public Page<MessageThreadResponse> getMyThreads(Long userId, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return threadRepository.findActiveThreadsForUser(orgId, String.valueOf(userId), pageable)
                .map(this::toThreadResponse);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long threadId, MessageRequest request, Long senderUserId) {
        Long orgId = OrganizationContext.getOrganizationId();
        MessageThread thread = threadRepository.findByThreadIdAndOrganizationId(threadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));
        if (thread.isClosed()) {
            throw new IllegalStateException("Cannot send message to a closed thread");
        }

        Message message = new Message();
        message.setMessageThreadId(threadId);
        message.setSenderUserId(senderUserId);
        message.setBody(request.getBody());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setSentAt(Instant.now());
        message.setDeleted(false);
        message = messageRepository.save(message);

        // Update thread last message timestamp
        thread.setLastMessageAt(message.getSentAt());
        threadRepository.save(thread);

        return toMessageResponse(message);
    }

    @Override
    public Page<MessageResponse> getMessages(Long threadId, Pageable pageable) {
        return messageRepository
                .findByMessageThreadIdAndDeletedFalseOrderBySentAtAsc(threadId, pageable)
                .map(this::toMessageResponse);
    }

    @Override
    @Transactional
    public void closeThread(Long threadId) {
        Long orgId = OrganizationContext.getOrganizationId();
        MessageThread thread = threadRepository.findByThreadIdAndOrganizationId(threadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));
        thread.setClosed(true);
        threadRepository.save(thread);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private MessageThreadResponse toThreadResponse(MessageThread t) {
        return MessageThreadResponse.builder()
                .threadId(t.getThreadId())
                .subject(t.getSubject())
                .participantUserIdsCsv(t.getParticipantUserIdsCsv())
                .lastMessageAt(t.getLastMessageAt())
                .closed(t.isClosed())
                .createdOn(t.getCreatedOn())
                .createdBy(t.getCreatedBy())
                .build();
    }

    private MessageResponse toMessageResponse(Message m) {
        return MessageResponse.builder()
                .messageId(m.getMessageId())
                .threadId(m.getMessageThreadId())
                .senderUserId(m.getSenderUserId())
                .body(m.getBody())
                .attachmentUrl(m.getAttachmentUrl())
                .sentAt(m.getSentAt())
                .build();
    }
}
