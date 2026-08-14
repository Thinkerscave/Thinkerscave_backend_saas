package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicYearRequest;
import com.thinkerscave.academics.dto.request.RejectAcademicYearRequest;
import com.thinkerscave.academics.dto.response.AcademicYearDashboardResponse;
import com.thinkerscave.academics.dto.response.AcademicYearResponse;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AcademicYearService {

    AcademicYearDashboardResponse getDashboard();

    Page<AcademicYearResponse> search(String query, AcademicYearStatus status, Pageable pageable);

    AcademicYearResponse getById(Long id);

    AcademicYearResponse create(AcademicYearRequest request);

    AcademicYearResponse update(Long id, AcademicYearRequest request);

    AcademicYearResponse deactivate(Long id);

    AcademicYearResponse markReadyForApproval(Long id);

    AcademicYearResponse submitForApproval(Long id);

    AcademicYearResponse approve(Long id);

    AcademicYearResponse reject(Long id, RejectAcademicYearRequest request);

    AcademicYearResponse activate(Long id);
}
