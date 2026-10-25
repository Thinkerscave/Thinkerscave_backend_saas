package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.SubjectMappingSaveRequest;
import com.thinkerscave.academics.dto.response.AvailableSubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingBoardResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingImpactResponse;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.shared.dto.PageResponse;

/** Shared curriculum mapping workspace: one academic group's subjects, weekly periods, and section coverage. */
public interface SubjectMappingService {

    SubjectMappingBoardResponse getBoard(Long academicGroupId);

    PageResponse<AvailableSubjectResponse> getAvailableSubjects(
            Long academicGroupId, String q, SubjectType subjectType, int page, int size);

    SubjectMappingImpactResponse preview(Long academicGroupId, SubjectMappingSaveRequest request);

    SubjectMappingBoardResponse save(Long academicGroupId, SubjectMappingSaveRequest request);
}
