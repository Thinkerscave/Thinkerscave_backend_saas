package com.thinkerscave.finance.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerationRunRequest {
    @NotNull
    private Long academicYearId;
    private Long classId;
}
