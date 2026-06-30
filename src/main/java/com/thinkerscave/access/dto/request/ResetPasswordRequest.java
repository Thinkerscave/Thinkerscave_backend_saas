package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin-triggered password reset (sets a temporary password)")
public class ResetPasswordRequest {

    @NotNull(message = "User ID is required")
    @Schema(description = "Target user ID")
    private Long userId;

    @Schema(description = "Send new credentials via email")
    private Boolean sendEmail = true;
}
