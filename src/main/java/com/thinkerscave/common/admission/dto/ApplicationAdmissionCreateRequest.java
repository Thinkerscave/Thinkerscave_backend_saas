package com.thinkerscave.common.admission.dto;

import com.thinkerscave.common.admission.domain.ApplicationStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the data required to create a new admission application.
 * <p>
 * This DTO contains all necessary fields for creating an application, including applicant details,
 * contact information, and uploaded documents, to be provided in API requests.
 *
 * @author Bibekananda Pradhan
 * @since 2023-06-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAdmissionCreateRequest {
    private String applicationId;
    private String applicantName;
    private LocalDateTime dateOfBirth;
    private String gender;
    private String applyingForSchoolOrCollege;
    private String parentName;
    private String guardianName;
    private String contactNumber;
    private String email;
    private String address;
    private String city;
    private String state;
    private String pinCode;
    private String emergencyContact;
    private List<String> uploadedDocuments;
    private ApplicationStatus status;
    private String internalComments;
}
