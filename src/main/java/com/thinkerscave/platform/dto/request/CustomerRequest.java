package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.CustomerType;
import com.thinkerscave.platform.enums.PreferredCommunication;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank
    @Size(max = 200)
    private String legalName;

    @NotBlank
    @Size(max = 200)
    private String displayName;

    @NotNull
    private CustomerType customerType;

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(max = 20)
    private String mobileNumber;

    @Size(max = 20)
    private String alternateMobileNumber;

    @Size(max = 255)
    private String website;

    @Size(max = 100)
    private String taxNumber;

    @Size(max = 100)
    private String registrationNumber;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String country;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 500)
    private String logoUrl;

    private PreferredCommunication preferredCommunication;

    private CustomerStatus status;

    @Size(max = 1000)
    private String remarks;
}
