package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.SubjectChoiceGroupRequest;
import com.thinkerscave.academics.dto.response.SubjectChoiceGroupResponse;
import com.thinkerscave.academics.dto.response.SubjectOptionResponse;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.shared.dto.PageResponse;

public interface SubjectChoiceGroupService {

    PageResponse<SubjectChoiceGroupResponse> getDirectory(
            String q, SubjectType subjectType, Boolean active, int page, int size);

    PageResponse<SubjectOptionResponse> getSubjectOptions(
            String q, SubjectType subjectType, int page, int size);

    SubjectChoiceGroupResponse getById(Long groupId);

    SubjectChoiceGroupResponse create(SubjectChoiceGroupRequest request);

    SubjectChoiceGroupResponse update(Long groupId, SubjectChoiceGroupRequest request);

    SubjectChoiceGroupResponse deactivate(Long groupId);

    SubjectChoiceGroupResponse activate(Long groupId);
}
