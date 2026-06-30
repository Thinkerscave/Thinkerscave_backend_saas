package com.thinkerscave.access.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization security policy")
public class SecurityPolicyResponse {

    private Long id;
    private Long organizationId;
    private Integer minPasswordLength;
    private Boolean requireUppercase;
    private Boolean requireLowercase;
    private Boolean requireNumbers;
    private Boolean requireSpecialChars;
    private Integer passwordExpiryDays;
    private Integer passwordHistoryCount;
    private Integer maxFailedAttempts;
    private Integer lockoutDurationMinutes;
    private Integer sessionTimeoutMinutes;
    private Integer maxConcurrentSessions;
    private Boolean allowRememberMe;
    private Boolean requireTwoFactor;
    private Boolean active;
}
