package com.thinkerscave.communication.service;

import com.thinkerscave.communication.dto.request.MessageRequest;
import com.thinkerscave.communication.dto.request.MessageThreadRequest;
import com.thinkerscave.communication.dto.response.MessageResponse;
import com.thinkerscave.communication.dto.response.MessageThreadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    MessageThreadResponse createThread(MessageThreadRequest request);

    /** Lists open threads for the authenticated user (identity from security context). */
    Page<MessageThreadResponse> getMyThreads(Pageable pageable);

    /** Sends a message as the authenticated user; caller must be a participant. */
    MessageResponse sendMessage(Long threadId, MessageRequest request);

    /** Lists messages; caller must be a participant of the thread. */
    Page<MessageResponse> getMessages(Long threadId, Pageable pageable);

    /** Closes a thread in the caller's organization (admin/staff roles via controller). */
    void closeThread(Long threadId);
}
