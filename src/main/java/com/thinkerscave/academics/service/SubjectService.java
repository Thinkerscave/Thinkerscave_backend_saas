package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectUsageResponse;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.shared.dto.PageResponse;

/** Reusable subject catalog: directory, lifecycle, and usage. Curriculum mapping lives in {@link SubjectMappingService}. */
public interface SubjectService {

    PageResponse<SubjectResponse> getDirectory(String q, SubjectType subjectType, Boolean active, int page, int size);

    SubjectResponse create(SubjectRequest request);

    SubjectResponse update(Long subjectId, SubjectRequest request);

    SubjectResponse getById(Long subjectId);

    SubjectResponse deactivate(Long subjectId);

    SubjectResponse activate(Long subjectId);

    PageResponse<SubjectUsageResponse> getUsages(Long subjectId, String q, int page, int size);
}
