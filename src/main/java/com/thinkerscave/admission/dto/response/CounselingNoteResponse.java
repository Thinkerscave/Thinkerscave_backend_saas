package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.enums.FollowUpType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Counseling session response")
public class CounselingNoteResponse {

    private Long noteId;
    private Long inquiryId;
    private LocalDateTime sessionAt;
    private FollowUpType mode;
    private Long counselorStaffId;
    private String counselorName;
    private String studentRequirements;
    private String parentConcerns;
    private String campusVisitInfo;
    private String recommendations;
    private String notes;
    private LocalDateTime createdOn;
    private String createdBy;
}
