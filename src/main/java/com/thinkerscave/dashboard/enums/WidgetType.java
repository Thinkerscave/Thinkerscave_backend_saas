package com.thinkerscave.dashboard.enums;

/**
 * Every renderable widget kind. The frontend keys a single widget-registry
 * lookup off this enum — it never branches on user role. Adding a new
 * widget later means: one new value here, one provider method, one
 * frontend component.
 */
public enum WidgetType {

    WELCOME_HEADER,
    KPI_GRID,
    CHART,
    STAT_LIST,
    RECENT_ACTIVITY,
    PENDING_TASKS,
    NOTIFICATIONS,
    ANNOUNCEMENTS,
    CALENDAR,
    EVENTS,
    ATTENDANCE_SUMMARY,
    STAFF_ATTENDANCE_TOGGLE,
    ACADEMIC_SUMMARY,
    TIMETABLE,
    PROFILE_SUMMARY,
    CHILD_PROFILE,
    QUICK_ACTIONS,
    RECENT_RECORDS,
    REPORTS,
    SYSTEM_HEALTH,
    TOP_ORGANIZATIONS,
    LEAVE_SUMMARY,
    EXAMINATION_SUMMARY,
    LIBRARY_SUMMARY,
    TRANSPORT_SUMMARY,
    SUPPORT_TICKETS,
    PROMOTION_BANNER
}
