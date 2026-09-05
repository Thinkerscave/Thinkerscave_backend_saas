package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.academics.entity.TeacherAllocation;
import com.thinkerscave.academics.entity.TeacherAllocationTeacher;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.attendance.entity.StaffAttendance;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import com.thinkerscave.attendance.repository.StaffAttendanceRepository;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.communication.entity.Notice;
import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.communication.repository.NoticeRepository;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.dto.response.widgetdata.*;
import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetType;
import com.thinkerscave.dashboard.service.DashboardTimetableHelper;
import com.thinkerscave.dashboard.util.RoleLabels;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.PayrollRepository;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Teacher/Staff dashboard. Centers around the sign-in/out attendance
 * widget and today's teaching schedule — all real, org/staff-scoped data.
 */
@Component
@RequiredArgsConstructor
public class StaffDashboardProvider extends AbstractDashboardWidgetProvider implements DashboardWidgetProvider {

    private final StaffRepository staffRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final PayrollRepository payrollRepository;
    private final NoticeRepository noticeRepository;
    private final DashboardTimetableHelper timetableHelper;
    private final TeacherAllocationTeacherRepository teacherAllocationTeacherRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ResponsibilityAssignmentRepository responsibilityAssignmentRepository;

    @Override
    public List<WidgetDTO<?>> getWidgets(User user) {
        Staff staff = user != null ? staffRepository.findByUser_Id(user.getId()).orElse(null) : null;
        List<TimetableSlotItem> todaySlots = staff != null
                ? timetableHelper.todaySlotsForTeacher(staff.getStaffId()) : List.of();

        return List.of(
                welcomeHeader(user, staff),
                kpiGrid(staff, todaySlots),
                staffAttendanceToggle(staff),
                quickActions(),
                todaysTimetable(todaySlots),
                studentAttendanceToday(),
                myClassesOverview(staff),
                myResponsibilities(staff),
                announcements(),
                upcomingEvents()
        );
    }

    private WidgetDTO<WelcomeHeaderData> welcomeHeader(User user, Staff staff) {
        return safeWidget("welcome-header", WidgetType.WELCOME_HEADER, "Welcome", 4, DataMode.LIVE, () ->
                WelcomeHeaderData.builder()
                        .displayName(staff != null ? staff.getFirstName() + " " + staff.getLastName() : displayName(user))
                        .roleLabel(RoleLabels.of(com.thinkerscave.access.enums.RoleType.STAFF))
                        .greeting(RoleLabels.greeting())
                        .avatarUrl(staff != null ? staff.getPhotoUrl() : null)
                        .todayLabel(LocalDate.now().toString())
                        .build());
    }

    private WidgetDTO<KpiGridData> kpiGrid(Staff staff, List<TimetableSlotItem> todaySlots) {
        return safeWidget("kpi-grid", WidgetType.KPI_GRID, "Your day at a glance", 4, DataMode.LIVE, () -> {
            String payrollStatus = "N/A";
            if (staff != null) {
                LocalDate now = LocalDate.now();
                payrollStatus = payrollRepository.findByStaff_StaffIdAndPayrollYearAndPayrollMonth(
                                staff.getStaffId(), now.getYear(), now.getMonthValue())
                        .map(p -> p.getStatus() != null ? p.getStatus().name() : "PENDING")
                        .orElse("NOT GENERATED");
            }

            long classesCompleted = todaySlots.stream()
                    .filter(s -> s.getEndTime() != null && s.getEndTime().isBefore(java.time.LocalTime.now()))
                    .count();

            Long orgId = OrganizationContext.getOrganizationId();
            long attendancePending = todaySlots.stream()
                    .map(TimetableSlotItem::getClassName)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();

            return KpiGridData.builder().items(List.of(
                    KpiItem.builder().label("Today's Classes").value(String.valueOf(todaySlots.size())).icon("pi-book").tone("primary").build(),
                    KpiItem.builder().label("Classes Completed").value(String.valueOf(classesCompleted)).icon("pi-check-circle").tone("success").build(),
                    KpiItem.builder().label("Distinct Classes Today").value(String.valueOf(attendancePending)).icon("pi-exclamation-circle").tone("warning").build(),
                    KpiItem.builder().label("Payroll Status").value(payrollStatus).icon("pi-money-bill").tone("success").build()
            )).build();
        });
    }

