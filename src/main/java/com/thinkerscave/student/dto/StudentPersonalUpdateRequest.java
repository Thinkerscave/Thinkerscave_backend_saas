package com.thinkerscave.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentPersonalUpdateRequest {

    @NotBlank
    private String firstName;
    private String middleName;
    @NotBlank
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private String mobile;
    private String email;
    private String currentAddressLine1;
    private String currentAddressLine2;
    private String currentLandmark;
    private String currentCity;
    private String currentDistrict;
    private String currentState;
    private String currentCountry;
    private String currentPostalCode;
    private Boolean sameAddress;
    private String permanentAddressLine1;
    private String permanentAddressLine2;
    private String permanentLandmark;
    private String permanentCity;
    private String permanentDistrict;
    private String permanentState;
    private String permanentCountry;
    private String permanentPostalCode;
}
