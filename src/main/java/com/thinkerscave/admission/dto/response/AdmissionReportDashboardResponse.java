package com.thinkerscave.admission.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AdmissionReportDashboardResponse {

    private Instant generatedAt;
    private AdmissionReportKpis kpis;
    private List<NamedCount> funnel;
    private TrendSeries trend;
    private List<SourceBreakdownRow> leadsBySource;
    private List<NamedCount> leadsByStatus;
    private List<NamedCount> applicationsByClass;
    private List<CounselorPerformanceRow> counselorPerformance;
    private FollowUpHealth followUpHealth;
    private List<NamedCount> applicationStatus;
    private List<NamedCount> documentVerification;
    private boolean lostReasonAnalysisSupported;
    private List<RecentApplicationRow> recentApplications;

    @Getter
    @Builder
    public static class AdmissionReportKpis {
        private long totalInquiries;
        private long totalLeads;
        private long applicationsStarted;
        private long applicationsSubmitted;
        private long applicationsApproved;
        private long enrolledStudents;
        private double leadToEnrollmentConversionRate;
        private long pendingActions;
        private Double totalInquiriesDeltaPct;
        private Double totalLeadsDeltaPct;
        private Double applicationsStartedDeltaPct;
        private Double applicationsSubmittedDeltaPct;
        private Double applicationsApprovedDeltaPct;
        private Double enrolledStudentsDeltaPct;
        private Double conversionDeltaPts;
        private Double pendingActionsDeltaPct;
    }

    @Getter
    @Builder
    public static class NamedCount {
        private String key;
        private String label;
        private long count;
        private Double percentOfTotal;
    }

    @Getter
    @Builder
    public static class SourceBreakdownRow {
        private String key;
        private String label;
        private long leads;
        private long applications;
        private long enrolled;
        private double conversionRate;
        private Double percentOfLeads;
    }

    @Getter
    @Builder
    public static class CounselorPerformanceRow {
        private Long counselorId;
        private String counselorName;
        private long leads;
        private long applications;
        private long enrolled;
        private double conversionRate;
        private long overdueFollowUps;
        private long dueTodayFollowUps;
    }

    @Getter
    @Builder
    public static class FollowUpHealth {
        private long dueToday;
        private long overdue;
        private long upcoming;
        private long completed;
        private long noFollowUp;
    }

    @Getter
    @Builder
    public static class TrendSeries {
        private String granularity;
        private List<String> labels;
        private List<Long> inquiries;
        private List<Long> leads;
        private List<Long> applications;
        private List<Long> approved;
        private List<Long> enrolled;
    }

    @Getter
    @Builder
    public static class RecentApplicationRow {
        private Long applicationId;
        private String applicantName;
        private String applyingForClass;
        private String source;
        private String counselorName;
        private String applicationDate;
        private String status;
        private long documentsUploaded;
        private long documentsVerified;
        private Long inquiryId;
    }
}
