package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearPattern;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AcademicYearResponse {

    private Long academicYearId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private AcademicYearPattern pattern;
    private AcademicYearStatus status;
    private Boolean active;

    private Integer progressPercent;
    private Long daysCompleted;
    private Long daysRemaining;

    private YearStructureStats structureStats;
    private List<ReadinessStep> readinessSteps;
    private Integer readinessPercent;
    private Integer readinessCompletedSteps;
    private Integer readinessTotalSteps;

    private LocalDateTime submittedAt;
    private Long submittedByUserId;
    private LocalDateTime approvedAt;
    private Long approvedByUserId;
    private LocalDateTime rejectedAt;
    private Long rejectedByUserId;
    private String rejectionReason;
    private LocalDateTime activatedAt;
    private Long activatedByUserId;

    private String createdBy;
    private LocalDateTime createdOn;
    private String updatedBy;
    private LocalDateTime updatedOn;

    @Getter
    @Setter
    @Builder
    public static class YearStructureStats {
        private long classesTotal;
        private long classesActive;
        private long sectionsTotal;
        private long sectionsActive;
        private long subjectsTotal;
        private long subjectsActive;
        private long studentsActive;
    }

    @Getter
    @Setter
    @Builder
    public static class ReadinessStep {
        private String code;
        private String label;
        private String state; // COMPLETE | PENDING | IN_PROGRESS | NOT_STARTED
        private String detail;
    }
}
