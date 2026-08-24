package com.thinkerscave.admission.dto;

import lombok.Data;

/**
 * Extra Indian-school admission fields stored as JSON on application_admission.profile_details.
 * Not used for search; the core identity/parent/class columns remain first-class.
 */
@Data
public class ApplicationProfileDetails {

    private String bloodGroup;
    private String religion;
    private String category;
    private String nationality;
    private String aadhaarNumber;
    private String motherTongue;
    private String placeOfBirth;
    private String fatherOccupation;
    private String motherName;
    private String motherOccupation;
    private String city;
    private String state;
    private String pinCode;
    private String previousSchoolName;
    private String previousBoard;
    private String previousClass;
    private String lastPercentage;
    private String tcNumber;
    private String mediumOfInstruction;
    private String firstLanguage;
    private String siblingName;
}
