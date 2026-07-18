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
    private ContactType contactType;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String designation;
    private Boolean active;
    private LocalDateTime createdOn;
    private String createdBy;
}
