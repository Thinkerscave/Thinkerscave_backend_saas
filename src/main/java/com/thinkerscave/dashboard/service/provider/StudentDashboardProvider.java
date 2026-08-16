package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.communication.entity.Notice;
import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.communication.repository.NoticeRepository;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.dto.response.widgetdata.*;
import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetType;
import com.thinkerscave.dashboard.service.SampleWidgetFactory;
import com.thinkerscave.dashboard.util.RoleLabels;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Student dashboard — today's schedule, attendance and academic snapshot.
 */
@Component
@RequiredArgsConstructor
public class StudentDashboardProvider extends AbstractDashboardWidgetProvider implements DashboardWidgetProvider {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final NoticeRepository noticeRepository;
    private final SampleWidgetFactory sampleWidgetFactory;

    @Override
    public List<WidgetDTO<?>> getWidgets(User user) {
        Student student = user != null ? studentRepository.findByUser_Id(user.getId()).orElse(null) : null;
        StudentEnrollment enrollment = student != null
                ? studentEnrollmentRepository.findActiveWithClassByStudentId(student.getStudentId()).orElse(null)
                : null;

        return List.of(
                welcomeHeader(user, student),
                kpiGrid(student),
                quickActions(),
                todaysTimetable(enrollment),
                attendanceOverview(student),
                examinationSummaryPreview(),
                schoolNotices(),
                feeSummaryPreview(),
                recentAnnouncements(),
                upcomingEvents(),
                academicCalendar(),
                libraryPreview(),
                transportPreview()
        );
    }

    private WidgetDTO<WelcomeHeaderData> welcomeHeader(User user, Student student) {
        return safeWidget("welcome-header", WidgetType.WELCOME_HEADER, "Welcome", 4, DataMode.LIVE, () ->
                WelcomeHeaderData.builder()
                        .displayName(student != null ? student.getFirstName() + " " + student.getLastName() : displayName(user))
                        .roleLabel(RoleLabels.of(com.thinkerscave.access.enums.RoleType.STUDENT))
                        .greeting(RoleLabels.greeting())
                        .avatarUrl(student != null ? student.getPhotoUrl() : null)
                        .todayLabel(LocalDate.now().toString())
                        .build());
    }

    private WidgetDTO<KpiGridData> kpiGrid(Student student) {
        return safeWidget("kpi-grid", WidgetType.KPI_GRID, "Your snapshot", 4, DataMode.LIVE, () -> {
            double attendancePct = attendancePercentage(student);
            long noticeCount = 0;
            try {
                Long orgId = OrganizationContext.getOrganizationId();
                noticeCount = noticeRepository.findByOrganizationIdAndStatusOrderByPublishDateDesc(
                        orgId, NoticeStatus.PUBLISHED, PageRequest.of(0, 50)).getTotalElements();
            } catch (Exception ignored) { /* best-effort */ }

            return KpiGridData.builder().items(List.of(
                    KpiItem.builder().label("Attendance").value(String.format("%.0f%%", attendancePct)).icon("pi-calendar-plus").tone(attendancePct >= 75 ? "success" : "danger").build(),
                    KpiItem.builder().label("Assignments").value("3 due").icon("pi-file-edit").tone("warning").sample(true).build(),
                    KpiItem.builder().label("Upcoming Exams").value("2").icon("pi-pencil").tone("info").sample(true).build(),
                    KpiItem.builder().label("Fee Balance").value("₹12.5K").icon("pi-wallet").tone("danger").sample(true).build(),
                    KpiItem.builder().label("Notifications").value(String.valueOf(noticeCount)).icon("pi-bell").tone("primary").build()
            )).build();
        });
    }

    private WidgetDTO<QuickActionsData> quickActions() {
        return safeWidget("quick-actions", WidgetType.QUICK_ACTIONS, "Quick links", 4, DataMode.LIVE, () ->
                QuickActionsData.builder().items(List.of(
                        QuickActionItem.builder().label("My Timetable").icon("pi-clock").route("/app/academics/timetable").tone("primary").build(),
                        QuickActionItem.builder().label("My Attendance").icon("pi-calendar-plus").route("/app/attendance/students").tone("info").build(),
                        QuickActionItem.builder().label("Notices").icon("pi-megaphone").route("/app/communication/notices").tone("warning").build(),
                        QuickActionItem.builder().label("Academic Calendar").icon("pi-calendar").route("/app/academics/academic-calendar").tone("success").build()
                )).build());
    }

    private WidgetDTO<TimetableData> todaysTimetable(StudentEnrollment enrollment) {
        return safeWidget("todays-timetable", WidgetType.TIMETABLE, "Today's timetable", 4, DataMode.LIVE, () ->
                TimetableData.builder()
                        .dayLabel(LocalDate.now().getDayOfWeek().toString())
                        .slots(Collections.emptyList())
                        .build());
    }

