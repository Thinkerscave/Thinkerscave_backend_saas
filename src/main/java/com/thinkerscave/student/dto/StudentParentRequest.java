package com.thinkerscave.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentParentRequest {

    private Long existingParentId;
    private String relationship;
    private String firstName;
    private String middleName;
    private String lastName;
    private String mobileNumber;
    @Email
    private String email;
    private String occupation;
    private String organizationName;
    private String qualification;
    private Double annualIncome;
    private Boolean primaryContact;
    private Boolean receiveSms;
    private Boolean receiveEmail;
    private Boolean pickupAuthorized;

    public boolean hasIdentity() {
        return existingParentId != null || (firstName != null && !firstName.isBlank()
                && mobileNumber != null && !mobileNumber.isBlank());
    }
}