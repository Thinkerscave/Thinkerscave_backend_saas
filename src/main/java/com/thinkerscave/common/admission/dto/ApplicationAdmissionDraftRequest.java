package com.thinkerscave.common.admission.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class ApplicationAdmissionDraftRequest {

    private Long id; // Used to identify and update an existing draft

    private BasicInfoDto basicInfo;
    private ParentDetailsDto parentDetails;
    private AddressDto address;
    private EmergencyContactDto emergencyContact;
    private List<DocumentDto> documents;

    // Nested DTOs to match the Angular Reactive Form structure
    @Data
    @NoArgsConstructor
    public static class BasicInfoDto {
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("last_name") private String lastName;
        @JsonProperty("date_of_birth") private LocalDateTime dateOfBirth;
        private String gender;
        @JsonProperty("applying_for_school") private String applyingForSchool;
    }

    @Data @NoArgsConstructor public static class ParentDetailsDto {
        @JsonProperty("parent_name") private String parentName;
        @JsonProperty("guardian_name") private String guardianName;
        private String email;
        @JsonProperty("contact_number") private String contactNumber;
    }

    @Data @NoArgsConstructor public static class AddressDto {
        private String street;
        private String city;
        private String state;
        private String pincode;
    }

    @Data @NoArgsConstructor public static class EmergencyContactDto {
        private String name;
        private String number;
    }

    @Data @NoArgsConstructor public static class DocumentDto {
        private String fileName;
    }
}