    private WidgetDTO<AttendanceSummaryData> attendanceOverview(Student student) {
        return safeWidget("attendance-overview", WidgetType.ATTENDANCE_SUMMARY, "Attendance overview", "Last 30 days", 2, DataMode.LIVE, () -> {
            if (student == null) return AttendanceSummaryData.builder().date(LocalDate.now()).build();
            Long orgId = OrganizationContext.getOrganizationId();
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(30);
            var rows = studentAttendanceRepository.countByStatusForStudent(orgId, student.getStudentId(), from, to);
            long present = 0, absent = 0, late = 0;
            for (Object[] r : rows) {
                String status = String.valueOf(r[0]);
                long count = ((Number) r[1]).longValue();
                if ("PRESENT".equals(status)) present = count;
                else if ("ABSENT".equals(status)) absent = count;
                else if ("LATE".equals(status)) late = count;
            }
            long total = present + absent + late;
            return AttendanceSummaryData.builder()
                    .presentCount(present).absentCount(absent).lateCount(late).totalCount(total)
                    .percentage(total == 0 ? 0 : (present * 100.0) / total)
                    .date(to)
                    .build();
        });
    }

    private WidgetDTO<ExaminationSummaryData> examinationSummaryPreview() {
        return safeWidget("examination-summary", WidgetType.EXAMINATION_SUMMARY, "Upcoming exams", "Future scope preview",
                2, DataMode.SAMPLE, sampleWidgetFactory::examinationSummary);
    }

    private WidgetDTO<NotificationsData> schoolNotices() {
        return safeWidget("school-notices", WidgetType.NOTIFICATIONS, "School notices", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            List<Notice> notices = noticeRepository.findByOrganizationIdAndStatusAndPinnedTrueOrderByPublishDateDesc(orgId, NoticeStatus.PUBLISHED);
            return NotificationsData.builder()
                    .unreadCount(notices.size())
                    .items(notices.stream().limit(6).map(n -> NotificationItem.builder()
                            .title(n.getTitle()).message(trim(n.getContent())).date(n.getPublishDate())
                            .category(n.getCategory()).pinned(n.isPinned()).build()).collect(Collectors.toList()))
                    .build();
        });
    }

    private WidgetDTO<FeeSummaryData> feeSummaryPreview() {
        return safeWidget("fee-summary", WidgetType.FEE_SUMMARY, "Fee summary", "Future scope preview",
                2, DataMode.SAMPLE, sampleWidgetFactory::feeSummary);
    }

    private WidgetDTO<AnnouncementsData> recentAnnouncements() {
        return safeWidget("recent-announcements", WidgetType.ANNOUNCEMENTS, "Recent announcements", 2, DataMode.LIVE, () -> {
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
        return safeWidget("upcoming-events", WidgetType.EVENTS, "Upcoming events", 2, DataMode.LIVE, () ->
                CalendarData.builder().items(Collections.emptyList()).build());
    }

    private WidgetDTO<CalendarData> academicCalendar() {
        return safeWidget("academic-calendar", WidgetType.CALENDAR, "Academic calendar", 4, DataMode.LIVE, () ->
                CalendarData.builder().items(Collections.emptyList()).build());
    }

    private WidgetDTO<LibrarySummaryData> libraryPreview() {
        return safeWidget("library-summary", WidgetType.LIBRARY_SUMMARY, "Library", "Future scope preview",
                2, DataMode.SAMPLE, sampleWidgetFactory::librarySummary);
    }

    private WidgetDTO<TransportSummaryData> transportPreview() {
        return safeWidget("transport-summary", WidgetType.TRANSPORT_SUMMARY, "Transport", "Future scope preview",
                2, DataMode.SAMPLE, sampleWidgetFactory::transportSummary);
    }

    private double attendancePercentage(Student student) {
        if (student == null) return 0;
        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        var rows = studentAttendanceRepository.countByStatusForStudent(orgId, student.getStudentId(), from, to);
        long present = 0, total = 0;
        for (Object[] r : rows) {
            long count = ((Number) r[1]).longValue();
            total += count;
            if ("PRESENT".equals(String.valueOf(r[0]))) present = count;
        }
        return total == 0 ? 0 : (present * 100.0) / total;
    }

    private String displayName(User user) {
        if (user == null) return "Student";
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null ? " " + user.getLastName() : "");
    }

    private String trim(String content) {
        if (content == null) return null;
        return content.length() > 140 ? content.substring(0, 140) + "…" : content;
    }
}
