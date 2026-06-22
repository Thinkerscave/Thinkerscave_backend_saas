package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicYearRequest;
import com.thinkerscave.academics.dto.request.CloneAcademicYearRequest;
import com.thinkerscave.academics.dto.response.AcademicYearResponse;

import java.util.List;

public interface AcademicYearService {

    AcademicYearResponse create(AcademicYearRequest request);

    AcademicYearResponse update(Long id, AcademicYearRequest request);

    AcademicYearResponse getById(Long id);

    List<AcademicYearResponse> getAll();

    AcademicYearResponse setCurrentYear(Long id);

    AcademicYearResponse deactivate(Long id);

    AcademicYearResponse clone(Long id, CloneAcademicYearRequest request);
}
