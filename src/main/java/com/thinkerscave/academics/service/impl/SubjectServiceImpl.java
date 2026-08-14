package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.service.SubjectService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectServiceImpl implements SubjectService {

    private static final String MSG = "Academics subject API rebuild in progress";

    @Override
    public SubjectResponse create(SubjectRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public SubjectResponse update(Long subjectId, SubjectRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public SubjectResponse getById(Long subjectId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<SubjectResponse> getAll() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<SubjectResponse> search(String keyword) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void deactivate(Long subjectId) {
        throw new UnsupportedOperationException(MSG);
    }
}
