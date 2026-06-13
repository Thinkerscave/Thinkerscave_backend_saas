package com.thinkerscave.student.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentCreateRequest {

    // ==========================
    // STUDENT DETAILS
    // ==========================

    @NotBlank
    private String admissionNumber;

    private String rollNumber;

    @NotBlank
    private String firstName;

    private String middleName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String gender;

    @NotNull
    private LocalDate dateOfBirth;

    private String religion;

    private String nationality;

    private String motherTongue;

    private String mobileNumber;

    @Email
    private String email;

    private String remarks;

    // ==========================
    // PARENT DETAILS
    // ==========================

    @NotBlank
    private String parentFirstName;

    private String parentMiddleName;

    @NotBlank
    private String parentLastName;

    private String parentGender;

    @NotBlank
    private String parentMobileNumber;

    @Email
    private String parentEmail;

    private String parentOccupation;

    private String parentOrganizationName;

    private String parentQualification;

    private Double annualIncome;

    // ==========================
    // ENROLLMENT DETAILS
    // ==========================

    @NotNull
    private Long academicYearId;

    @NotNull
    private Long classId;

    private Long sectionId;

    // ==========================
    // MEDICAL DETAILS
    // ==========================

    private String bloodGroup;

    private String allergies;

    private String medicalConditions;

    private String medications;

    private String doctorName;

    private String doctorContact;

    private String emergencyNotes;
}