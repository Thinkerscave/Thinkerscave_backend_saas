package com.thinkerscave.common.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    private Long id;
    private Long messageThreadId;

    @NotNull
    private Long senderUserId;

    @NotBlank
    private String body;

    private String attachmentUrl;
    private Instant sentAt;
    private Instant editedAt;
    private boolean deleted;
}
