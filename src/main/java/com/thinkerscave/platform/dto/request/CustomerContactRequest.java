package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerContactRequest {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @Size(max = 100)
    private String designation;

    @NotNull
    private ContactType contactType;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String mobileNumber;

    @Size(max = 20)
    private String alternateMobileNumber;

    @Size(max = 20)
    private String officePhone;

    @Size(max = 100)
    private String department;

    private Boolean primaryContact;

    private Boolean billingContact;

    private Boolean technicalContact;

    private Boolean salesContact;

    private Boolean supportContact;

    @Size(max = 1000)
    private String remarks;
}
