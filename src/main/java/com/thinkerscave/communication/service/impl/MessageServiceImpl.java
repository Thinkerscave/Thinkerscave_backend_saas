package com.thinkerscave.communication.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.communication.dto.request.MessageRequest;
import com.thinkerscave.communication.dto.request.MessageThreadRequest;
import com.thinkerscave.communication.dto.response.MessageResponse;
import com.thinkerscave.communication.dto.response.MessageThreadResponse;
import com.thinkerscave.communication.entity.Message;
import com.thinkerscave.communication.entity.MessageThread;
import com.thinkerscave.communication.repository.MessageRepository;
import com.thinkerscave.communication.repository.MessageThreadRepository;
import com.thinkerscave.communication.service.MessageService;
import com.thinkerscave.communication.util.ParticipantCsv;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MessageThreadResponse createThread(MessageThreadRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Long currentUserId = requireCurrentUserId();

        Set<Long> participants = new LinkedHashSet<>();
        if (request.getParticipantUserIds() != null) {
            participants.addAll(request.getParticipantUserIds());
        }
        participants.add(currentUserId);

        MessageThread thread = new MessageThread();
        thread.setOrganizationId(orgId);
        thread.setSubject(request.getSubject());
        thread.setParticipantUserIdsCsv(ParticipantCsv.join(participants));
        thread.setClosed(false);
        return toThreadResponse(threadRepository.save(thread));
    }

    @Override
    public Page<MessageThreadResponse> getMyThreads(Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        Long currentUserId = requireCurrentUserId();
        return threadRepository.findActiveThreadsForUser(orgId, String.valueOf(currentUserId), pageable)
                .map(this::toThreadResponse);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long threadId, MessageRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        Long currentUserId = requireCurrentUserId();
        MessageThread thread = requireThreadInOrg(threadId, orgId);
        requireParticipant(thread, currentUserId);

        if (thread.isClosed()) {
            throw new BadRequestException("Cannot send message to a closed thread");
        }

        Message message = new Message();
        message.setMessageThreadId(threadId);
        message.setSenderUserId(currentUserId);
        message.setBody(request.getBody());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setSentAt(Instant.now());
        message.setDeleted(false);
        message = messageRepository.save(message);

        thread.setLastMessageAt(message.getSentAt());
        threadRepository.save(thread);

        return toMessageResponse(message);
    }

    @Override
    public Page<MessageResponse> getMessages(Long threadId, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        Long currentUserId = requireCurrentUserId();
        MessageThread thread = requireThreadInOrg(threadId, orgId);
        requireParticipant(thread, currentUserId);

        return messageRepository
                .findByMessageThreadIdAndDeletedFalseOrderBySentAtAsc(threadId, pageable)
                .map(this::toMessageResponse);
    }

    @Override
    @Transactional
    public void closeThread(Long threadId) {
        Long orgId = OrganizationContext.getOrganizationId();
        MessageThread thread = requireThreadInOrg(threadId, orgId);
        thread.setClosed(true);
        threadRepository.save(thread);
    }

    // ─── Auth / membership helpers ───────────────────────────────────────────

    Long requireCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found: " + username));
        return user.getId();
    }

    private MessageThread requireThreadInOrg(Long threadId, Long orgId) {
        return threadRepository.findByThreadIdAndOrganizationId(threadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));
    }

    private void requireParticipant(MessageThread thread, Long userId) {
        if (!ParticipantCsv.contains(thread.getParticipantUserIdsCsv(), userId)) {
            throw new AccessDeniedException("Not a participant of this thread");
        }
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

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
