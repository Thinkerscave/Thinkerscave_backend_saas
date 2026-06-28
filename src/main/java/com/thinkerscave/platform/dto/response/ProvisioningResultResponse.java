package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response returned when the provisioning workflow completes.
 */
@Getter
@Setter
@Builder
public class ProvisioningResultResponse {

    private Long organizationId;
    private String organizationCode;
    private String organizationName;
    private Long tenantId;
    private String tenantIdentifier;
    private String schemaName;
    private Long subscriptionId;
    private Long provisioningJobId;
    private String jobCode;
    private String adminEmail;
    private String defaultDomain;
    private String message;
}
