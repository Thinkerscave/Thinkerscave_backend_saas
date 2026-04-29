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
    private AddressDto address; // Use a nested object
    private EmergencyContactDto emergencyContact; // Use a nested object
    private List<String> uploadedDocuments;
    private ApplicationStatus status;
    private String internalComments;
    public String getApplicantName() { return applicantName; }
    public java.time.LocalDateTime getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getApplyingForSchoolOrCollege() { return applyingForSchoolOrCollege; }
    public String getParentName() { return parentName; }
    public String getGuardianName() { return guardianName; }
    public String getContactNumber() { return contactNumber; }
    public String getEmail() { return email; }
    public AddressDto getAddress() { return address; }
    public EmergencyContactDto getEmergencyContact() { return emergencyContact; }
    public java.util.List<String> getUploadedDocuments() { return uploadedDocuments; }
    public com.thinkerscave.common.admission.domain.ApplicationStatus getStatus() { return status; }
    public String getInternalComments() { return internalComments; }

}
