package com.thinkerscave.common.orgm.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class OrganisationListDTO {
    private Long orgId;
    private String orgCode;
    private String orgName;
    private String brandName;
    private String orgUrl;
    private String orgType;
    private String city;
    private String state;
    private LocalDate establishDate;

    // Flattened data from related entities
    private String ownerName;
    private String ownerEmail;
    private String ownerMobile;
    private String ownerCode;

    // Additional fields for UI logic
    private boolean isGroup;
    private Long parentOrgId;
    private Boolean isActive;
    private String tenantId; // Schema name
}
