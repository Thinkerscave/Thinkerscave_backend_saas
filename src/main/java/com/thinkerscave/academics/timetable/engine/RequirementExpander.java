package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.ClassSubjectMapping;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Lesson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RequirementExpander {

    /** One requirement per weekly period of every section subject that has an active teacher. */
    public List<SchedulingRequirement> expand(List<Lesson> lessons) {
        List<SchedulingRequirement> requirements = new ArrayList<>();
        int seq = 1;

        for (Lesson lesson : lessons) {
            ClassSubjectMapping mapping = lesson.mapping();
            AcademicSection section = lesson.section();

            int weeklyPeriods = lesson.weeklyPeriods();
            if (weeklyPeriods <= 0) continue;

            SubjectTimetablePreference pref = mapping.getTimetablePreference() == null
                    ? SubjectTimetablePreference.ANY : mapping.getTimetablePreference();

            Long defaultResourceId = section.getDefaultResource() == null
                    ? null : section.getDefaultResource().getAcademicResourceId();

            String staffName = TimetablePlanningData.staffName(lesson.teacher());

            for (int i = 0; i < weeklyPeriods; i++) {
                SchedulingRequirement req = new SchedulingRequirement();
                req.setRequirementId(String.format("REQ-%04d", seq++));
                req.setSectionId(section.getSectionId());
                req.setSectionName(TimetablePlanningData.sectionLabel(section));
                req.setClassId(section.getAcademicClass().getClassId());
                req.setClassName(section.getAcademicClass().getName());
                req.setClassSubjectMappingId(mapping.getClassSubjectMappingId());
                req.setSubjectId(mapping.getSubject().getSubjectId());
                req.setSubjectName(mapping.getSubject().getName());
                req.setWeeklyPeriods(weeklyPeriods);
                req.setPeriodIndex(i);
                req.setTeacherAllocationId(lesson.allocation().getTeacherAllocationId());
                req.setPrimaryStaffId(lesson.teacher().getStaffId());
                req.setPrimaryStaffName(staffName);
                req.setDefaultResourceId(defaultResourceId);
                req.setPreference(pref);
                requirements.add(req);
            }
        }
        return requirements;
    }
}
