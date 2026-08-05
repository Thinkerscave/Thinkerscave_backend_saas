package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.academics.entity.AcademicCalendarEvent;
import com.thinkerscave.academics.repository.AcademicCalendarEventRepository;
import com.thinkerscave.admission.entity.Inquiry;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.audit.entity.AuditLog;
import com.thinkerscave.audit.repository.AuditLogRepository;
import com.thinkerscave.communication.entity.Notice;
import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.communication.repository.NoticeRepository;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.dto.response.widgetdata.*;
import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetType;
import com.thinkerscave.dashboard.service.SampleWidgetFactory;
import com.thinkerscave.dashboard.util.ChartBucketUtil;
import com.thinkerscave.dashboard.util.RoleLabels;
import com.thinkerscave.onboarding.dto.OnboardingChecklistItemResponse;
import com.thinkerscave.onboarding.dto.OnboardingChecklistResponse;
import com.thinkerscave.onboarding.service.OnboardingService;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.enums.StudentStatus;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.stream.Collectors;

/**
 * School/Institution owner dashboard — business + operational overview for
 * a single organization (tenant).
 */
@Component
@RequiredArgsConstructor
public class OrgOwnerDashboardProvider extends AbstractDashboardWidgetProvider implements DashboardWidgetProvider {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final InquiryRepository inquiryRepository;
    private final ApplicationAdmissionRepository applicationAdmissionRepository;
    private final NoticeRepository noticeRepository;
    private final AuditLogRepository auditLogRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;
    private final SampleWidgetFactory sampleWidgetFactory;
    private final OnboardingService onboardingService;
    private final OrganizationRepository organizationRepository;

    @Override
    public List<WidgetDTO<?>> getWidgets(User user) {
        return List.of(
                welcomeHeader(user),
                kpiGrid(),
                quickActions(),
                studentGrowthChart(),
                admissionTrendChart(),
                feeCollectionTrendChart(),
                recentAdmissions(),
                pendingApprovals(),
                recentActivity(),
                announcements(),
                upcomingEvents(),
                todaysBirthdays()
        );
    }

    private WidgetDTO<WelcomeHeaderData> welcomeHeader(User user) {
        return safeWidget("welcome-header", WidgetType.WELCOME_HEADER, "Welcome", 4, DataMode.LIVE, () -> {
            WelcomeHeaderData.WelcomeHeaderDataBuilder builder = WelcomeHeaderData.builder()
                    .displayName(user != null ? displayName(user) : "Owner")
                    .roleLabel(RoleLabels.of(com.thinkerscave.access.enums.RoleType.ORGANIZATION_OWNER))
                    .organizationName(resolveOrganizationName())
                    .greeting(RoleLabels.greeting())
                    .avatarUrl(user != null ? user.getProfileImageUrl() : null)
                    .todayLabel(LocalDate.now().toString());
            applySetupGuide(builder);
            return builder.build();
        });
    }

    private void applySetupGuide(WelcomeHeaderData.WelcomeHeaderDataBuilder builder) {
        try {
            OnboardingChecklistResponse checklist = onboardingService.getChecklist();
            boolean showGuide = !checklist.isSetupComplete();
            builder.setupComplete(checklist.isSetupComplete())
                    .showSetupGuide(showGuide)
                    .setupProgressPercent(checklist.getProgressPercent())
                    .recommendedNextLabel(showGuide ? checklist.getRecommendedNextLabel() : null)
                    .recommendedNextRoute(showGuide ? checklist.getRecommendedNextRoute() : null)
                    .setupChecklist(showGuide ? checklist.getItems().stream()
                            .filter(OnboardingChecklistItemResponse::isAvailable)
                            .map(i -> WelcomeHeaderData.SetupChecklistItem.builder()
                                    .key(i.getKey())
                                    .label(i.getLabel())
                                    .completed(i.isCompleted())
                                    .requiredForCompletion(i.isRequiredForCompletion())
                                    .available(i.isAvailable())
                                    .route(i.getRoute())
                                    .build())
                            .collect(Collectors.toList()) : null);
        } catch (Exception ex) {
            builder.showSetupGuide(false).setupComplete(false);
        }
    }

