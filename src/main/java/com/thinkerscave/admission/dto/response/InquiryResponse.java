package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.enums.LeadSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Inquiry record for a prospect")
public class InquiryResponse {

    private Long inquiryId;
    private String inquiryNumber;
    private String name;
    private String studentName;
    private String parentContactName;
    private String mobileNumber;
    private String email;
    private String classInterestedIn;
    private Long academicYearId;
    private Long classId;
    private String address;
    private LeadSource inquirySource;
    private String referredBy;
    private String comments;
    private Long assignedCounselorId;
    private String assignedCounselorName;
    private InquiryStatus status;
    private LocalDateTime lastFollowUpDate;
    private FollowUpType lastFollowUpType;
    private LocalDate nextFollowUpDate;
    private LocalDateTime createdOn;
    private String createdBy;

    // ─── Progressive enrichment ────────────────────────────────────────────────
    private LocalDate dateOfBirth;
    private String gender;
    private String currentClass;
    private String previousSchool;
    private String alternateMobileNumber;
    private String contactRelationship;
    private String campusPreference;
    private String transportRequired;
    private String hostelRequired;
    private String otherRequirements;
}
