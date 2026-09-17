package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.OperationStatus;

import java.time.LocalDateTime;

public record ProvisioningStepResponse(
        String code,
        String label,
        Integer sequence,
        OperationStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage
) {
}
