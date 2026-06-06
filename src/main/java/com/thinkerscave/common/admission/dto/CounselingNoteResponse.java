package com.thinkerscave.common.admission.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CounselingNoteResponse {
    private Long id;
    private Long inquiryId;
    private String studentRequirements;
    private String parentConcerns;
    private String campusVisitInfo;
    private String recommendations;
    private String notes;
    private String createdBy;
    private Instant createdAt;
}
