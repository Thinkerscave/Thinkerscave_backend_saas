package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.TeacherWorkloadStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * View-only Teacher Workload dashboard for one academic year.
 *
 * <p>The expected target and maximum are derived from the persisted timetable schedule
 * patterns. When no valid teaching capacity exists both values are {@code null} and
 * {@link #targetAvailable} is {@code false}; the UI must show "Workload target unavailable".</p>
 */
@Getter
@Setter
@Builder
public class TeacherWorkloadDashboardResponse {

    private Long academicYearId;
    private String academicYearName;
    private String academicYearStatus;

    private boolean targetAvailable;
    private Integer benchmarkWeeklyCapacity;
    private Integer expectedTarget;
    private Integer maximumWeeklyCapacity;
    private Integer nearMaximumThreshold;
    private int bufferPercentage;
    private String targetBasis;
    private String targetExplanation;

    /** ALLOCATION_ESTIMATE — assigned periods come from persisted allocations, not the timetable grid. */
    private String workloadBasis;
    private String timetableStatus;
    private Long timetableVersionId;
    private Integer timetableVersionNumber;
    private String timetableVersionStatus;
    private int timetableConflictCount;

    private Summary summary;
    private List<Warning> warnings;
    private List<Row> rows;
    private List<FilterOption> subjectOptions;
    private List<FilterOption> academicGroupOptions;
    private List<UnassignedSubject> unassignedSubjects;

    @Getter
    @Setter
    @Builder
    public static class Summary {
        private int totalTeachers;
        private int overloaded;
        private int nearMaximum;
        private int atTarget;
        private int belowTarget;
        private int unassignedSubjects;
    }

    @Getter
    @Setter
    @Builder
    public static class Warning {
        private String code;
        private String severity;
        private String message;
    }

    @Getter
    @Setter
    @Builder
    public static class FilterOption {
        private Long id;
        private String label;
    }

    @Getter
    @Setter
    @Builder
    public static class Row {
        private Long staffId;
        private String staffName;
        private String employeeId;
        private String email;

        private int assignedPeriods;
        private Integer expectedTarget;
        private Integer remainingCapacity;
        private Integer maximumPeriods;
        private int overloadPeriods;
        private TeacherWorkloadStatus status;

        private String workloadBasis;
        private Integer scheduledPeriods;
        private int timetableConflicts;

        private List<String> subjects;
        private List<Breakdown> breakdown;
        private List<Conflict> conflicts;
    }

    @Getter
    @Setter
    @Builder
    public static class Breakdown {
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private Long academicGroupId;
        private String academicGroupName;
        private Long sectionId;
        private String sectionName;
        private int weeklyPeriods;
        /** ALLOCATION | CHOICE_GROUP | FIXED_ACTIVITY */
        private String source;
        private String choiceGroupName;
    }

    @Getter
    @Setter
    @Builder
    public static class Conflict {
        private Long conflictId;
        private String conflictType;
        private String message;
        private boolean blocking;
        private String status;
        private String dayOfWeek;
        private String periodName;
        private String sectionName;
    }

    @Getter
    @Setter
    @Builder
    public static class UnassignedSubject {
        private Long subjectId;
        private String subjectName;
        private Long academicGroupId;
        private String academicGroupName;
        private Long sectionId;
        private String sectionName;
        private int weeklyPeriods;
        private String reason;
    }
}
