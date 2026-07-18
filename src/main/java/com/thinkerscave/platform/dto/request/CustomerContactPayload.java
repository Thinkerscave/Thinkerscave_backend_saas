package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerContactPayload {

    @Size(max = 100, message = "Contact name must be at most 100 characters")
    private String fullName;

    @Email(message = "Contact email is invalid")
    @Size(max = 150)
    private String email;

    @Size(max = 30, message = "Contact mobile is invalid")
    private String mobileNumber;

    @Size(max = 100, message = "Designation must be at most 100 characters")
    private String designation;
}
