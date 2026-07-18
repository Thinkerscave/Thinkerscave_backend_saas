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
public class CustomerResponse {

    private Long id;
    private String customerCode;
    private String customerName;
    private String businessEmail;
    private String mobileNumber;
    private String alternateMobileNumber;
    private String notes;
    private CustomerStatus status;
    private Long ownerUserId;
    /** Present only on create — owner login username (typically business email). */
    private String ownerUsername;
    /** Present only on create — one-time temporary password for the owner account. */
    private String temporaryPassword;
    private Boolean active;
    private Long organizationCount;
    private CustomerContactResponse primaryContact;
    private CustomerContactResponse secondaryContact;
    private List<CustomerContactResponse> contacts;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
