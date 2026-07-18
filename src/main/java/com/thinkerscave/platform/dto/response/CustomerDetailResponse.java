package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.CustomerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class CustomerDetailResponse {

    private Long id;
    private String customerCode;
    private String customerName;
    private String businessEmail;
    private String mobileNumber;
    private String alternateMobileNumber;
    private String notes;
    private CustomerStatus status;
    private Long ownerUserId;
    private Boolean active;
    private CustomerContactResponse primaryContact;
    private CustomerContactResponse secondaryContact;
    private List<CustomerContactResponse> contacts;
    private List<OrganizationSummaryResponse> organizations;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
