package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.CustomerType;
import com.thinkerscave.platform.enums.PreferredCommunication;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CustomerResponse {

    private Long id;
    private String customerCode;
    private String legalName;
    private String displayName;
    private CustomerType customerType;
    private CustomerStatus status;
    private String email;
    private String mobileNumber;
    private String alternateMobileNumber;
    private String website;
    private String taxNumber;
    private String registrationNumber;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String logoUrl;
    private PreferredCommunication preferredCommunication;
    private Boolean onboardingCompleted;
    private Boolean active;
    private String remarks;
    private long organizationCount;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
