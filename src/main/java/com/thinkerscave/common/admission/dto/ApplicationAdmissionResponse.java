package com.thinkerscave.common.admission.dto;

import com.thinkerscave.common.admission.domain.ApplicationStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the complete response details of an admission application.
 *
 * @apiNote This DTO encapsulates all relevant information about an application, including applicant details,
 * contact information, status, and internal comments, for use in API responses.
 *
 * @author Bibekananda Pradhan
 * @since 2023-06-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAdmissionResponse {
    private Long id;
    private String applicationId;
    private String applicantName;
    private LocalDateTime dateOfBirth;
    private String gender;
    private String applyingForSchoolOrCollege;
    private String parentName;
    private String guardianName;
    private String contactNumber;
    private String email;

    // --- MODIFICATION START ---

    // Remove the old flat fields
    // private String address;
    // private String city;
    // private String state;
    // private String pinCode;
    // private String emergencyContact;

    // Add nested DTOs
    private AddressDto address;
    private EmergencyContactDto emergencyContact;

    // --- MODIFICATION END ---

    private List<String> uploadedDocuments;
    private ApplicationStatus status;
    private String internalComments;


}