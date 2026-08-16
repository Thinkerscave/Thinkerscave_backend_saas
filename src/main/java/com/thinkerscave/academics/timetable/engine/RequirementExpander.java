package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class RequirementExpander {

    public List<SchedulingRequirement> expand(SchedulingSourceData source) {
        List<SchedulingRequirement> requirements = new ArrayList<>();
        int seq = 1;

        for (TeacherAllocationTeacher primary : source.getActivePrimaries()) {
            if (primary.getRole() != TeacherAllocationTeacherRole.PRIMARY) continue;

            TeacherAllocation allocation = primary.getTeacherAllocation();
            ClassSubjectMapping mapping = allocation.getClassSubjectMapping();
            AcademicSection section = allocation.getSection();

            int weeklyPeriods = mapping.getWeeklyPeriods() == null ? 0 : mapping.getWeeklyPeriods();
            if (weeklyPeriods <= 0) continue;

            SubjectTimetablePreference pref = mapping.getTimetablePreference() == null
                    ? SubjectTimetablePreference.ANY : mapping.getTimetablePreference();

            Long defaultResourceId = section.getDefaultResource() == null
                    ? null : section.getDefaultResource().getAcademicResourceId();

            String staffName = buildStaffName(primary.getStaff());

            for (int i = 0; i < weeklyPeriods; i++) {
                SchedulingRequirement req = new SchedulingRequirement();
                req.setRequirementId(String.format("REQ-%04d", seq++));
                req.setSectionId(section.getSectionId());
                req.setSectionName(section.getName());
                req.setClassId(section.getAcademicClass().getClassId());
                req.setClassName(section.getAcademicClass().getName());
                req.setClassSubjectMappingId(mapping.getClassSubjectMappingId());
                req.setSubjectId(mapping.getSubject().getSubjectId());
                req.setSubjectName(mapping.getSubject().getName());
                req.setWeeklyPeriods(weeklyPeriods);
                req.setPeriodIndex(i);
                req.setTeacherAllocationId(allocation.getTeacherAllocationId());
                req.setPrimaryStaffId(primary.getStaff().getStaffId());
                req.setPrimaryStaffName(staffName);
                req.setDefaultResourceId(defaultResourceId);
                req.setPreference(pref);
                requirements.add(req);
            }
        }
        return requirements;
    }

    private String buildStaffName(com.thinkerscave.staff.entity.Staff staff) {
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return (staff.getFirstName() + middle + " " + staff.getLastName()).trim();
    }
}
