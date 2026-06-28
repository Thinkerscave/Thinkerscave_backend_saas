package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrganizationSummaryResponse {

    private Long id;
    private String organizationCode;
    private String organizationName;
    private String shortName;
    private InstitutionType institutionType;
    private OrganizationStatus status;
    private String email;
    private String mobileNumber;
    private String city;
    private String state;
    private String country;
    private String logoUrl;
    private Boolean onboardingCompleted;
    private Boolean active;
    private String tenantIdentifier;
    private LocalDateTime createdOn;
}
