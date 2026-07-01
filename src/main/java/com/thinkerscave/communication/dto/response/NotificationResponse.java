package com.thinkerscave.communication.dto.response;

import com.thinkerscave.communication.enums.NotificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Notification response")
public class NotificationResponse {

    private Long notificationId;
    private String subject;
    private String body;
    private String category;
    private String channelsCsv;
    private Instant scheduledAt;
    private Instant sentAt;
    private NotificationStatus status;
    private Integer totalRecipients;
    private Integer deliveredCount;
    private Integer failedCount;
    private LocalDateTime createdOn;
    private String createdBy;
}
