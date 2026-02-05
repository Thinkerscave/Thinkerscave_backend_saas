package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.dto.SemesterDTO;
import java.util.List;

public interface SemesterService {

    SemesterDTO createSemester(SemesterDTO dto);

    SemesterDTO updateSemester(Long semesterId, SemesterDTO dto);

    SemesterDTO getSemester(Long semesterId);

    List<SemesterDTO> getSemestersByAcademicYear(Long academicYearId);

    List<SemesterDTO> getActiveSemesters(Long academicYearId);

    void setCurrentSemester(Long semesterId);

    SemesterDTO getCurrentSemester(Long academicYearId);

    void deleteSemester(Long semesterId);
}
