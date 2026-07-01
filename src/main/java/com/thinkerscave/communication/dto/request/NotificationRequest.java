package com.thinkerscave.communication.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Schema(description = "Send a notification to users")
public class NotificationRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String body;
    private String category;

    @Schema(description = "Comma-separated channel codes: IN_APP, EMAIL, SMS, PUSH")
    private String channelsCsv = "IN_APP";

    private Instant scheduledAt;

    @NotEmpty(message = "At least one recipient user ID is required")
    private List<Long> recipientUserIds;
}