    private WidgetDTO<StaffAttendanceToggleData> staffAttendanceToggle(Staff staff) {
        return safeWidget("staff-attendance-toggle", WidgetType.STAFF_ATTENDANCE_TOGGLE, "Attendance", 4, DataMode.LIVE, () -> {
            if (staff == null) {
                return StaffAttendanceToggleData.builder().signedIn(false).signedOut(false).build();
            }
            Long orgId = OrganizationContext.getOrganizationId();
            StaffAttendance today = staffAttendanceRepository
                    .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, staff.getStaffId(), LocalDate.now())
                    .orElse(null);
            return StaffAttendanceToggleData.builder()
                    .staffId(staff.getStaffId())
                    .signedIn(today != null && today.getSignInTime() != null)
                    .signedOut(today != null && today.getSignOutTime() != null)
                    .signInTime(today != null ? today.getSignInTime() : null)
                    .signOutTime(today != null ? today.getSignOutTime() : null)
                    .workingMinutesSoFar(today != null ? today.getWorkingMinutes() : null)
                    .status(today != null && today.getStatus() != null ? today.getStatus().name() : null)
                    .build();
        });
    }

    private WidgetDTO<QuickActionsData> quickActions() {
        return safeWidget("quick-actions", WidgetType.QUICK_ACTIONS, "Quick actions", 4, DataMode.LIVE, () ->
                QuickActionsData.builder().items(List.of(
                        QuickActionItem.builder().label("Mark Student Attendance").icon("pi-calendar-plus").route("/app/attendance/students").tone("primary").build(),
                        QuickActionItem.builder().label("View Timetable").icon("pi-clock").route("/app/academics/timetable").tone("info").build(),
                        QuickActionItem.builder().label("My Payslips").icon("pi-money-bill").route("/app/staff/payroll").tone("success").build(),
                        QuickActionItem.builder().label("Notices").icon("pi-megaphone").route("/app/communication/notices").tone("warning").build()
                )).build());
    }

    private WidgetDTO<TimetableData> todaysTimetable(List<TimetableSlotItem> todaySlots) {
        return safeWidget("todays-timetable", WidgetType.TIMETABLE, "Today's timetable", 4, DataMode.LIVE, () ->
                TimetableData.builder()
                        .dayLabel(LocalDate.now().getDayOfWeek().toString())
                        .slots(todaySlots)
                        .build());
    }

    private WidgetDTO<AttendanceSummaryData> studentAttendanceToday() {
        return safeWidget("student-attendance-today", WidgetType.ATTENDANCE_SUMMARY, "Student attendance (today)", 2, DataMode.LIVE, () -> {
            Long orgId = OrganizationContext.getOrganizationId();
            LocalDate today = LocalDate.now();
            long present = studentAttendanceRepository.countByOrganizationIdAndAttendanceDateAndStatus(orgId, today, StudentAttendanceStatus.PRESENT);
            long absent = studentAttendanceRepository.countByOrganizationIdAndAttendanceDateAndStatus(orgId, today, StudentAttendanceStatus.ABSENT);
            long late = studentAttendanceRepository.countByOrganizationIdAndAttendanceDateAndStatus(orgId, today, StudentAttendanceStatus.LATE);
            long total = present + absent + late;
            return AttendanceSummaryData.builder()
                    .presentCount(present).absentCount(absent).lateCount(late).totalCount(total)
                    .percentage(total == 0 ? 0 : (present * 100.0) / total)
                    .date(today)
                    .build();
        });
    }

    private WidgetDTO<StatListData> myClassesOverview(Staff staff) {
        return safeWidget("my-classes-overview", WidgetType.STAT_LIST, "My classes overview", 2, DataMode.LIVE, () -> {
            if (staff == null) {
                return StatListData.builder().items(Collections.emptyList()).build();
            }
            Long yearId = academicYearRepository.findByCurrentYearTrue()
                    .map(y -> y.getAcademicYearId()).orElse(null);
            if (yearId == null) {
                return StatListData.builder().items(Collections.emptyList()).build();
            }
            List<TeacherAllocationTeacher> allocations = teacherAllocationTeacherRepository
                    .findActiveByStaffAndYear(staff.getStaffId(), yearId);

            Map<String, Integer> subjectCountBySection = new LinkedHashMap<>();
            for (TeacherAllocationTeacher tat : allocations) {
                TeacherAllocation ta = tat.getTeacherAllocation();
                String key = ta.getSection().getAcademicClass().getName() + " " + ta.getSection().getName();
                subjectCountBySection.merge(key, 1, Integer::sum);
            }

            return StatListData.builder().items(subjectCountBySection.entrySet().stream()
                    .map(e -> StatListItem.builder()
                            .label(e.getKey())
                            .value(e.getValue() + (e.getValue() == 1 ? " subject" : " subjects"))
                            .icon("pi-book")
                            .tone("primary")
                            .build())
                    .collect(Collectors.toList())).build();
        });
    }

    private WidgetDTO<StatListData> myResponsibilities(Staff staff) {
        return safeWidget("my-responsibilities", WidgetType.STAT_LIST, "My responsibilities", 2, DataMode.LIVE, () -> {
            if (staff == null) {
                return StatListData.builder().items(Collections.emptyList()).build();
            }
            var assignments = responsibilityAssignmentRepository
                    .findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(staff.getStaffId());
            return StatListData.builder().items(assignments.stream()
                    .map(a -> StatListItem.builder()
                            .label(a.getResponsibility().getResponsibilityName())
                            .value(a.getScope() != null ? a.getScope() : "—")
                            .icon("pi-sitemap")
                            .tone("info")
                            .build())
                    .collect(Collectors.toList())).build();
        });
    }

    private WidgetDTO<AnnouncementsData> announcements() {
        return safeWidget("announcements", WidgetType.ANNOUNCEMENTS, "Recent announcements", 2, DataMode.LIVE, () -> {
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

    private String displayName(User user) {
        if (user == null) return "Staff";
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return (user.getFirstName() != null ? user.getFirstName() : "") +
                (user.getLastName() != null ? " " + user.getLastName() : "");
    }

    private String trim(String content) {
        if (content == null) return null;
        return content.length() > 140 ? content.substring(0, 140) + "…" : content;
    }
}
