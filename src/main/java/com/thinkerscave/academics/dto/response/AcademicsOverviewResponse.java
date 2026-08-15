package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicsOverviewResponse {

    private YearHeader yearHeader;
    private StructureCounts structureCounts;
    private MappingSummary mapping;
    private AllocationSummary allocation;
    private TimetableSummary timetable;
    private List<ReadinessStep> readinessSteps;
    private List<OverviewAlert> alerts;
    private List<ClassCard> topClasses;
    private List<SubjectCard> topSubjects;
    private Integer setupCompletePercent;
    private List<ImportantDate> importantDates;
    private List<StudentsByClass> studentsByClass;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class YearHeader {
        private Long academicYearId;
        private String name;
        private AcademicYearStatus status;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean yearReadOnly;
        private int progressPercent;
        private Long daysCompleted;
        private Long daysRemaining;
        private Long totalDays;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StructureCounts {
        private long classesActive;
        private long classesTotal;
        private long sectionsActive;
        private long sectionsTotal;
        private long subjectsActive;
        private long subjectsTotal;
        private long studentsActive;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MappingSummary {
        private long subjectsMapped;
        private long subjectsTotal;
        private long pendingMappings;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AllocationSummary {
        private long assignedSlots;
        private long totalSlots;
        private long missingSlots;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TimetableSummary {
        private Integer publishedVersion;
        private Integer latestVersion;
        private TimetableGenerationStatus generationStatus;
        private TimetableStatus status;
        private long openBlockingConflicts;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReadinessStep {
        private String code;
        private String label;
        private String state;
        private String detail;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OverviewAlert {
        private String severity;
        private String code;
        private String message;
        private String route;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ClassCard {
        private Long classId;
        private String className;
        private long sectionCount;
        private Long studentCount;
        private String classTeacherName;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubjectCard {
        private Long subjectId;
        private String subjectName;
        private String code;
        private String category;
        private Long mappedClassCount;
        private Short defaultWeeklyPeriods;
        private String teacherStatus;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ImportantDate {
        private String label;
        private LocalDate date;
        private String endDate;
        private String code;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudentsByClass {
        private Long classId;
        private String className;
        private long studentCount;
        private double percent;
    }
}
