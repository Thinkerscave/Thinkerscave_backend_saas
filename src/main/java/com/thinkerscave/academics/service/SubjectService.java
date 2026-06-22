package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;

import java.util.List;

public interface SubjectService {

    SubjectResponse create(SubjectRequest request);

    SubjectResponse update(Long subjectId, SubjectRequest request);

    SubjectResponse getById(Long subjectId);

    List<SubjectResponse> getAll();

    List<SubjectResponse> search(String keyword);

    void deactivate(Long subjectId);
}
