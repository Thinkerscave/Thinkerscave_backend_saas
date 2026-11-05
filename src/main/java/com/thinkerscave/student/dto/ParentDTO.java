package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParentDTO {

    private Long parentId;

    private String parentCode;

    private String fullName;

    private String mobileNumber;

    private String email;

    private String occupation;

    private String organizationName;

    private String qualification;

    private String relationship;

    private Boolean primaryContact;

    private Boolean receiveSms;

    private Boolean receiveEmail;

    private Boolean pickupAuthorized;
}