package com.thinkerscave.communication.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(description = "Message response")
public class MessageResponse {

    private Long messageId;
    private Long threadId;
    private Long senderUserId;
    private String body;
    private String attachmentUrl;
    private Instant sentAt;
}
