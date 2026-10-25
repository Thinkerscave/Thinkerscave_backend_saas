package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SectionTeacherAssignmentResponse {
    private Long sectionTeacherAssignmentId;
    private Long sectionId;
    private String sectionName;
    private Long academicGroupId;
    private String academicGroupName;
    private Long staffId;
    private String staffName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private LocalDateTime createdOn;
}