    private String resolveOrganizationName() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null || orgId <= 0) {
            return null;
        }
        return organizationRepository.findById(orgId)
                .map(org -> org.getOrganizationName())
                .orElse(null);
    }

    private WidgetDTO<KpiGridData> kpiGrid() {
        return safeWidget("kpi-grid", WidgetType.KPI_GRID, "Organization overview", 4, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            long totalStudents = studentRepository.count();
            long totalStaff = staffRepository.countByActive(true);
            long presentToday = studentAttendanceRepository.countByOrganizationIdAndAttendanceDateAndStatus(
                    orgId, LocalDate.now(), StudentAttendanceStatus.PRESENT);
            long newAdmissionsToday = inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.NEW);

            return KpiGridData.builder().items(List.of(
                    KpiItem.builder().label("Students").value(String.valueOf(totalStudents)).icon("pi-users").tone("primary").build(),
                    KpiItem.builder().label("Staff").value(String.valueOf(totalStaff)).icon("pi-id-card").tone("info").build(),
                    KpiItem.builder().label("Today's Attendance").value(String.valueOf(presentToday)).icon("pi-calendar-plus").tone("success").build(),
                    KpiItem.builder().label("New Admissions").value(String.valueOf(newAdmissionsToday)).icon("pi-user-plus").tone("warning").build(),
                    KpiItem.builder().label("Fee Collection").value("₹3.2L").icon("pi-wallet").tone("success").sample(true).build(),
                    KpiItem.builder().label("Pending Fees").value("₹85K").icon("pi-exclamation-circle").tone("danger").sample(true).build()
            )).build();
        });
    }

    private WidgetDTO<QuickActionsData> quickActions() {
        return safeWidget("quick-actions", WidgetType.QUICK_ACTIONS, "Quick actions", 4, DataMode.LIVE, () ->
                QuickActionsData.builder().items(List.of(
                        QuickActionItem.builder().label("Add Student").icon("pi-user-plus").route("/app/student/students/new").tone("primary").build(),
                        QuickActionItem.builder().label("Add Staff").icon("pi-id-card").route("/app/staff/staff/new").tone("info").build(),
                        QuickActionItem.builder().label("Mark Attendance").icon("pi-calendar-plus").route("/app/attendance/students").tone("success").build(),
                        QuickActionItem.builder().label("Publish Notice").icon("pi-megaphone").route("/app/communication/notices/new").tone("warning").build()
                )).build());
    }

    private WidgetDTO<ChartData> studentGrowthChart() {
        return safeWidget("student-growth", WidgetType.CHART, "Student growth", "Last 6 months", 2, DataMode.LIVE, () -> {
            var timestamps = studentRepository.findAll(PageRequest.of(0, 2000)).stream()
                    .map(s -> s.getCreatedOn())
                    .collect(Collectors.toList());
            return ChartBucketUtil.monthlyCounts(timestamps, 6, "Students", "line");
        });
    }

    private WidgetDTO<ChartData> admissionTrendChart() {
        return safeWidget("admission-trend", WidgetType.CHART, "Admission trend", "Inquiries, last 6 months", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            var timestamps = inquiryRepository.findByOrganizationIdAndDeletedFalseOrderByCreatedOnDesc(orgId, PageRequest.of(0, 500))
                    .getContent().stream().map(Inquiry::getCreatedOn).collect(Collectors.toList());
            return ChartBucketUtil.monthlyCounts(timestamps, 6, "Inquiries", "bar");
        });
    }

    private WidgetDTO<ChartData> feeCollectionTrendChart() {
        return safeWidget("fee-collection-trend", WidgetType.CHART, "Fee collection trend", "Preview — last 6 months",
                2, DataMode.SAMPLE, () -> sampleWidgetFactory.trendChart(
                        "Fee collected (₹K)", "area", new double[]{240, 265, 250, 290, 310, 320}));
    }

    private WidgetDTO<RecentRecordsData> recentAdmissions() {
        return safeWidget("recent-admissions", WidgetType.RECENT_RECORDS, "Recent admissions", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            var apps = applicationAdmissionRepository.findByOrganizationIdOrderByCreatedOnDesc(orgId, PageRequest.of(0, 5)).getContent();
            return RecentRecordsData.builder()
                    .columns(List.of("Applicant", "Status", "Date"))
                    .items(apps.stream().map(a -> RecordItem.builder()
                            .primaryLabel(a.getApplicantName())
                            .secondaryLabel(a.getApplyingForClass())
                            .statusLabel(a.getStatus() != null ? a.getStatus().name() : "-")
                            .statusTone(a.getStatus() == ApplicationStatus.APPROVED ? "success" : "info")
                            .timestampLabel(a.getCreatedOn() != null ? a.getCreatedOn().toLocalDate().toString() : "-")
                            .build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private WidgetDTO<PendingTasksData> pendingApprovals() {
        return safeWidget("pending-approvals", WidgetType.PENDING_TASKS, "Pending approvals", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            long pendingApps = applicationAdmissionRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.UNDER_REVIEW);
            long docsPending = applicationAdmissionRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.DOCUMENTS_PENDING);
            return PendingTasksData.builder().items(List.of(
                    TaskItem.builder().title(pendingApps + " admission applications awaiting review").priority("high").completed(false).link("/app/admission/applications").build(),
                    TaskItem.builder().title(docsPending + " applications pending documents").priority("medium").completed(false).link("/app/admission/applications").build(),
                    TaskItem.builder().title("Review staff leave requests").priority("low").completed(false).sample(true).build()
            )).build();
        });
    }

    private WidgetDTO<RecentActivityData> recentActivity() {
        return safeWidget("recent-activity", WidgetType.RECENT_ACTIVITY, "Recent activity", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            List<AuditLog> logs = auditLogRepository.findByOrganizationId(orgId, PageRequest.of(0, 6, Sort.by("occurredAt").descending())).getContent();
            return RecentActivityData.builder()
                    .items(logs.stream().map(l -> ActivityItem.builder()
                            .title(l.getAction()).description(l.getSummary()).actorName(l.getActorUsername())
                            .occurredAt(l.getOccurredAt()).icon("pi-history").build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private WidgetDTO<AnnouncementsData> announcements() {
        return safeWidget("announcements", WidgetType.ANNOUNCEMENTS, "Announcements", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            List<Notice> notices = noticeRepository.findByOrganizationIdAndStatusOrderByPublishDateDesc(
                    orgId, NoticeStatus.PUBLISHED, PageRequest.of(0, 5)).getContent();
            return AnnouncementsData.builder()
                    .items(notices.stream().map(n -> AnnouncementItem.builder()
                            .title(n.getTitle()).summary(trim(n.getContent())).category(n.getCategory())
                            .publishedAt(n.getPublishDate()).pinned(n.isPinned()).build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private WidgetDTO<CalendarData> upcomingEvents() {
        return safeWidget("upcoming-events", WidgetType.EVENTS, "Upcoming events", 2, DataMode.LIVE, () -> {
            List<AcademicCalendarEvent> events = academicCalendarEventRepository
                    .findByStartDateGreaterThanEqualAndActiveOrderByStartDateAsc(LocalDate.now(), true);
            return CalendarData.builder().items(events.stream().limit(6).map(e -> CalendarEventItem.builder()
                    .title(e.getTitle()).startDate(e.getStartDate()).endDate(e.getEndDate())
                    .eventType(e.getEventType() != null ? e.getEventType().name() : null)
                    .allDay(Boolean.TRUE.equals(e.getAllDay())).build()).collect(Collectors.toList())).build();
        });
    }

    private WidgetDTO<StatListData> todaysBirthdays() {
        return safeWidget("todays-birthdays", WidgetType.STAT_LIST, "Today's birthdays", 2, DataMode.LIVE, () -> {
            MonthDay today = MonthDay.from(LocalDate.now());
            List<Staff> staff = staffRepository.findAll(PageRequest.of(0, 2000)).getContent();
            var items = staff.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActive()) && s.getDateOfBirth() != null
                            && MonthDay.from(s.getDateOfBirth()).equals(today))
                    .map(s -> StatListItem.builder()
                            .label(s.getFirstName() + " " + s.getLastName())
                            .value(s.getDesignation())
                            .icon("pi-gift")
                            .tone("info")
                            .build())
                    .collect(Collectors.toList());
            return StatListData.builder().items(items).build();
        });
    }

    private String displayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null ? " " + user.getLastName() : "");
    }

    private String trim(String content) {
        if (content == null) return null;
        return content.length() > 140 ? content.substring(0, 140) + "…" : content;
    }
}
