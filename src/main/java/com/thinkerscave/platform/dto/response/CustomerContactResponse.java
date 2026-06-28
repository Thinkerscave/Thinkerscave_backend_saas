package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.ContactType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CustomerContactResponse {

    private Long id;
    private String contactCode;
    private Long customerId;
    private String fullName;
    private String designation;
    private ContactType contactType;
    private String email;
    private String mobileNumber;
    private String alternateMobileNumber;
    private String officePhone;
    private String department;
    private Boolean primaryContact;
    private Boolean billingContact;
    private Boolean technicalContact;
    private Boolean salesContact;
    private Boolean supportContact;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
}
