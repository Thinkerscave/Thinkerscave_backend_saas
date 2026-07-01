package com.thinkerscave.admission.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Counseling note for a prospect inquiry")
public class CounselingNoteRequest {

    private String studentRequirements;
    private String parentConcerns;
    private String campusVisitInfo;
    private String recommendations;
    private String notes;
}
