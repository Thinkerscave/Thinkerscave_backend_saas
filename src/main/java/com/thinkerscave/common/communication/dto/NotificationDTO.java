package com.thinkerscave.common.communication.dto;

import com.thinkerscave.common.communication.domain.NotificationStatus;
import com.thinkerscave.common.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;

    @NotBlank
    @Size(max = 256)
    private String subject;

    @NotBlank
    private String body;

    @NotBlank
    private String channelsCsv;

    private String category;
    private SeverityLevel severity;
    private Instant scheduledAt;
    private Instant sentAt;
    private NotificationStatus status;
    private Long triggeredByUserId;
    private String sourceRef;
    private Integer totalRecipients;
    private Integer deliveredCount;
    private Integer failedCount;
}
