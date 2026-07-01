package com.thinkerscave.communication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Message thread summary")
public class MessageThreadResponse {

    private Long threadId;
    private String subject;
    private String participantUserIdsCsv;
    private Instant lastMessageAt;
    private boolean closed;
    private LocalDateTime createdOn;
    private String createdBy;
}
