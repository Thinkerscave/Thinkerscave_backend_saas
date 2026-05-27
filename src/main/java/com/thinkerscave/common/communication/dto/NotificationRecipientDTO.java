package com.thinkerscave.common.communication.dto;

import com.thinkerscave.common.communication.domain.NotificationChannel;
import com.thinkerscave.common.communication.domain.NotificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecipientDTO {
    private Long id;
    private Long notificationId;
    private Long userId;

    @NotBlank
    private String address;

    @NotNull
    private NotificationChannel channel;

    private NotificationStatus status;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private String failureReason;
    private Integer attemptCount;
    private String providerMessageId;
}
