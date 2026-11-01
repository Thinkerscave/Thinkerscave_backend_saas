package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TeacherCoveragePreviewResponse {
    private boolean valid;
    private boolean requiresConfirmation;
    private List<AffectedSection> affectedSections;
    private List<WorkloadWarning> workloadWarnings;
    private List<Impact> impacts;

    @Getter
    @Setter
    @Builder
    public static class AffectedSection {
        private Long mappingId;
        private String subjectName;
        private Long sectionId;
        private String sectionName;
        private String fromTeacher;
        private String toTeacher;
    }

    @Getter
    @Setter
    @Builder
    public static class WorkloadWarning {
        private Long staffId;
        private String staffName;
        private int projectedPeriods;
        private int maxPeriods;
    }

    @Getter
    @Setter
    @Builder
    public static class Impact {
        private Long mappingId;
        private String subjectName;
        /** SECTION_OVERRIDES_KEPT or TIMETABLE_LESSONS. */
        private String type;
        private long count;
        private String message;
    }
}
