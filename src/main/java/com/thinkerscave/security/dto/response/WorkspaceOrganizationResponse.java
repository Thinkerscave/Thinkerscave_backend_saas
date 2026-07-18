package com.thinkerscave.security.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WorkspaceOrganizationResponse {

    private Long organizationId;
    private String organizationCode;
    private String organizationName;
    private String tenantId;
    private String domain;
    private String logoUrl;
    private boolean current;
}
