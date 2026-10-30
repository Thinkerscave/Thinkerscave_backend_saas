package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.staff.entity.Staff;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of everything timetable readiness and generation need for one academic year,
 * loaded in a fixed number of queries so both always evaluate the same data.
 *
 * <p>A scheduling unit is a section when the group has sections, or the whole group when it
 * does not. Units have a stable synthetic id ({@code sectionId} or {@code -groupId}) used for
 * occupancy checks; the real section id is null for group-level units.</p>
 */
@Getter
@Builder
public class TimetablePlanningData {

    private final AcademicYear year;
    private final TimetableConfiguration configuration;
    private final List<SchedulePattern> patterns;
    private final Map<Long, List<TimetablePeriod>> slotsByPattern;
    private final List<GroupScheduleAssignment> assignments;
    private final List<FixedActivity> fixedActivities;
    private final List<Unit> units;
    private final Map<Long, Map<DayOfWeek, List<TimetablePeriod>>> scheduleByUnit;
    private final LocalDate referenceDate;
    private final List<DayOfWeek> workingDays;
    private final List<TimetablePeriod> allPeriods;
    private final List<TimetablePeriod> teachingPeriods;
    private final List<AcademicSection> sections;
    private final List<AcademicGroupSubjectMapping> mappings;
    private final List<SubjectChoiceGroupMember> activeChoiceGroupMembers;
    private final List<Lesson> lessons;
    private final List<MissingTeacher> missingTeachers;
    private final List<AcademicGroupSubjectMapping> zeroPeriodMappings;
    private final List<AcademicResource> resources;
    private final List<TimetableVersion> versions;

    public int getResourceCount() {
        return resources == null ? 0 : resources.size();
    }

    public int weeklySlotCount() {
        int total = 0;
        for (Map<DayOfWeek, List<TimetablePeriod>> byDay : scheduleByUnit.values()) {
            for (List<TimetablePeriod> slots : byDay.values()) {
                total += (int) slots.stream()
                        .filter(s -> s.getSlotKind() == com.thinkerscave.academics.enums.TimetableSlotKind.TEACHING)
                        .count();
            }
        }
        return total;
    }

    public List<TimetablePeriod> slotsFor(Long unitId, DayOfWeek day) {
        Map<DayOfWeek, List<TimetablePeriod>> byDay = scheduleByUnit.get(unitId);
        if (byDay == null) return List.of();
        return byDay.getOrDefault(day, List.of()).stream()
                .filter(s -> s.getSlotKind() == com.thinkerscave.academics.enums.TimetableSlotKind.TEACHING)
                .toList();
    }

    public boolean hasSchedule(Long unitId) {
        Map<DayOfWeek, List<TimetablePeriod>> byDay = scheduleByUnit.get(unitId);
        return byDay != null && byDay.values().stream().anyMatch(list -> !list.isEmpty());
    }

    public List<FixedActivity> fixedActivitiesFor(Unit unit) {
        List<FixedActivity> result = new ArrayList<>();
        for (FixedActivity activity : fixedActivities) {
            if (!activity.isActive() || !activity.isEffectiveOn(referenceDate)) continue;
            if (Boolean.TRUE.equals(activity.getAppliesToAllGroups())) {
                result.add(activity);
                continue;
            }
            boolean matches = activity.getGroups().stream().anyMatch(g ->
                    g.appliesTo(unit.group().getAcademicGroupId(),
                            unit.section() == null ? null : unit.section().getSectionId()));
            if (matches) result.add(activity);
        }
        return result;
    }

    public record Unit(Long unitId, AcademicSection section, AcademicGroup group) {
        public Long sectionId() {
            return section == null ? null : section.getSectionId();
        }

        public Long academicGroupId() {
            return group.getAcademicGroupId();
        }

        public String academicGroupName() {
            return group.getName();
        }

        public String sectionName() {
            return section == null ? null : section.getName();
        }

        public String label() {
            return section == null ? group.getName() : group.getName() + " - " + section.getName();
        }
    }

    public record Lesson(Unit unit,
                         AcademicGroupSubjectMapping mapping,
                         TeacherAllocation allocation,
                         Staff teacher) {
        public int weeklyPeriods() {
            return mapping.getWeeklyPeriods() == null ? 0 : mapping.getWeeklyPeriods();
        }
    }

    public enum MissingTeacherReason {
        NO_ALLOCATION("no teacher allocated"),
        UNASSIGNED("allocation is unassigned"),
        NO_PRIMARY_TEACHER("no active primary teacher"),
        TEACHER_INACTIVE("assigned teacher is no longer active");

        private final String label;

        MissingTeacherReason(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public record MissingTeacher(Unit unit,
                                 AcademicGroupSubjectMapping mapping,
                                 MissingTeacherReason reason,
                                 String teacherName) {
        public int weeklyPeriods() {
            return mapping.getWeeklyPeriods() == null ? 0 : mapping.getWeeklyPeriods();
        }
    }

    public static String sectionLabel(AcademicSection section) {
        String academicGroupName = section.getAcademicGroup() == null ? "" : section.getAcademicGroup().getName();
        return (academicGroupName + " - " + section.getName()).trim();
    }

    public static String staffName(Staff staff) {
        if (staff == null) return "";
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return ((staff.getFirstName() == null ? "" : staff.getFirstName()) + middle + " "
                + (staff.getLastName() == null ? "" : staff.getLastName())).trim();
    }
}
