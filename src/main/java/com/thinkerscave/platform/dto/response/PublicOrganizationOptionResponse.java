package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Minimal organization payload for the unauthenticated login org-select screen.
 * Intentionally omits contact, billing, and internal provisioning details.
 */
@Getter
@Setter
@Builder
public class PublicOrganizationOptionResponse {

    private Long id;
    private String name;
    private String tenantId;
    private String location;
    private String logoUrl;
    private String institutionType;
}
