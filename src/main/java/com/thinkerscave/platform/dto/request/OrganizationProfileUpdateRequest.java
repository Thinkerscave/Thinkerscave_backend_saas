package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Self-service Organization Profile update payload for Organization Admin/Owner.
 * Deliberately has no {@code institutionType} field — Institution Type is not editable
 * through this screen (set during platform provisioning only).
 */
@Getter
@Setter
public class OrganizationProfileUpdateRequest {

    @NotBlank
    @Size(max = 200)
    private String organizationName;

    @Size(max = 100)
    private String shortName;

    @Size(max = 100)
    private String boardName;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 20)
    private String mobileNumber;

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

    @Size(max = 500)
    private String logoUrl;
}
