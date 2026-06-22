package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.response.LookupDTO;

import java.util.List;

public interface AcademicsLookupService {

    List<LookupDTO> getActiveAcademicYears();

    List<LookupDTO> getClassesByYear(Long academicYearId);

    List<LookupDTO> getSectionsByClass(Long classId);

    List<LookupDTO> getActiveSubjects();

    List<LookupDTO> getSchedulesByYear(Long academicYearId);

    List<LookupDTO> getTemplatesBySchedule(Long scheduleId);
}
