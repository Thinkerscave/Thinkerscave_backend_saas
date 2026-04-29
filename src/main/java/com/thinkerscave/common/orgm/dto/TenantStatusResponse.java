package com.thinkerscave.common.orgm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for tenant status queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStatusResponse {

    private String tenantId;

    private String tenantName;

    private Boolean isActive;

    private String subdomain;

    private Integer currentUserCount;

    private Integer maxUsers;

    private Long storageUsedMb;

    private Integer storageLimitMb;

    private LocalDateTime createdAt;

    private LocalDateTime lastAccessedAt;

    private Map<String, Object> features;

    private String healthStatus; // HEALTHY, WARNING, CRITICAL
}
