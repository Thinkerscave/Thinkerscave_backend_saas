package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Lesson;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Unit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RequirementExpander {

    /** One requirement per weekly period of every scheduling-unit subject that has a teacher. */
    public List<SchedulingRequirement> expand(List<Lesson> lessons) {
        List<SchedulingRequirement> requirements = new ArrayList<>();
        int seq = 1;

        for (Lesson lesson : lessons) {
            AcademicGroupSubjectMapping mapping = lesson.mapping();
            Unit unit = lesson.unit();

            int weeklyPeriods = lesson.weeklyPeriods();
            if (weeklyPeriods <= 0) continue;

            SubjectTimetablePreference pref = mapping.getTimetablePreference() == null
                    || mapping.getTimetablePreference() == SubjectTimetablePreference.ANY
                    ? (mapping.getSubject().getDefaultTimetablePreference() == null
                    ? SubjectTimetablePreference.ANY : mapping.getSubject().getDefaultTimetablePreference())
                    : mapping.getTimetablePreference();

            Long defaultResourceId = unit.section() == null || unit.section().getDefaultResource() == null
                    ? null : unit.section().getDefaultResource().getAcademicResourceId();

            String staffName = TimetablePlanningData.staffName(lesson.teacher());

            for (int i = 0; i < weeklyPeriods; i++) {
                SchedulingRequirement req = new SchedulingRequirement();
                req.setRequirementId(String.format("REQ-%04d", seq++));
                req.setUnitId(unit.unitId());
                req.setSectionId(unit.sectionId());
                req.setSectionName(unit.label());
                req.setAcademicGroupId(unit.academicGroupId());
                req.setAcademicGroupName(unit.academicGroupName());
                req.setAcademicGroupSubjectMappingId(mapping.getAcademicGroupSubjectMappingId());
                req.setSubjectId(mapping.getSubject().getSubjectId());
                req.setSubjectName(mapping.getSubject().getName());
                req.setWeeklyPeriods(weeklyPeriods);
                req.setPeriodIndex(i);
                req.setTeacherAllocationId(lesson.allocation() == null ? null : lesson.allocation().getTeacherAllocationId());
                req.setPrimaryStaffId(lesson.teacher().getStaffId());
                req.setPrimaryStaffName(staffName);
                req.setDefaultResourceId(defaultResourceId);
                req.setPreference(pref);
                req.setOptional(mapping.getRequirement() == SubjectRequirement.OPTIONAL);
                req.setConsecutivePeriods(mapping.getConsecutivePeriods() == null ? 1 : mapping.getConsecutivePeriods());
                requirements.add(req);
            }
        }
        return requirements;
    }
}
