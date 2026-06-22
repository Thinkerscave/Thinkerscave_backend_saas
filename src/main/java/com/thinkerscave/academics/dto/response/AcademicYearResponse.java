package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AcademicYearResponse {
    private Long academicYearId;
    private String yearCode;
    private String yearName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentYear;
    private Boolean active;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdOn;
}
