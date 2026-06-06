package com.thinkerscave.common.admission.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CounselingNoteRequest {
    private String studentRequirements;
    private String parentConcerns;
    private String campusVisitInfo;
    private String recommendations;
    private String notes;
}
