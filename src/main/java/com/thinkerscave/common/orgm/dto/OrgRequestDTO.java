package com.thinkerscave.common.orgm.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrgRequestDTO {

    // General Info
    private Boolean isAGroup;
    private Long parentOrgId; // We only need the ID of the parent

    // Organisation Details
    private String orgName;
    private String brandName;
    private String orgUrl;
    private String orgType;
    private String city;
    private String state;
    private LocalDate establishDate;
    private String subscriptionType;
    private String schemaName;

    // Owner Details
    private String ownerName;
    private String ownerEmail;
    private String ownerMobile;
}
