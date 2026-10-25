package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.staff.entity.Staff;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Snapshot of everything timetable readiness and generation need for one academic year,
 * loaded in a fixed number of queries so both always evaluate the same data.
 */
@Getter
@Builder
public class TimetablePlanningData {

    private final AcademicYear year;
    private final TimetableConfiguration configuration;
    private final List<TimetableConfiguration> activeConfigurations;
    private final List<DayOfWeek> workingDays;
    private final List<TimetablePeriod> allPeriods;
    private final List<TimetablePeriod> teachingPeriods;
    private final List<AcademicSection> sections;
    private final List<AcademicGroupSubjectMapping> mappings;
    private final List<Lesson> lessons;
    private final List<MissingTeacher> missingTeachers;
    private final List<AcademicGroupSubjectMapping> zeroPeriodMappings;
    private final List<AcademicResource> resources;
    private final List<TimetableVersion> versions;

    public int getResourceCount() {
        return resources == null ? 0 : resources.size();
    }

    public int weeklySlotCount() {
        return workingDays.size() * teachingPeriods.size();
    }

    public record Lesson(AcademicSection section,
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

    public record MissingTeacher(AcademicSection section,
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
