package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TimetableDashboardResponse {

    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearReadOnly;

    private List<ReadinessItem> readinessChecks;
    private String overallStatus;
    private boolean canGenerate;

    private TimetableConfigurationResponse configurationSummary;
    private TimetableVersionResponse currentVersion;
    private TimetableVersionResponse latestVersion;

    private long totalConflicts;
    private long openBlockingConflicts;

    @Getter
    @Setter
    @Builder
    public static class ReadinessItem {
        private String key;
        private String label;
        private String status;
        private String message;
        private boolean blocking;
    }
}
