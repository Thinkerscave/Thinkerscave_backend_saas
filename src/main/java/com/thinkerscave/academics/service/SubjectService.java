package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicGroupSubjectMappingRequest;
import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.AcademicGroupMappingBoardResponse;
import com.thinkerscave.academics.dto.response.AcademicGroupSubjectMappingResponse;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectsMappingDashboardResponse;
import com.thinkerscave.academics.enums.SubjectCategory;

import java.util.List;

public interface SubjectService {

    SubjectsMappingDashboardResponse getDashboard(
            Long academicYearId, String q, SubjectCategory category, Boolean active);

    SubjectResponse create(SubjectRequest request);

    SubjectResponse update(Long subjectId, SubjectRequest request);

    SubjectResponse getById(Long subjectId);

    List<SubjectResponse> getByYear(Long academicYearId);

    SubjectResponse deactivate(Long subjectId);

    SubjectResponse activate(Long subjectId);

    AcademicGroupMappingBoardResponse getAcademicGroupMappingBoard(Long academicGroupId);

    AcademicGroupSubjectMappingResponse upsertAcademicGroupMapping(Long academicGroupId, AcademicGroupSubjectMappingRequest request);
}
