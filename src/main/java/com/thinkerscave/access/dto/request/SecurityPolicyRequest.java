package com.thinkerscave.access.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization security policy settings")
public class SecurityPolicyRequest {

    @Min(value = 6, message = "Minimum password length is 6")
    @Max(value = 128, message = "Maximum password length is 128")
    private Integer minPasswordLength = 8;

    private Boolean requireUppercase = true;
    private Boolean requireLowercase = true;
    private Boolean requireNumbers = true;
    private Boolean requireSpecialChars = false;

    @Min(0) @Max(365)
    @Schema(description = "0 = passwords never expire")
    private Integer passwordExpiryDays = 90;

    @Min(0) @Max(20)
    @Schema(description = "Number of previous passwords to remember")
    private Integer passwordHistoryCount = 5;

    @Min(1) @Max(20)
    private Integer maxFailedAttempts = 5;

    @Min(1) @Max(1440)
    @Schema(description = "Minutes to lock account after max failed attempts")
    private Integer lockoutDurationMinutes = 30;

    @Min(5) @Max(480)
    @Schema(description = "JWT session timeout in minutes")
    private Integer sessionTimeoutMinutes = 60;

    @Min(1) @Max(10)
    private Integer maxConcurrentSessions = 3;

    private Boolean allowRememberMe = false;
    private Boolean requireTwoFactor = false;
}
