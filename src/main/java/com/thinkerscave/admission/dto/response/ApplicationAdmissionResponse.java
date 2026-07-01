package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Application admission response")
public class ApplicationAdmissionResponse {

    private Long applicationId;
    private String applicationNumber;
    private Long inquiryId;
    private String applicantName;
    private LocalDate dateOfBirth;
    private String gender;
    private String applyingForClass;
    private String email;
    private String contactNumber;
    private String address;
    private String parentName;
    private String parentContact;
    private String parentEmail;
    private ApplicationStatus status;
    private String internalComments;
    private List<String> uploadedDocuments;
    private LocalDateTime createdOn;
    private String createdBy;
}
