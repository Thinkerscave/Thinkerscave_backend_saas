package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.InstitutionType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Self-service profile view for an Organization Admin/Owner — excludes platform-only fields. */
@Getter
@Setter
@Builder
public class OrganizationProfileResponse {

    private Long id;
    private String organizationCode;
    private String organizationName;
    private String shortName;
    private InstitutionType institutionType;
    private String boardName;
    private String email;
    private String mobileNumber;
    private String alternateMobileNumber;
    private String website;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String logoUrl;
}
