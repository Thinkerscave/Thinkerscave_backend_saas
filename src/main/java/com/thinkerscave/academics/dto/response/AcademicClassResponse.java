package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AcademicClassResponse {
    private Long classId;
    private Long academicYearId;
    private String yearCode;
    private String classCode;
    private String className;
    private String academicStage;
    private Integer displayOrder;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
}
