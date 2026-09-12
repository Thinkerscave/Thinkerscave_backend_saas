package com.thinkerscave.admission.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.admission.dto.request.AdmissionReportFilterRequest;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse.AdmissionReportKpis;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse.CounselorPerformanceRow;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse.FollowUpHealth;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse.NamedCount;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse.RecentApplicationRow;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse.SourceBreakdownRow;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse.TrendSeries;
import com.thinkerscave.admission.entity.AdmissionApplicationDocument;
import com.thinkerscave.admission.entity.ApplicationAdmission;
import com.thinkerscave.admission.entity.Inquiry;
import com.thinkerscave.admission.entity.InquiryFollowUp;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.enums.LeadSource;
import com.thinkerscave.admission.repository.AdmissionApplicationDocumentRepository;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.InquiryFollowUpRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.admission.service.AdmissionReportService;
import com.thinkerscave.admission.specification.AdmissionReportSpecification;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionReportServiceImpl implements AdmissionReportService {

    private static final String RESOURCE_ADMISSIONS_REPORTS = "ADMISSIONS_REPORTS";
    private static final EnumSet<ApplicationStatus> SUBMITTED_PLUS = EnumSet.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.ACTION_REQUIRED,
            ApplicationStatus.DOCUMENTS_PENDING,
            ApplicationStatus.FEE_PENDING,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.CANCELLED,
            ApplicationStatus.ENROLLED
    );
    private static final EnumSet<ApplicationStatus> PENDING_APP = EnumSet.of(
            ApplicationStatus.ACTION_REQUIRED,
            ApplicationStatus.DOCUMENTS_PENDING,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.FEE_PENDING
    );

    private final InquiryRepository inquiryRepository;
    private final ApplicationAdmissionRepository applicationRepository;
    private final AdmissionApplicationDocumentRepository documentRepository;
    private final InquiryFollowUpRepository followUpRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    @Override
    public Map<String, Object> overview() {
        requireViewReports();
        AdmissionReportDashboardResponse dash = dashboard(new AdmissionReportFilterRequest());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalLeads", dash.getKpis().getTotalLeads());
        response.put("interestedLeads", countStatus(dash.getLeadsByStatus(), InquiryStatus.INTERESTED.name()));
        response.put("submittedApplications", dash.getKpis().getApplicationsSubmitted());
        response.put("approvedApplications", dash.getKpis().getApplicationsApproved());
        response.put("conversionRateInquiryToAdmission", dash.getKpis().getLeadToEnrollmentConversionRate());
        return response;
    }

    @Override
    public Map<String, Long> funnel() {
        requireViewReports();
        AdmissionReportDashboardResponse dash = dashboard(new AdmissionReportFilterRequest());
        Map<String, Long> funnel = new LinkedHashMap<>();
        for (NamedCount stage : dash.getFunnel()) {
            funnel.put(stage.getKey(), stage.getCount());
        }
        return funnel;
    }

    @Override
    public List<Map<String, Object>> counselorPerformance() {
        requireViewReports();
        return dashboard(new AdmissionReportFilterRequest()).getCounselorPerformance().stream()
                .map(row -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("counselorId", row.getCounselorId());
                    data.put("counselorName", row.getCounselorName());
                    data.put("leadCount", row.getLeads());
                    data.put("applications", row.getApplications());
                    data.put("enrolled", row.getEnrolled());
                    data.put("conversionRate", row.getConversionRate());
                    return data;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> sourceAnalysis() {
        requireViewReports();
        Map<String, Long> source = new LinkedHashMap<>();
        for (SourceBreakdownRow row : dashboard(new AdmissionReportFilterRequest()).getLeadsBySource()) {
            source.put(row.getLabel(), row.getLeads());
        }
        return source;
    }

    @Override
    public AdmissionReportDashboardResponse dashboard(AdmissionReportFilterRequest filter) {
        requireViewReports();
        AdmissionReportFilterRequest f = filter != null ? filter : new AdmissionReportFilterRequest();

        List<Inquiry> inquiries = inquiryRepository.findAll(AdmissionReportSpecification.inquiries(f));
        boolean leadFiltered = AdmissionReportSpecification.leadFiltersActive(f);
        Set<Long> inquiryIds = inquiries.stream().map(Inquiry::getInquiryId).collect(Collectors.toSet());
        List<ApplicationAdmission> applications = applicationRepository.findAll(
                AdmissionReportSpecification.applications(f, inquiryIds, leadFiltered));

        Map<Long, Inquiry> inquiryById = inquiries.stream()
                .collect(Collectors.toMap(Inquiry::getInquiryId, i -> i, (a, b) -> a));

        // When lead filters are inactive, still attach inquiry metadata for source/counselor display.
        if (!leadFiltered) {
            Set<Long> linked = applications.stream()
                    .map(ApplicationAdmission::getInquiryId)
                    .filter(Objects::nonNull)
                    .filter(id -> !inquiryById.containsKey(id))
                    .collect(Collectors.toSet());
            if (!linked.isEmpty()) {
                for (Inquiry extra : inquiryRepository.findAllById(linked)) {
                    if (!Boolean.TRUE.equals(extra.getDeleted())) {
                        inquiryById.put(extra.getInquiryId(), extra);
                    }
                }
            }
        }

        AdmissionReportKpis kpis = buildKpis(inquiries, applications, f);
        List<NamedCount> funnel = buildFunnel(inquiries, applications);
        TrendSeries trend = buildTrend(inquiries, applications, f);
        List<SourceBreakdownRow> bySource = buildSourceBreakdown(inquiries, applications, inquiryById);
        List<NamedCount> byLeadStatus = buildLeadStatus(inquiries);
        List<NamedCount> byClass = buildApplicationsByClass(applications);
        FollowUpHealth followUpHealth = buildFollowUpHealth(inquiries, f);
        List<CounselorPerformanceRow> counselors = buildCounselorPerformance(inquiries, applications, inquiryById, followUpHealth);
        List<NamedCount> appStatus = buildApplicationStatus(applications);
        List<NamedCount> docs = buildDocumentVerification(applications);
        List<RecentApplicationRow> recent = buildRecentApplications(applications, inquiryById, f);

        return AdmissionReportDashboardResponse.builder()
                .generatedAt(Instant.now())
                .kpis(kpis)
                .funnel(funnel)
                .trend(trend)
                .leadsBySource(bySource)
                .leadsByStatus(byLeadStatus)
                .applicationsByClass(byClass)
                .counselorPerformance(counselors)
                .followUpHealth(followUpHealth)
                .applicationStatus(appStatus)
                .documentVerification(docs)
                .lostReasonAnalysisSupported(false)
                .recentApplications(recent)
                .build();
    }

    @Override
    public byte[] exportCsv(AdmissionReportFilterRequest filter) {
        AdmissionReportDashboardResponse dash = dashboard(filter);
        StringBuilder sb = new StringBuilder();
        sb.append("Section,Key,Label,Value\n");
        AdmissionReportKpis k = dash.getKpis();
        appendCsv(sb, "KPI", "totalInquiries", "Total Inquiries", k.getTotalInquiries());
        appendCsv(sb, "KPI", "totalLeads", "Total Leads", k.getTotalLeads());
        appendCsv(sb, "KPI", "applicationsStarted", "Applications Started", k.getApplicationsStarted());
        appendCsv(sb, "KPI", "applicationsSubmitted", "Applications Submitted", k.getApplicationsSubmitted());
        appendCsv(sb, "KPI", "applicationsApproved", "Applications Approved", k.getApplicationsApproved());
        appendCsv(sb, "KPI", "enrolledStudents", "Enrolled Students", k.getEnrolledStudents());
        appendCsv(sb, "KPI", "conversionRate", "Lead to Enrollment Conversion %", k.getLeadToEnrollmentConversionRate());
        appendCsv(sb, "KPI", "pendingActions", "Pending Actions", k.getPendingActions());
        for (NamedCount row : dash.getFunnel()) {
            appendCsv(sb, "Funnel", row.getKey(), row.getLabel(), row.getCount());
        }
        for (SourceBreakdownRow row : dash.getLeadsBySource()) {
            appendCsv(sb, "SourceLeads", row.getKey(), row.getLabel(), row.getLeads());
            appendCsv(sb, "SourceApplications", row.getKey(), row.getLabel(), row.getApplications());
            appendCsv(sb, "SourceEnrolled", row.getKey(), row.getLabel(), row.getEnrolled());
        }
        for (NamedCount row : dash.getLeadsByStatus()) {
            appendCsv(sb, "LeadStatus", row.getKey(), row.getLabel(), row.getCount());
        }
        for (NamedCount row : dash.getApplicationsByClass()) {
            appendCsv(sb, "Class", row.getKey(), row.getLabel(), row.getCount());
        }
        for (CounselorPerformanceRow row : dash.getCounselorPerformance()) {
            appendCsv(sb, "CounselorLeads", String.valueOf(row.getCounselorId()), row.getCounselorName(), row.getLeads());
            appendCsv(sb, "CounselorEnrolled", String.valueOf(row.getCounselorId()), row.getCounselorName(), row.getEnrolled());
        }
        FollowUpHealth fu = dash.getFollowUpHealth();
        appendCsv(sb, "FollowUp", "dueToday", "Due Today", fu.getDueToday());
        appendCsv(sb, "FollowUp", "overdue", "Overdue", fu.getOverdue());
        appendCsv(sb, "FollowUp", "upcoming", "Upcoming", fu.getUpcoming());
        appendCsv(sb, "FollowUp", "completed", "Completed", fu.getCompleted());
        appendCsv(sb, "FollowUp", "noFollowUp", "No Follow-up", fu.getNoFollowUp());
        for (NamedCount row : dash.getApplicationStatus()) {
            appendCsv(sb, "ApplicationStatus", row.getKey(), row.getLabel(), row.getCount());
        }
        for (NamedCount row : dash.getDocumentVerification()) {
            appendCsv(sb, "Documents", row.getKey(), row.getLabel(), row.getCount());
        }
        sb.append("\nRecent Applications\n");
        sb.append("ApplicationId,Student,Class,Source,Counselor,Date,Status,DocsUploaded,DocsVerified\n");
        for (RecentApplicationRow row : dash.getRecentApplications()) {
            sb.append(csv(row.getApplicationId())).append(',')
                    .append(csv(row.getApplicantName())).append(',')
                    .append(csv(row.getApplyingForClass())).append(',')
                    .append(csv(row.getSource())).append(',')
                    .append(csv(row.getCounselorName())).append(',')
                    .append(csv(row.getApplicationDate())).append(',')
                    .append(csv(row.getStatus())).append(',')
                    .append(row.getDocumentsUploaded()).append(',')
                    .append(row.getDocumentsVerified()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private AdmissionReportKpis buildKpis(
            List<Inquiry> inquiries,
            List<ApplicationAdmission> applications,
            AdmissionReportFilterRequest filter) {
        long totalInquiries = inquiries.size();
        // Active pipeline leads (exclude LOST) — product "Total Leads"
        long totalLeads = inquiries.stream().filter(i -> i.getStatus() != InquiryStatus.LOST).count();
        long started = applications.size();
        long submitted = applications.stream().filter(a -> SUBMITTED_PLUS.contains(a.getStatus())).count();
        long approved = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
        long enrolled = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.ENROLLED).count();
        double conversion = totalLeads == 0 ? 0D : round1(enrolled * 100.0 / totalLeads);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        Set<Long> inquiryIdSet = inquiries.stream().map(Inquiry::getInquiryId).collect(Collectors.toSet());
        long overdueFu = countFollowUps(followUpRepository.findOverdueOrgWide(dayStart), inquiryIdSet, filter);
        long dueTodayFu = countFollowUps(followUpRepository.findTodayOrgWide(dayStart, dayEnd), inquiryIdSet, filter);
        long pendingApps = applications.stream().filter(a -> PENDING_APP.contains(a.getStatus())).count();
        long pendingActions = overdueFu + dueTodayFu + pendingApps;

        AdmissionReportFilterRequest prior = priorPeriodFilter(filter);
        Double inqDelta = null;
        Double leadDelta = null;
        Double startedDelta = null;
        Double submittedDelta = null;
        Double approvedDelta = null;
        Double enrolledDelta = null;
        Double conversionDelta = null;
        Double pendingDelta = null;
        if (prior != null) {
            List<Inquiry> priorInquiries = inquiryRepository.findAll(AdmissionReportSpecification.inquiries(prior));
            boolean leadFiltered = AdmissionReportSpecification.leadFiltersActive(prior);
            Set<Long> priorIds = priorInquiries.stream().map(Inquiry::getInquiryId).collect(Collectors.toSet());
            List<ApplicationAdmission> priorApps = applicationRepository.findAll(
                    AdmissionReportSpecification.applications(prior, priorIds, leadFiltered));
            long pInq = priorInquiries.size();
            long pLeads = priorInquiries.stream().filter(i -> i.getStatus() != InquiryStatus.LOST).count();
            long pStarted = priorApps.size();
            long pSubmitted = priorApps.stream().filter(a -> SUBMITTED_PLUS.contains(a.getStatus())).count();
            long pApproved = priorApps.stream().filter(a -> a.getStatus() == ApplicationStatus.APPROVED).count();
            long pEnrolled = priorApps.stream().filter(a -> a.getStatus() == ApplicationStatus.ENROLLED).count();
            double pConv = pLeads == 0 ? 0D : round1(pEnrolled * 100.0 / pLeads);
            Set<Long> priorIdSet = priorIds;
            long pOverdue = countFollowUps(followUpRepository.findOverdueOrgWide(dayStart), priorIdSet, prior);
            long pDue = countFollowUps(followUpRepository.findTodayOrgWide(dayStart, dayEnd), priorIdSet, prior);
            long pPendingApps = priorApps.stream().filter(a -> PENDING_APP.contains(a.getStatus())).count();
            long pPending = pOverdue + pDue + pPendingApps;
            inqDelta = pctChange(pInq, totalInquiries);
            leadDelta = pctChange(pLeads, totalLeads);
            startedDelta = pctChange(pStarted, started);
            submittedDelta = pctChange(pSubmitted, submitted);
            approvedDelta = pctChange(pApproved, approved);
            enrolledDelta = pctChange(pEnrolled, enrolled);
            conversionDelta = round1(conversion - pConv);
            pendingDelta = pctChange(pPending, pendingActions);
        }

        return AdmissionReportKpis.builder()
                .totalInquiries(totalInquiries)
                .totalLeads(totalLeads)
                .applicationsStarted(started)
                .applicationsSubmitted(submitted)
                .applicationsApproved(approved)
                .enrolledStudents(enrolled)
                .leadToEnrollmentConversionRate(conversion)
                .pendingActions(pendingActions)
                .totalInquiriesDeltaPct(inqDelta)
                .totalLeadsDeltaPct(leadDelta)
                .applicationsStartedDeltaPct(startedDelta)
                .applicationsSubmittedDeltaPct(submittedDelta)
                .applicationsApprovedDeltaPct(approvedDelta)
                .enrolledStudentsDeltaPct(enrolledDelta)
                .conversionDeltaPts(conversionDelta)
                .pendingActionsDeltaPct(pendingDelta)
                .build();
    }

    private List<NamedCount> buildFunnel(List<Inquiry> inquiries, List<ApplicationAdmission> applications) {
        long inquiriesCount = inquiries.size();
        long leads = inquiries.stream()
                .filter(i -> i.getStatus() != null && i.getStatus() != InquiryStatus.NEW)
                .count();
        long interested = inquiries.stream()
                .filter(i -> i.getStatus() == InquiryStatus.INTERESTED
                        || i.getStatus() == InquiryStatus.APPLICATION_STARTED
                        || i.getStatus() == InquiryStatus.APPLICATION_SUBMITTED)
                .count();
        long appStarted = inquiries.stream()
                .filter(i -> i.getStatus() == InquiryStatus.APPLICATION_STARTED
                        || i.getStatus() == InquiryStatus.APPLICATION_SUBMITTED)
                .count();
        if (appStarted == 0) {
            appStarted = applications.size();
        }
        long submitted = applications.stream().filter(a -> SUBMITTED_PLUS.contains(a.getStatus())).count();
        long approved = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.APPROVED || a.getStatus() == ApplicationStatus.ENROLLED)
                .count();
        long enrolled = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.ENROLLED).count();

        long base = Math.max(inquiriesCount, 1);
        List<NamedCount> stages = new ArrayList<>();
        stages.add(named("INQUIRIES", "Inquiries", inquiriesCount, pct(inquiriesCount, base)));
        stages.add(named("LEADS", "Leads", leads, pct(leads, base)));
        stages.add(named("INTERESTED", "Interested", interested, pct(interested, base)));
        stages.add(named("APPLICATION_STARTED", "Application Started", appStarted, pct(appStarted, base)));
        stages.add(named("SUBMITTED", "Submitted", submitted, pct(submitted, base)));
        stages.add(named("APPROVED", "Approved", approved, pct(approved, base)));
        stages.add(named("ENROLLED", "Enrolled", enrolled, pct(enrolled, base)));
        return stages;
    }

    private TrendSeries buildTrend(
            List<Inquiry> inquiries,
            List<ApplicationAdmission> applications,
            AdmissionReportFilterRequest filter) {
        boolean weekly = filter.getTrendGranularity() != null
                && filter.getTrendGranularity().equalsIgnoreCase("WEEKLY");
        LocalDate end = filter.getDateTo() != null ? filter.getDateTo() : LocalDate.now();
        LocalDate start = filter.getDateFrom() != null
                ? filter.getDateFrom()
                : (weekly ? end.minusWeeks(11) : end.minusMonths(5).withDayOfMonth(1));

        List<String> labels = new ArrayList<>();
        List<LocalDate> bucketStarts = new ArrayList<>();
        if (weekly) {
            LocalDate cursor = start;
            WeekFields wf = WeekFields.of(Locale.getDefault());
            while (!cursor.isAfter(end)) {
                LocalDate weekStart = cursor.with(wf.dayOfWeek(), 1);
                if (bucketStarts.isEmpty() || !bucketStarts.get(bucketStarts.size() - 1).equals(weekStart)) {
                    bucketStarts.add(weekStart);
                    labels.add(weekStart.format(DateTimeFormatter.ofPattern("dd MMM")));
                }
                cursor = cursor.plusWeeks(1);
            }
        } else {
            YearMonth cursor = YearMonth.from(start);
            YearMonth endYm = YearMonth.from(end);
            while (!cursor.isAfter(endYm)) {
                bucketStarts.add(cursor.atDay(1));
                labels.add(cursor.format(DateTimeFormatter.ofPattern("MMM")));
                cursor = cursor.plusMonths(1);
            }
        }

        List<Long> inqSeries = new ArrayList<>();
        List<Long> leadSeries = new ArrayList<>();
        List<Long> appSeries = new ArrayList<>();
        List<Long> approvedSeries = new ArrayList<>();
        List<Long> enrolledSeries = new ArrayList<>();

        for (int i = 0; i < bucketStarts.size(); i++) {
            LocalDate bucketStart = bucketStarts.get(i);
            LocalDateTime from = bucketStart.atStartOfDay();
            LocalDateTime to = i + 1 < bucketStarts.size()
                    ? bucketStarts.get(i + 1).atStartOfDay()
                    : end.plusDays(1).atStartOfDay();

            long inq = inquiries.stream()
                    .filter(x -> inRange(x.getCreatedOn(), from, to))
                    .count();
            long leads = inquiries.stream()
                    .filter(x -> inRange(x.getCreatedOn(), from, to))
                    .filter(x -> x.getStatus() != InquiryStatus.NEW && x.getStatus() != InquiryStatus.LOST)
                    .count();
            long apps = applications.stream()
                    .filter(x -> inRange(x.getCreatedOn(), from, to))
                    .count();
            long approved = applications.stream()
                    .filter(x -> inRange(x.getCreatedOn(), from, to))
                    .filter(x -> x.getStatus() == ApplicationStatus.APPROVED || x.getStatus() == ApplicationStatus.ENROLLED)
                    .count();
            long enrolled = applications.stream()
                    .filter(x -> inRange(x.getCreatedOn(), from, to))
                    .filter(x -> x.getStatus() == ApplicationStatus.ENROLLED)
                    .count();
            inqSeries.add(inq);
            leadSeries.add(leads);
            appSeries.add(apps);
            approvedSeries.add(approved);
            enrolledSeries.add(enrolled);
        }

        return TrendSeries.builder()
                .granularity(weekly ? "WEEKLY" : "MONTHLY")
                .labels(labels)
                .inquiries(inqSeries)
                .leads(leadSeries)
                .applications(appSeries)
                .approved(approvedSeries)
                .enrolled(enrolledSeries)
                .build();
    }

    private List<SourceBreakdownRow> buildSourceBreakdown(
            List<Inquiry> inquiries,
            List<ApplicationAdmission> applications,
            Map<Long, Inquiry> inquiryById) {
        Map<String, Long> leadCounts = new LinkedHashMap<>();
        for (LeadSource source : LeadSource.values()) {
            leadCounts.put(source.name(), 0L);
        }
        leadCounts.put("UNKNOWN", 0L);
        for (Inquiry inquiry : inquiries) {
            String key = inquiry.getInquirySource() == null ? "UNKNOWN" : inquiry.getInquirySource().name();
            leadCounts.merge(key, 1L, Long::sum);
        }

        Map<String, Long> appCounts = new HashMap<>();
        Map<String, Long> enrolledCounts = new HashMap<>();
        for (ApplicationAdmission app : applications) {
            Inquiry linked = app.getInquiryId() == null ? null : inquiryById.get(app.getInquiryId());
            String key = linked == null || linked.getInquirySource() == null
                    ? "UNKNOWN"
                    : linked.getInquirySource().name();
            appCounts.merge(key, 1L, Long::sum);
            if (app.getStatus() == ApplicationStatus.ENROLLED) {
                enrolledCounts.merge(key, 1L, Long::sum);
            }
        }

        long totalLeads = Math.max(inquiries.size(), 1);
        List<SourceBreakdownRow> rows = new ArrayList<>();
        for (Map.Entry<String, Long> e : leadCounts.entrySet()) {
            if (e.getValue() == 0 && !"UNKNOWN".equals(e.getKey())) {
                // keep zero sources only if they appear in apps
                if (!appCounts.containsKey(e.getKey())) {
                    continue;
                }
            }
            if (e.getValue() == 0 && (appCounts.getOrDefault(e.getKey(), 0L) == 0)) {
                continue;
            }
            long leads = e.getValue();
            long apps = appCounts.getOrDefault(e.getKey(), 0L);
            long enrolled = enrolledCounts.getOrDefault(e.getKey(), 0L);
            rows.add(SourceBreakdownRow.builder()
                    .key(e.getKey())
                    .label(humanize(e.getKey()))
                    .leads(leads)
                    .applications(apps)
                    .enrolled(enrolled)
                    .conversionRate(leads == 0 ? 0D : round1(enrolled * 100.0 / leads))
                    .percentOfLeads(pct(leads, totalLeads))
                    .build());
        }
        rows.sort(Comparator.comparingLong(SourceBreakdownRow::getLeads).reversed());
        return rows;
    }

    private List<NamedCount> buildLeadStatus(List<Inquiry> inquiries) {
        Map<InquiryStatus, Long> counts = new EnumMap<>(InquiryStatus.class);
        for (InquiryStatus status : InquiryStatus.values()) {
            counts.put(status, 0L);
        }
        for (Inquiry inquiry : inquiries) {
            if (inquiry.getStatus() != null) {
                counts.merge(inquiry.getStatus(), 1L, Long::sum);
            }
        }
        long total = Math.max(inquiries.size(), 1);
        List<NamedCount> rows = new ArrayList<>();
        for (InquiryStatus status : InquiryStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            rows.add(named(status.name(), humanize(status.name()), count, pct(count, total)));
        }
        return rows;
    }

    private List<NamedCount> buildApplicationsByClass(List<ApplicationAdmission> applications) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ApplicationAdmission app : applications) {
            String label = (app.getApplyingForClass() == null || app.getApplyingForClass().isBlank())
                    ? "Unspecified"
                    : app.getApplyingForClass().trim();
            counts.merge(label, 1L, Long::sum);
        }
        long total = Math.max(applications.size(), 1);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> named(e.getKey(), e.getKey(), e.getValue(), pct(e.getValue(), total)))
                .collect(Collectors.toList());
    }

    private FollowUpHealth buildFollowUpHealth(List<Inquiry> inquiries, AdmissionReportFilterRequest filter) {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        Set<Long> inquiryIds = inquiries.stream().map(Inquiry::getInquiryId).collect(Collectors.toSet());

        long dueToday = countFollowUps(followUpRepository.findTodayOrgWide(dayStart, dayEnd), inquiryIds, filter);
        long overdue = countFollowUps(followUpRepository.findOverdueOrgWide(dayStart), inquiryIds, filter);
        long upcoming = countFollowUps(followUpRepository.findUpcomingOrgWide(dayEnd), inquiryIds, filter);
        long completed = countFollowUps(followUpRepository.findCompletedOrgWide(), inquiryIds, filter);

        Set<Long> withOpenFollowUp = new HashSet<>();
        for (InquiryFollowUp fu : followUpRepository.findTodayOrgWide(dayStart, dayEnd)) {
            if (fu.getInquiry() != null) withOpenFollowUp.add(fu.getInquiry().getInquiryId());
        }
        for (InquiryFollowUp fu : followUpRepository.findOverdueOrgWide(dayStart)) {
            if (fu.getInquiry() != null) withOpenFollowUp.add(fu.getInquiry().getInquiryId());
        }
        for (InquiryFollowUp fu : followUpRepository.findUpcomingOrgWide(dayEnd)) {
            if (fu.getInquiry() != null) withOpenFollowUp.add(fu.getInquiry().getInquiryId());
        }
        long noFollowUp = inquiries.stream()
                .filter(i -> i.getStatus() != InquiryStatus.LOST
                        && i.getStatus() != InquiryStatus.APPLICATION_SUBMITTED)
                .filter(i -> !withOpenFollowUp.contains(i.getInquiryId()))
                .filter(i -> i.getNextFollowUpDate() == null)
                .count();

        return FollowUpHealth.builder()
                .dueToday(dueToday)
                .overdue(overdue)
                .upcoming(upcoming)
                .completed(completed)
                .noFollowUp(noFollowUp)
                .build();
    }

    private List<CounselorPerformanceRow> buildCounselorPerformance(
            List<Inquiry> inquiries,
            List<ApplicationAdmission> applications,
            Map<Long, Inquiry> inquiryById,
            FollowUpHealth ignored) {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        Map<Long, long[]> leadAppEnrolled = new HashMap<>();
        for (Inquiry inquiry : inquiries) {
            Long id = inquiry.getAssignedCounselorId();
            leadAppEnrolled.computeIfAbsent(id, k -> new long[3])[0]++;
        }
        for (ApplicationAdmission app : applications) {
            Inquiry linked = app.getInquiryId() == null ? null : inquiryById.get(app.getInquiryId());
            Long counselorId = linked == null ? null : linked.getAssignedCounselorId();
            long[] row = leadAppEnrolled.computeIfAbsent(counselorId, k -> new long[3]);
            row[1]++;
            if (app.getStatus() == ApplicationStatus.ENROLLED) {
                row[2]++;
            }
        }

        Map<Long, Long> overdueByCounselor = new HashMap<>();
        Map<Long, Long> dueByCounselor = new HashMap<>();
        Set<Long> inquiryIds = inquiries.stream().map(Inquiry::getInquiryId).collect(Collectors.toSet());
        for (InquiryFollowUp fu : followUpRepository.findOverdueOrgWide(dayStart)) {
            if (fu.getInquiry() == null || !inquiryIds.contains(fu.getInquiry().getInquiryId())) continue;
            overdueByCounselor.merge(fu.getInquiry().getAssignedCounselorId(), 1L, Long::sum);
        }
        for (InquiryFollowUp fu : followUpRepository.findTodayOrgWide(dayStart, dayEnd)) {
            if (fu.getInquiry() == null || !inquiryIds.contains(fu.getInquiry().getInquiryId())) continue;
            dueByCounselor.merge(fu.getInquiry().getAssignedCounselorId(), 1L, Long::sum);
        }

        List<CounselorPerformanceRow> rows = new ArrayList<>();
        for (Map.Entry<Long, long[]> e : leadAppEnrolled.entrySet()) {
            long leads = e.getValue()[0];
            long apps = e.getValue()[1];
            long enrolled = e.getValue()[2];
            rows.add(CounselorPerformanceRow.builder()
                    .counselorId(e.getKey())
                    .counselorName(resolveCounselorName(e.getKey()))
                    .leads(leads)
                    .applications(apps)
                    .enrolled(enrolled)
                    .conversionRate(leads == 0 ? 0D : round1(enrolled * 100.0 / leads))
                    .overdueFollowUps(overdueByCounselor.getOrDefault(e.getKey(), 0L))
                    .dueTodayFollowUps(dueByCounselor.getOrDefault(e.getKey(), 0L))
                    .build());
        }
        rows.sort(Comparator.comparingLong(CounselorPerformanceRow::getLeads).reversed());
        return rows;
    }

    private List<NamedCount> buildApplicationStatus(List<ApplicationAdmission> applications) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }
        for (ApplicationAdmission app : applications) {
            if (app.getStatus() != null) {
                counts.merge(app.getStatus(), 1L, Long::sum);
            }
        }
        long total = Math.max(applications.size(), 1);
        List<NamedCount> rows = new ArrayList<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            if (count == 0) continue;
            rows.add(named(status.name(), humanize(status.name()), count, pct(count, total)));
        }
        return rows;
    }

    private List<NamedCount> buildDocumentVerification(List<ApplicationAdmission> applications) {
        if (applications.isEmpty()) {
            return List.of(
                    named(DocumentCheckStatus.PENDING.name(), "Pending Verification", 0, 0D),
                    named(DocumentCheckStatus.VERIFIED.name(), "Verified", 0, 0D),
                    named(DocumentCheckStatus.REJECTED.name(), "Rejected", 0, 0D),
                    named(DocumentCheckStatus.MISSING.name(), "Not Uploaded", 0, 0D)
            );
        }
        List<Long> appIds = applications.stream().map(ApplicationAdmission::getApplicationId).toList();
        Map<DocumentCheckStatus, Long> counts = new EnumMap<>(DocumentCheckStatus.class);
        for (DocumentCheckStatus status : DocumentCheckStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : documentRepository.countByStatusForApplications(appIds)) {
            DocumentCheckStatus status = (DocumentCheckStatus) row[0];
            counts.put(status, ((Number) row[1]).longValue());
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long denom = Math.max(total, 1);
        List<NamedCount> rows = new ArrayList<>();
        rows.add(named(DocumentCheckStatus.PENDING.name(), "Pending Verification",
                counts.getOrDefault(DocumentCheckStatus.PENDING, 0L),
                pct(counts.getOrDefault(DocumentCheckStatus.PENDING, 0L), denom)));
        rows.add(named(DocumentCheckStatus.VERIFIED.name(), "Verified",
                counts.getOrDefault(DocumentCheckStatus.VERIFIED, 0L),
                pct(counts.getOrDefault(DocumentCheckStatus.VERIFIED, 0L), denom)));
        rows.add(named(DocumentCheckStatus.REJECTED.name(), "Rejected",
                counts.getOrDefault(DocumentCheckStatus.REJECTED, 0L),
                pct(counts.getOrDefault(DocumentCheckStatus.REJECTED, 0L), denom)));
        rows.add(named(DocumentCheckStatus.MISSING.name(), "Not Uploaded",
                counts.getOrDefault(DocumentCheckStatus.MISSING, 0L),
                pct(counts.getOrDefault(DocumentCheckStatus.MISSING, 0L), denom)));
        return rows;
    }

    private List<RecentApplicationRow> buildRecentApplications(
            List<ApplicationAdmission> applications,
            Map<Long, Inquiry> inquiryById,
            AdmissionReportFilterRequest filter) {
        int limit = filter.getRecentLimit() == null ? 15 : Math.min(Math.max(filter.getRecentLimit(), 1), 50);
        List<ApplicationAdmission> recent = applications.stream()
                .sorted(Comparator.comparing(ApplicationAdmission::getCreatedOn,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
        if (recent.isEmpty()) {
            return List.of();
        }
        List<Long> ids = recent.stream().map(ApplicationAdmission::getApplicationId).toList();
        Map<Long, long[]> docStats = new HashMap<>();
        for (AdmissionApplicationDocument doc : documentRepository.findByApplicationApplicationIdIn(ids)) {
            Long appId = doc.getApplication().getApplicationId();
            long[] stats = docStats.computeIfAbsent(appId, k -> new long[2]);
            stats[0]++;
            if (doc.getStatus() == DocumentCheckStatus.VERIFIED) {
                stats[1]++;
            }
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        List<RecentApplicationRow> rows = new ArrayList<>();
        for (ApplicationAdmission app : recent) {
            Inquiry linked = app.getInquiryId() == null ? null : inquiryById.get(app.getInquiryId());
            long[] stats = docStats.getOrDefault(app.getApplicationId(), new long[]{0, 0});
            rows.add(RecentApplicationRow.builder()
                    .applicationId(app.getApplicationId())
                    .applicantName(app.getApplicantName())
                    .applyingForClass(app.getApplyingForClass())
                    .source(linked == null || linked.getInquirySource() == null
                            ? "—"
                            : humanize(linked.getInquirySource().name()))
                    .counselorName(linked == null
                            ? "—"
                            : resolveCounselorName(linked.getAssignedCounselorId()))
                    .applicationDate(app.getCreatedOn() == null ? "—" : app.getCreatedOn().toLocalDate().format(fmt))
                    .status(app.getStatus() == null ? "—" : app.getStatus().name())
                    .documentsUploaded(stats[0])
                    .documentsVerified(stats[1])
                    .inquiryId(app.getInquiryId())
                    .build());
        }
        return rows;
    }

    private long countFollowUps(List<InquiryFollowUp> followUps, Set<Long> inquiryIds, AdmissionReportFilterRequest filter) {
        if (inquiryIds.isEmpty() && AdmissionReportSpecification.leadFiltersActive(filter)) {
            return 0;
        }
        boolean restrict = AdmissionReportSpecification.leadFiltersActive(filter)
                || filter.getAcademicYearId() != null
                || filter.getClassId() != null
                || filter.getDateFrom() != null
                || filter.getDateTo() != null
                || filter.getLeadStatus() != null;
        return followUps.stream()
                .filter(fu -> fu.getInquiry() != null && !Boolean.TRUE.equals(fu.getInquiry().getDeleted()))
                .filter(fu -> !restrict || inquiryIds.contains(fu.getInquiry().getInquiryId()))
                .filter(fu -> {
                    if (filter.getCounselorId() == null) return true;
                    return Objects.equals(filter.getCounselorId(), fu.getInquiry().getAssignedCounselorId());
                })
                .count();
    }

    private AdmissionReportFilterRequest priorPeriodFilter(AdmissionReportFilterRequest filter) {
        if (filter.getDateFrom() == null || filter.getDateTo() == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(filter.getDateFrom(), filter.getDateTo()) + 1;
        if (days <= 0) {
            return null;
        }
        AdmissionReportFilterRequest prior = new AdmissionReportFilterRequest();
        prior.setAcademicYearId(filter.getAcademicYearId());
        prior.setClassId(filter.getClassId());
        prior.setSource(filter.getSource());
        prior.setCounselorId(filter.getCounselorId());
        prior.setLeadStatus(filter.getLeadStatus());
        prior.setApplicationStatus(filter.getApplicationStatus());
        prior.setDateTo(filter.getDateFrom().minusDays(1));
        prior.setDateFrom(prior.getDateTo().minusDays(days - 1));
        prior.setTrendGranularity(filter.getTrendGranularity());
        return prior;
    }

    private static boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime toExclusive) {
        return value != null && !value.isBefore(from) && value.isBefore(toExclusive);
    }

    private static NamedCount named(String key, String label, long count, Double percent) {
        return NamedCount.builder().key(key).label(label).count(count).percentOfTotal(percent).build();
    }

    private static double pct(long count, long total) {
        return round1(count * 100.0 / total);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static Double pctChange(long previous, long current) {
        if (previous == 0) {
            return current == 0 ? 0D : 100D;
        }
        return round1((current - previous) * 100.0 / previous);
    }

    private static long countStatus(List<NamedCount> rows, String key) {
        return rows.stream().filter(r -> key.equals(r.getKey())).mapToLong(NamedCount::getCount).findFirst().orElse(0L);
    }

    private static String humanize(String raw) {
        if (raw == null || raw.isBlank() || "UNKNOWN".equals(raw)) {
            return "Unknown";
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private void appendCsv(StringBuilder sb, String section, String key, String label, Object value) {
        sb.append(csv(section)).append(',')
                .append(csv(key)).append(',')
                .append(csv(label)).append(',')
                .append(csv(value)).append('\n');
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String normalized = String.valueOf(value).replace("\r", " ").replace("\n", " ");
        if (normalized.contains(",") || normalized.contains("\"")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private String resolveCounselorName(Long counselorId) {
        if (counselorId == null) {
            return "Unassigned";
        }
        return staffRepository.findById(counselorId)
                .or(() -> staffRepository.findByUser_Id(counselorId))
                .map(staff -> {
                    String last = staff.getLastName() == null ? "" : staff.getLastName().trim();
                    return (staff.getFirstName() + " " + last).trim();
                })
                .filter(name -> !name.isBlank())
                .orElse("Counselor " + counselorId);
    }

    private void requireViewReports() {
        if (hasElevatedRole()) {
            return;
        }
        User user = currentUser();
        Long orgId = OrganizationContext.getOrganizationId();
        if (user == null || orgId == null
                || !permissionService.hasPermission(user.getId(), orgId, RESOURCE_ADMISSIONS_REPORTS, "VIEW")) {
            // Fall back to leads view so counselors with leads access can still open overview metrics
            if (user != null && orgId != null
                    && permissionService.hasPermission(user.getId(), orgId, "ADMISSIONS_LEADS", "VIEW")) {
                return;
            }
            throw new AccessDeniedException("ADMISSIONS_REPORTS:VIEW required");
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean hasElevatedRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "SUPER_ADMIN".equals(a)
                        || "ORGANIZATION_OWNER".equals(a)
                        || "ORGANIZATION_ADMIN".equals(a));
    }
}
