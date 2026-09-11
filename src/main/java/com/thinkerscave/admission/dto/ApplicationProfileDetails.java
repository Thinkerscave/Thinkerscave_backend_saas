package com.thinkerscave.admission.dto;

import lombok.Data;

/**
 * Extra admission form fields stored as JSON on application_admission.profile_details.
 * Core identity/parent/class columns remain first-class on ApplicationAdmission.
 */
@Data
public class ApplicationProfileDetails {

    private String bloodGroup;
    private String religion;
    private String category;
    private String nationality;
    private String identityDocumentType;
    private String identityDocumentNumber;
    /** @deprecated prefer identityDocumentNumber */
    private String aadhaarNumber;
    private String motherTongue;
    private String placeOfBirth;

    private String parentRelationship;
    private String fatherOccupation;
    private String motherName;
    private String motherOccupation;
    private String motherContact;
    private String motherEmail;
    private String motherRelationship;
    private String secondaryGuardianName;
    private String secondaryGuardianRelationship;
    private String secondaryGuardianMobile;
    private String secondaryGuardianEmail;
    private String secondaryGuardianOccupation;
    private String secondaryIdentityDocumentType;
    private String secondaryIdentityDocumentNumber;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    private Boolean sameAsPresentAddress;
    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentCity;
    private String permanentState;
    private String permanentCountry;
    private String permanentPinCode;

    private String emergencyContactName;
    private String emergencyContactRelationship;
    private String emergencyContactMobile;
    private String emergencyContactAlternateMobile;

    private Boolean hasPreviousSchooling;
    private String previousSchoolName;
    private String previousBoard;
    private String previousClass;
    private String previousAcademicYear;
    private String lastPercentage;
    private String tcNumber;
    private String tcDate;
    private String mediumOfInstruction;
    private String firstLanguage;
    private String secondLanguage;
    private Long linkedParentId;
    private String siblingName;
}
