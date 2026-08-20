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
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parent dashboard. Resolves the linked children as a list (future-ready
 * for multiple children) and defaults widgets to the first child.
 */
@Component
@RequiredArgsConstructor
public class ParentDashboardProvider extends AbstractDashboardWidgetProvider implements DashboardWidgetProvider {

    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final NoticeRepository noticeRepository;
    private final SampleWidgetFactory sampleWidgetFactory;

    @Override
    public List<WidgetDTO<?>> getWidgets(User user) {
        Parent parent = user != null ? parentRepository.findByUser_Id(user.getId()).orElse(null) : null;
        List<StudentParent> links = parent != null
                ? studentParentRepository.findByParent_ParentIdAndActiveTrue(parent.getParentId())
                : List.of();
        Student primaryChild = links.stream().findFirst().map(StudentParent::getStudent).orElse(null);
        StudentEnrollment enrollment = primaryChild != null
                ? studentEnrollmentRepository.findActiveWithClassByStudentId(primaryChild.getStudentId()).orElse(null)
                : null;

        return List.of(
                welcomeHeader(user, parent),
                childProfile(links, primaryChild, enrollment),
                kpiGrid(primaryChild),
                quickActions(),
                todaysSchedule(enrollment),
                attendanceOverview(primaryChild),
                recentAnnouncements(),
                upcomingEvents(),
                academicCalendar()
        );
    }

    private WidgetDTO<WelcomeHeaderData> welcomeHeader(User user, Parent parent) {
        return safeWidget("welcome-header", WidgetType.WELCOME_HEADER, "Welcome", 4, DataMode.LIVE, () ->
                WelcomeHeaderData.builder()
                        .displayName(parent != null ? parent.getFirstName() + " " + parent.getLastName() : displayName(user))
                        .roleLabel(RoleLabels.of(com.thinkerscave.access.enums.RoleType.PARENT))
                        .greeting(RoleLabels.greeting())
                        .avatarUrl(parent != null ? parent.getPhotoUrl() : null)
                        .todayLabel(LocalDate.now().toString())
                        .build());
    }

    private WidgetDTO<ChildProfileData> childProfile(List<StudentParent> links, Student primaryChild, StudentEnrollment enrollment) {
        return safeWidget("child-profile", WidgetType.CHILD_PROFILE, "My children", 4, DataMode.LIVE, () ->
                ChildProfileData.builder().children(links.stream().map(link -> {
                    Student s = link.getStudent();
                    boolean isPrimary = primaryChild != null && s.getStudentId().equals(primaryChild.getStudentId());
                    StudentEnrollment e = isPrimary ? enrollment
                            : studentEnrollmentRepository.findActiveWithClassByStudentId(s.getStudentId()).orElse(null);
                    return ChildItem.builder()
                            .studentId(s.getStudentId())
                            .displayName(s.getFirstName() + " " + s.getLastName())
                            .className(e != null && e.getClassEntity() != null ? e.getClassEntity().getClassName() : null)
                            .sectionName(e != null && e.getSection() != null ? e.getSection().getSectionName() : null)
                            .rollNumber(e != null ? e.getRollNumber() : null)
                            .photoUrl(s.getPhotoUrl())
                            .selected(isPrimary)
                            .build();
                }).collect(Collectors.toList())).build());
    }

    private WidgetDTO<KpiGridData> kpiGrid(Student primaryChild) {
        return safeWidget("kpi-grid", WidgetType.KPI_GRID, "Your child's snapshot", 4, DataMode.LIVE, () -> {
            double attendancePct = attendancePercentage(primaryChild);
            return KpiGridData.builder().items(List.of(
                    KpiItem.builder().label("Attendance").value(String.format("%.0f%%", attendancePct)).icon("pi-calendar-plus").tone(attendancePct >= 75 ? "success" : "danger").build(),
                    KpiItem.builder().label("Homework").value("2 pending").icon("pi-file-edit").tone("warning").sample(true).build(),
                    KpiItem.builder().label("Upcoming Exams").value("2").icon("pi-pencil").tone("info").sample(true).build(),
                    KpiItem.builder().label("Fee Balance").value("₹12.5K").icon("pi-wallet").tone("danger").sample(true).build(),
                    KpiItem.builder().label("Leave Requests").value("0").icon("pi-calendar-times").tone("success").sample(true).build()
            )).build();
        });
    }

    private WidgetDTO<QuickActionsData> quickActions() {
        return safeWidget("quick-actions", WidgetType.QUICK_ACTIONS, "Quick links", 4, DataMode.LIVE, () ->
                QuickActionsData.builder().items(List.of(
                        QuickActionItem.builder().label("View Attendance").icon("pi-calendar-plus").route("/app/attendance/students").tone("primary").build(),
                        QuickActionItem.builder().label("Fee Payments").icon("pi-wallet").route("/app/finance/fees").tone("warning").build(),
                        QuickActionItem.builder().label("Notices").icon("pi-megaphone").route("/app/communication/notices").tone("info").build(),
                        QuickActionItem.builder().label("Academic Calendar").icon("pi-calendar").route("/app/academics/academic-calendar").tone("success").build()
                )).build());
    }

    private WidgetDTO<TimetableData> todaysSchedule(StudentEnrollment enrollment) {
        return safeWidget("todays-schedule", WidgetType.TIMETABLE, "Today's schedule", 4, DataMode.LIVE, () ->
                TimetableData.builder()
                        .dayLabel(LocalDate.now().getDayOfWeek().toString())
                        .slots(Collections.emptyList())
                        .build());
    }

    private WidgetDTO<AttendanceSummaryData> attendanceOverview(Student primaryChild) {
        return safeWidget("attendance-overview", WidgetType.ATTENDANCE_SUMMARY, "Attendance overview", "Last 30 days", 2, DataMode.LIVE, () -> {
            if (primaryChild == null) return AttendanceSummaryData.builder().date(LocalDate.now()).build();
            Long orgId = OrganizationContext.getOrganizationId();
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(30);
            var rows = studentAttendanceRepository.countByStatusForStudent(orgId, primaryChild.getStudentId(), from, to);
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
        if (user == null) return "Parent";
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null ? " " + user.getLastName() : "");
    }

    private String trim(String content) {
        if (content == null) return null;
        return content.length() > 140 ? content.substring(0, 140) + "…" : content;
    }
}
