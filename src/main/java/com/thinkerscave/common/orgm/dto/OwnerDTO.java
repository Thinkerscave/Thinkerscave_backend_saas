package com.thinkerscave.common.orgm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OwnerDTO {

    // Required to identify the owner record
    private String ownerCode;

    // Owner-specific fields
    private String ownerName;
    private String gender;
    private String mailId;

    // User-related fields
    private String userName;
    private String address;
    private Long phoneNumber;
    private String city;
    private String state;
}