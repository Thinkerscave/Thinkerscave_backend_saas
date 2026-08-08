package com.thinkerscave.student.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkerscave.student.enums.PromotionDecision;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PromotionRecordResponse {

    @JsonProperty("id")
    private Long id;

    private Long batchId;
    private Long studentId;
    private Long fromEnrollmentId;
    private Long toEnrollmentId;
    private Long fromClassId;
    private Long toClassId;
    private PromotionDecision decision;
    private String reason;
}
