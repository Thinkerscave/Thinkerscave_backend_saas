package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DomainAvailabilityResponse {

    private String subdomain;
    private String tenantIdentifier;
    private String previewDomain;
    private boolean available;
    private String message;
}
