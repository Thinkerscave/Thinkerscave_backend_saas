package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.response.*;

public interface AcademicsMeService {

    TeacherMyAcademicGroupsResponse getTeacherMyAcademicGroups(Long academicYearId);

    MyTimetableResponse getMyTimetable(Long academicYearId);

    TeacherAcademicStructureResponse getTeacherStructure(Long academicYearId);

    StudentMyAcademicsResponse getStudentMyAcademics(Long academicYearId);
}
