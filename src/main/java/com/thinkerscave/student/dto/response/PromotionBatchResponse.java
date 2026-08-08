package com.thinkerscave.student.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thinkerscave.student.enums.PromotionBatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PromotionBatchResponse {

    @JsonProperty("id")
    private Long id;

    private String batchCode;
    private String batchNumber;
    private Long fromAcademicYearId;
    private Long toAcademicYearId;
    private PromotionBatchStatus status;
    private Integer plannedCount;
    private Integer processedCount;
    private LocalDateTime executedOn;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
