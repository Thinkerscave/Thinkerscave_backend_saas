package com.thinkerscave.admission.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollApplicationRequest {

    @NotNull(message = "Academic year is required")
    private Long academicYearId;

    @NotNull(message = "Class is required")
    private Long classId;

    private Long sectionId;
}
