package com.thinkerscave.common.student.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentRequestDTO {

    // User
    private String firstName, middleName, lastName, email,mobileNumber;

    // Student
    private String gender,age, remarks,rollNumber;
    private LocalDate dateOfBirth, enrollmentDate;
    private Boolean isSameAddress;
    // Address
    private String currentCountry, currentState, currentCity, currentZipCode, currentAddressLine;
    private String permanentCountry, permanentState, permanentCity, permanentZipCode, permanentAddressLine;
    // Relations
    private Long classId, sectionId;
    // Guardian
    private String guardianFirstName, guardianMiddleName, guardianLastName;
    private String guardianRelation, guardianEmail, guardianPhoneNumber, guardianAddress;

}
