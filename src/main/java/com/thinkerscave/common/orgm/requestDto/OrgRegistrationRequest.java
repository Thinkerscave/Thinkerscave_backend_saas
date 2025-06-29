package com.thinkerscave.common.orgm.requestDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgRegistrationRequest {

    // Owner/User details
    private String name;           // Full name (can split inside service)
    private String gender;
    private Long phoneNumber;
    private String address;
    private String mailId;

    // Organization details
    private String organizationName;
    private String brandName;
    private String orgType;
    private String city;
    private String state;
}
