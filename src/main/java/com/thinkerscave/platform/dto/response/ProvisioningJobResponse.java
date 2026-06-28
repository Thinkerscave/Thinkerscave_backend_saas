package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.ProvisionJobStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProvisioningJobResponse {

    private Long id;
    private String jobCode;
    private Long organizationId;
    private String organizationName;
    private Long tenantRegistryId;
    private Long templateId;
    private String templateName;
    private ProvisionJobStatus status;
    private String currentStep;
    private Integer progressPercentage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    private Integer retryCount;
    private String errorMessage;
    private String provisionedBy;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
}
