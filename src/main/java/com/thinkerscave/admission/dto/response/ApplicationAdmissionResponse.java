package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.dto.ApplicationProfileDetails;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.FeePaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
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
    private Long academicYearId;
    private Long classId;
    private Long sectionId;
    private Long studentId;
    private String studentCode;
    private String admissionNumber;
    private String email;
    private String contactNumber;
    private String address;
    private String parentName;
    private String parentContact;
    private String parentEmail;
    private ApplicationProfileDetails profile;
    private ApplicationStatus status;
    private String internalComments;
    private List<String> uploadedDocuments;
    private List<ApplicationDocumentResponse> documents;
    private BigDecimal feeAmount;
    private String feeReceiptNumber;
    private String feePaymentMode;
    private LocalDate feePaidOn;
    private String feeReceivedBy;
    private String feeRemarks;
    private FeePaymentStatus feeStatus;
    private Long reviewedByUserId;
    private LocalDate reviewedOn;
    private boolean archived;
    private LocalDateTime createdOn;
    private String createdBy;
}
