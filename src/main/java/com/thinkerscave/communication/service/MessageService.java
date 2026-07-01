package com.thinkerscave.communication.service;

import com.thinkerscave.communication.dto.request.MessageRequest;
import com.thinkerscave.communication.dto.request.MessageThreadRequest;
import com.thinkerscave.communication.dto.response.MessageResponse;
import com.thinkerscave.communication.dto.response.MessageThreadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    MessageThreadResponse createThread(MessageThreadRequest request);

    Page<MessageThreadResponse> getMyThreads(Long userId, Pageable pageable);

    MessageResponse sendMessage(Long threadId, MessageRequest request, Long senderUserId);

    Page<MessageResponse> getMessages(Long threadId, Pageable pageable);

    void closeThread(Long threadId);
}
