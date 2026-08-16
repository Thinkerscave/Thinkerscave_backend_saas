package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicYearTransitionRequest;
import com.thinkerscave.academics.dto.response.AcademicYearTransitionResponse;

import java.util.List;

public interface AcademicYearTransitionService {

    AcademicYearTransitionResponse create(Long sourceYearId, AcademicYearTransitionRequest request);

    List<AcademicYearTransitionResponse> listByYear(Long yearId);

    AcademicYearTransitionResponse start(Long transitionId);

    AcademicYearTransitionResponse approve(Long transitionId);
}
