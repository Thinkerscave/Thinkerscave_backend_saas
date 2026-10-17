package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.dto.response.EffectivePermissionResponse;
import com.thinkerscave.access.service.PermissionService;
import com.thinkerscave.academics.dto.response.AcademicCalendarEventResponse;
import com.thinkerscave.academics.service.AcademicCalendarService;
import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.attendance.repository.StudentPeriodAttendanceRepository;
import com.thinkerscave.attendance.service.StaffAttendanceService;
import com.thinkerscave.communication.dto.response.NoticeResponse;
import com.thinkerscave.communication.service.NoticeService;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.dto.response.widgetdata.*;
import com.thinkerscave.dashboard.enums.DataMode;
import com.thinkerscave.dashboard.enums.WidgetType;
import com.thinkerscave.dashboard.service.DashboardTimetableHelper;
import com.thinkerscave.dashboard.util.RoleLabels;
import com.thinkerscave.dashboard.util.StaffAttendanceWeekMapper;
import com.thinkerscave.dashboard.util.StaffAttendanceWidgetMapper;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final String ATTENDANCE_MENU = "ATTENDANCE";
    private static final String STUDENT_ATTENDANCE_MENU = "ATTENDANCE_STUDENTS";
    private static final String STAFF_ATTENDANCE_MENU = "ATTENDANCE_STAFF";
    private static final String ATTENDANCE_REPORTS_MENU = "ATTENDANCE_REPORTS";
        private static final String TIMETABLE_MENU = "ACADEMICS_MY_TIMETABLE";
    private static final String CALENDAR_MENU = "ACADEMICS_CALENDAR";
    private static final String NOTICES_MENU = "COMMUNICATION_NOTICES";

    private final StaffRepository staffRepository;
    private final StaffAttendanceService staffAttendanceService;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final StudentPeriodAttendanceRepository studentPeriodAttendanceRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final NoticeService noticeService;
    private final AcademicCalendarService calendarService;
    private final DashboardTimetableHelper timetableHelper;
    private final PermissionService permissionService;

    @Override
    public List<WidgetDTO<?>> getWidgets(User user) {
        Staff staff = user != null ? staffRepository.findByUser_Id(user.getId()).orElse(null) : null;
        Map<String, EffectivePermissionResponse> permissions = permissionSnapshot(user);
        var todayAttendance = staff != null ? staffAttendanceService.buildTodayStatus(staff) : null;
        List<TimetableSlotItem> todaySlots = staff != null
                ? enrichAttendanceSlots(timetableHelper.todaySlotsForTeacher(staff.getStaffId()), permissions)
                : List.of();

        List<WidgetDTO<?>> widgets = new ArrayList<>();
        widgets.add(welcomeHeader(user, staff, todayAttendance));
        widgets.add(staffAttendanceToggle(todayAttendance));
        widgets.add(weeklyAttendanceSummary(staff));
        widgets.add(quickActions(permissions));
        widgets.add(todaysTimetable(permissions, todaySlots));
        widgets.add(announcements(user, staff, permissions));
                widgets.add(upcomingEvents(permissions));
        return widgets;
    }

    private List<TimetableSlotItem> enrichAttendanceSlots(
            List<TimetableSlotItem> slots, Map<String, EffectivePermissionResponse> permissions) {
        Long organizationId = OrganizationContext.getOrganizationId();
        boolean canView = canViewAttendancePage(permissions, STUDENT_ATTENDANCE_MENU);
        boolean canManage = hasPermission(permissions, ATTENDANCE_MENU, "MANAGE")
                && hasPermission(permissions, STUDENT_ATTENDANCE_MENU, "MANAGE");
        LocalDate date = LocalDate.now();
        LocalTime now = LocalTime.now();
        Map<SectionKey, Long> expectedBySection = new HashMap<>();
        Map<SessionKey, Long> markedBySession = new HashMap<>();

        for (TimetableSlotItem slot : slots) {
            if (slot.getClassId() == null || slot.getSectionId() == null) continue;
            SectionKey section = new SectionKey(slot.getClassId(), slot.getSectionId());
            long expected = expectedBySection.computeIfAbsent(section, key ->
                    studentEnrollmentRepository.countByClassEntityClassIdAndSectionSectionIdAndActiveTrue(
                            key.classId(), key.sectionId()));
            Long periodId = "PERIOD_WISE".equals(slot.getAttendanceMode()) ? slot.getPeriodId() : null;
            SessionKey session = new SessionKey(section, periodId);
            long marked = markedBySession.computeIfAbsent(session, key -> key.periodId() != null
                    ? studentPeriodAttendanceRepository.countByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateAndPeriodId(
                            organizationId, key.section().classId(), key.section().sectionId(), date, key.periodId())
                    : studentAttendanceRepository.countByOrganizationIdAndClassIdAndSectionIdAndAttendanceDate(
                            organizationId, key.section().classId(), key.section().sectionId(), date));

            String status = expected == 0 ? "NO_STUDENTS"
                    : marked >= expected ? "COMPLETED"
                    : marked > 0 ? "IN_PROGRESS"
                    : slot.getStartTime() != null && slot.getStartTime().isAfter(now) ? "UPCOMING" : "PENDING";
            boolean sessionAuthorized = slot.isAttendanceAllowed();
            slot.setAttendanceStatus(status);
            slot.setExpectedStudents((int) expected);
            slot.setMarkedStudents((int) marked);
            slot.setCanViewAttendance(canView && sessionAuthorized);
            slot.setCanMarkAttendance(canManage && sessionAuthorized && expected > 0 && !"COMPLETED".equals(status));
        }
        return slots;
    }

    private WidgetDTO<WelcomeHeaderData> welcomeHeader(
            User user, Staff staff, com.thinkerscave.attendance.dto.response.StaffAttendanceTodayResponse today) {
        return safeWidget("welcome-header", WidgetType.WELCOME_HEADER, "Welcome", 4, DataMode.LIVE, () ->
                WelcomeHeaderData.builder()
                        .displayName(staff != null ? staff.getFirstName() + " " + staff.getLastName() : displayName(user))
                        .organizationName(today != null ? today.getOrganizationName() : null)
                        .roleLabel(RoleLabels.of(com.thinkerscave.access.enums.RoleType.STAFF))
                        .greeting(RoleLabels.greeting())
                        .avatarUrl(staff != null ? staff.getPhotoUrl() : null)
                        .todayLabel(LocalDate.now().toString())
                        .build());
    }

        private WidgetDTO<StaffAttendanceToggleData> staffAttendanceToggle(
                        com.thinkerscave.attendance.dto.response.StaffAttendanceTodayResponse today) {
                return safeWidget("staff-attendance-toggle", WidgetType.STAFF_ATTENDANCE_TOGGLE,
                                "My Staff Attendance", "Mark your daily attendance", 1, DataMode.LIVE,
                                () -> StaffAttendanceWidgetMapper.toToggleData(today));
        }

        private WidgetDTO<StaffAttendanceWeekData> weeklyAttendanceSummary(Staff staff) {
                LocalDate today = LocalDate.now();
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate end = start.plusDays(6);
                return safeWidget("staff-attendance-week", WidgetType.STAFF_ATTENDANCE_WEEK,
                                "Attendance Summary (This Week)", 1, DataMode.LIVE,
                                () -> staff == null ? null
                                                : StaffAttendanceWeekMapper.map(today, staffAttendanceService.getMyHistory(start, end)));
        }

        private WidgetDTO<QuickActionsData> quickActions(Map<String, EffectivePermissionResponse> permissions) {
                List<QuickActionItem> items = new ArrayList<>();
                if (canViewAttendancePage(permissions, STUDENT_ATTENDANCE_MENU)
                                && hasPermission(permissions, ATTENDANCE_MENU, "MANAGE")
                                && hasPermission(permissions, STUDENT_ATTENDANCE_MENU, "MANAGE")) {
                        items.add(action("Mark Attendance", "pi-calendar-plus", "/app/attendance/students", "success"));
                }
                if (hasPermission(permissions, TIMETABLE_MENU, "VIEW")) {
                            items.add(action("My Timetable", "pi-clock", "/app/academics/my-timetable", "info"));
                }
                if (canViewAttendancePage(permissions, STAFF_ATTENDANCE_MENU)) {
                        items.add(action("Attendance Records", "pi-file", "/app/attendance/staff", "primary"));
                }
                if (canViewAttendancePage(permissions, ATTENDANCE_REPORTS_MENU)) {
                        items.add(action("Attendance Reports", "pi-chart-bar", "/app/attendance/reports", "danger"));
                }
                return safeWidget("quick-actions", WidgetType.QUICK_ACTIONS, "Quick Actions", 4, DataMode.LIVE,
                                () -> QuickActionsData.builder().items(items).build());
        }

            private WidgetDTO<TimetableData> todaysTimetable(
                    Map<String, EffectivePermissionResponse> permissions, List<TimetableSlotItem> todaySlots) {
                WidgetDTO<TimetableData> widget = safeWidget("todays-timetable", WidgetType.TIMETABLE,
                                "Today's Classes", 2, DataMode.LIVE, () -> TimetableData.builder()
                                                .dayLabel(LocalDate.now().getDayOfWeek().toString())
                                                .date(LocalDate.now())
                                                .slots(todaySlots)
                                                .build());
                if (hasPermission(permissions, TIMETABLE_MENU, "VIEW")) {
                        widget.setActionLabel("View Full Timetable");
                            widget.setActionRoute("/app/academics/my-timetable");
                }
                return widget;
        }

    private WidgetDTO<AnnouncementsData> announcements(
            User user, Staff staff, Map<String, EffectivePermissionResponse> permissions) {
        WidgetDTO<AnnouncementsData> widget = safeWidget("announcements", WidgetType.ANNOUNCEMENTS,
                "Recent Announcements", 2, DataMode.LIVE, () -> {
                    List<NoticeResponse> notices = user == null || staff == null ? List.of()
                            : noticeService.getRecentPublishedForStaff(user.getId(), staff.getStaffId(), 4);
                    return AnnouncementsData.builder()
                            .items(notices.stream().map(notice -> AnnouncementItem.builder()
                                    .title(notice.getTitle())
                                    .summary(trim(notice.getContent()))
                                    .category(notice.getCategory())
                                    .publishedAt(notice.getPublishDate())
                                    .pinned(notice.isPinned())
                                    .build()).toList())
                            .viewAllRoute(hasPermission(permissions, NOTICES_MENU, "VIEW")
                                    ? "/app/communication/notices" : null)
                            .build();
                });
        if (hasPermission(permissions, NOTICES_MENU, "VIEW")) {
            widget.setActionLabel("View All");
            widget.setActionRoute("/app/communication/notices");
        }
        return widget;
    }

    private WidgetDTO<CalendarData> upcomingEvents(Map<String, EffectivePermissionResponse> permissions) {
        boolean canView = hasPermission(permissions, CALENDAR_MENU, "VIEW");
        WidgetDTO<CalendarData> widget = safeWidget("upcoming-events", WidgetType.EVENTS,
                "Upcoming Events", 4, DataMode.LIVE, () -> CalendarData.builder()
                        .items(canView ? calendarService.upcoming(null, 4).stream()
                                .map(this::toCalendarEvent)
                                .toList() : List.of())
                        .viewAllRoute(canView ? "/app/academics/academic-calendar" : null)
                        .build());
        if (canView) {
            widget.setActionLabel("View All");
            widget.setActionRoute("/app/academics/academic-calendar");
        }
        return widget;
    }

    private CalendarEventItem toCalendarEvent(AcademicCalendarEventResponse event) {
        return CalendarEventItem.builder()
                .eventId(event.getEventId())
                .title(event.getTitle())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .startTime(event.getStartTime() != null ? event.getStartTime().toString() : null)
                .endTime(event.getEndTime() != null ? event.getEndTime().toString() : null)
                .location(event.getLocation())
                .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                .allDay(event.isAllDay())
                .build();
    }

        private boolean canViewAttendancePage(Map<String, EffectivePermissionResponse> permissions, String pageMenu) {
                return hasPermission(permissions, ATTENDANCE_MENU, "VIEW")
                                && hasPermission(permissions, pageMenu, "VIEW");
    }

        private Map<String, EffectivePermissionResponse> permissionSnapshot(User user) {
        Long organizationId = OrganizationContext.getOrganizationId();
                if (user == null || organizationId == null) return Map.of();
                return permissionService.getEffectivePermissions(user.getId(), organizationId).stream()
                                .filter(permission -> permission.getMenuCode() != null)
                                .collect(Collectors.toMap(EffectivePermissionResponse::getMenuCode, permission -> permission, (first, ignored) -> first));
        }

        private boolean hasPermission(Map<String, EffectivePermissionResponse> permissions, String menuCode, String privilege) {
                EffectivePermissionResponse permission = permissions.get(menuCode);
                if (permission == null) return false;
                return switch (privilege) {
                        case "VIEW" -> Boolean.TRUE.equals(permission.getCanView());
                        case "MANAGE" -> Boolean.TRUE.equals(permission.getCanManage());
                        case "APPROVE" -> Boolean.TRUE.equals(permission.getCanApprove());
                        default -> false;
                };
    }

        private QuickActionItem action(String label, String icon, String route, String tone) {
                return QuickActionItem.builder().label(label).icon(icon).route(route).tone(tone).build();
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

        private record SectionKey(Long classId, Long sectionId) {}

        private record SessionKey(SectionKey section, Long periodId) {}
}
