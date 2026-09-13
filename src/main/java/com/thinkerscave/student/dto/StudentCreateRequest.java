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

    private String category;

    private String placeOfBirth;

    private String identityDocumentType;

    private String identityDocumentNumber;

    private String mobileNumber;

    @Email
    private String email;

    private String remarks;

    /** When set, links this student back to the originating admission application. */
    private Long applicationId;

    // ==========================
    // PRIMARY PARENT / GUARDIAN
    // ==========================

    private Long existingParentId;

    /** FATHER / MOTHER / GUARDIAN / … — defaults to FATHER when blank. */
    private String parentRelationship;

    @NotBlank
    private String parentFirstName;

    private String parentMiddleName;

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
    // SECONDARY PARENT / GUARDIAN (optional)
    // ==========================

    private String secondaryParentRelationship;

    private String secondaryParentFirstName;

    private String secondaryParentLastName;

    private String secondaryParentMobileNumber;

    @Email
    private String secondaryParentEmail;

    private String secondaryParentOccupation;

    // ==========================
    // ADDRESS
    // ==========================

    private Boolean sameAddress;

    private String currentAddressLine1;

    private String currentAddressLine2;

    private String currentCity;

    private String currentState;

    private String currentCountry;

    private String currentPostalCode;

    private String permanentAddressLine1;

    private String permanentAddressLine2;

    private String permanentCity;

    private String permanentState;

    private String permanentCountry;

    private String permanentPostalCode;

    // ==========================
    // ENROLLMENT (optional for onboarding/import)
    // ==========================

    private Long academicYearId;

    private Long classId;

    private Long sectionId;

    private LocalDate enrollmentDate;

    private String enrollmentStatus;

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

    // ==========================
    // EMERGENCY CONTACT (optional long-term profile)
    // ==========================

    private String emergencyContactName;

    private String emergencyContactPhone;

    private String emergencyContactRelation;
}
