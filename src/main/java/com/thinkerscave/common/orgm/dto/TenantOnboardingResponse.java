package com.thinkerscave.common.orgm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for tenant onboarding operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantOnboardingResponse {

    private String tenantId;

    private String tenantName;

    private String displayName;

    private String adminUsername;

    private String adminEmail;

    private String subdomainUrl; // e.g., "https://tenant.thinkerscave.com"

    private String status; // ACTIVE, PENDING, FAILED, INACTIVE

    private LocalDateTime createdAt;

    private String message;

    private Integer maxUsers;

    private Integer storageLimitMb;
}
