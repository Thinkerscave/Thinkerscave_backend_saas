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
public class OrganizationDetailResponse {

    private Long id;
    private String organizationCode;
    private String organizationName;
    private String shortName;
    private InstitutionType institutionType;
    private String boardName;
    private OrganizationStatus status;
    private String email;
    private String mobileNumber;
    private String alternateMobileNumber;
    private String website;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String timeZone;
    private String currency;
    private String language;
    private String logoUrl;
    private Boolean onboardingCompleted;
    private Boolean active;
    private String remarks;

    // customer summary
    private Long customerId;
    private String customerCode;
    private String customerName;

    // tenant info
    private TenantRegistryResponse tenant;

    // domain info
    private OrganizationDomainResponse domain;

    // active subscription
    private OrganizationSubscriptionResponse subscription;

    // configuration
    private OrganizationConfigurationResponse configuration;

    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
