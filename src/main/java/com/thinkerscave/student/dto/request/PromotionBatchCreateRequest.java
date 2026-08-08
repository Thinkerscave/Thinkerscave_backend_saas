package com.thinkerscave.student.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionBatchCreateRequest {

    @NotNull(message = "fromAcademicYearId is required")
    private Long fromAcademicYearId;

    @NotNull(message = "toAcademicYearId is required")
    private Long toAcademicYearId;

    private String batchCode;

    private String remarks;
}
