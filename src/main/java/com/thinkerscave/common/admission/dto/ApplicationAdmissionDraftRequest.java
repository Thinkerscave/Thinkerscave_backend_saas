package com.thinkerscave.common.admission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class ApplicationAdmissionDraftRequest {

    private String applicationId;// Used to identify and update an existing draft


    // --- Fields moved from nested objects to the top level ---
    private String applicantName;
    private LocalDateTime dateOfBirth;
    private String gender;
    private String applyingForSchoolOrCollege;
    private String parentName;
    private String guardianName;
    private String contactNumber;
    private String email;

    // --- Nested objects are kept as they match the final structure ---
    private AddressDto address;
    private EmergencyContactDto emergencyContact;

    /**
     * For a draft, we only need the names of the files that have been selected,
     * not the files themselves.
     */
    private List<String> uploadedDocuments;

    // --- Nested DTO definitions for Address and Emergency Contact ---
    // These remain as they are part of the desired final structure.
    @Data
    @NoArgsConstructor
    public static class AddressDto {
        private String street;
        private String city;
        private String state;
        private String pincode;
    }

    @Data
    @NoArgsConstructor
    public static class EmergencyContactDto {
        private String name;
        private String number;
    }
}
