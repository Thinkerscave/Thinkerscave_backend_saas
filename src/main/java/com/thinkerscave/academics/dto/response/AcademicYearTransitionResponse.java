package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicTransitionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicYearTransitionResponse {

    private Long academicYearTransitionId;
    private Long sourceAcademicYearId;
    private String sourceAcademicYearName;
    private Long targetAcademicYearId;
    private String targetAcademicYearName;
    private AcademicTransitionStatus status;
    private Boolean copyClasses;
    private Boolean copySections;
    private Boolean copySubjects;
    private Boolean copyMappings;
    private Boolean copyAllocations;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime approvedAt;
    private Long approvedByUserId;
    private String failureReason;
}
