package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.response.LookupDTO;

import java.util.List;

public interface AcademicsLookupService {

    List<LookupDTO> getActiveAcademicYears();

    List<LookupDTO> getAcademicGroupsByYear(Long academicYearId);

    default List<LookupDTO> getClassesByYear(Long academicYearId) {
        return getAcademicGroupsByYear(academicYearId);
    }

    List<LookupDTO> getSectionsByAcademicGroup(Long academicGroupId);

    default List<LookupDTO> getSectionsByClass(Long classId) {
        return getSectionsByAcademicGroup(classId);
    }

    List<LookupDTO> getActiveSubjects();

    List<LookupDTO> getSchedulesByYear(Long academicYearId);

    List<LookupDTO> getTemplatesBySchedule(Long scheduleId);
}
