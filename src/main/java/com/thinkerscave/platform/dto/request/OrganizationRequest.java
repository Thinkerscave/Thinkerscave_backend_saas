package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.InstitutionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationRequest {

    @NotNull
    private Long customerId;

    @NotBlank
    @Size(max = 200)
    private String organizationName;

    @Size(max = 100)
    private String shortName;

    @NotNull
    private InstitutionType institutionType;

    @Size(max = 100)
    private String boardName;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String mobileNumber;

    @Size(max = 200)
    private String adminFullName;

    @Size(max = 20)
    private String alternateMobileNumber;

    @Size(max = 255)
    private String website;

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

    @Size(max = 100)
    private String timeZone;

    @Size(max = 20)
    private String currency;

    @Size(max = 50)
    private String language;

    @Size(max = 350000)
    private String logoUrl;

    @Size(max = 1000)
    private String remarks;
}
