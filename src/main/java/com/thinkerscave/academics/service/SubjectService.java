package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.ClassSubjectMappingRequest;
import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.ClassMappingBoardResponse;
import com.thinkerscave.academics.dto.response.ClassSubjectMappingResponse;
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

    ClassMappingBoardResponse getClassMappingBoard(Long classId);

    ClassSubjectMappingResponse upsertClassMapping(Long classId, ClassSubjectMappingRequest request);
}
