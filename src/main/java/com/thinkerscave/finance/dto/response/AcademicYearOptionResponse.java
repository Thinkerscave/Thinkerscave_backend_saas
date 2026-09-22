package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcademicYearOptionResponse {
    private Long academicYearId;
    private String name;
}
