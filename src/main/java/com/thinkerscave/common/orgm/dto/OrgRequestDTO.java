package com.thinkerscave.common.orgm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgRequestDTO {

    // Optional for updates
    private String orgCode;

    // Owner/User details
    private String name;           // Full name (split inside service)
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

    // Optional flags
    private Boolean isActive;
    private Boolean isGroup;
}
