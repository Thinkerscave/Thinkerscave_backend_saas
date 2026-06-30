package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk status update for users")
public class BulkUserStatusRequest {

    @NotEmpty(message = "User IDs are required")
    private List<Long> userIds;

    @NotBlank(message = "Action is required: ACTIVATE, DEACTIVATE, LOCK, UNLOCK")
    private String action;
}
