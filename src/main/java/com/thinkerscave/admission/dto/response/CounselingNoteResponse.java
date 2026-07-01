package com.thinkerscave.admission.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Counseling note response")
public class CounselingNoteResponse {

    private Long noteId;
    private Long inquiryId;
    private String studentRequirements;
    private String parentConcerns;
    private String campusVisitInfo;
    private String recommendations;
    private String notes;
    private LocalDateTime createdOn;
    private String createdBy;
}
