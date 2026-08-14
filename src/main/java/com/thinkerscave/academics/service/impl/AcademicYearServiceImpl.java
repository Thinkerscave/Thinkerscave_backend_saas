package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicYearRequest;
import com.thinkerscave.academics.dto.request.CloneAcademicYearRequest;
import com.thinkerscave.academics.dto.response.AcademicYearResponse;
import com.thinkerscave.academics.service.AcademicYearService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Placeholder until the Academics rebuild service layer is implemented against the frozen schema.
 */
@Service
public class AcademicYearServiceImpl implements AcademicYearService {

    private static final String MSG = "Academics year API rebuild in progress";

    @Override
    public AcademicYearResponse create(AcademicYearRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicYearResponse update(Long id, AcademicYearRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicYearResponse getById(Long id) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<AcademicYearResponse> getAll() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicYearResponse setCurrentYear(Long id) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicYearResponse deactivate(Long id) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public AcademicYearResponse clone(Long id, CloneAcademicYearRequest request) {
        throw new UnsupportedOperationException(MSG);
    }
}
