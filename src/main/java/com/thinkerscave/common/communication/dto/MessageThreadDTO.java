package com.thinkerscave.common.communication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageThreadDTO {
    private Long id;
    private String subject;

    @NotBlank
    private String participantUserIdsCsv;

    private String contextRef;
    private Instant lastMessageAt;
    private Long lastMessageByUserId;
    private boolean closed;
}
