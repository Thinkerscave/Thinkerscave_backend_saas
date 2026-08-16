package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.response.AcademicsOverviewResponse;

public interface AcademicsOverviewService {

    AcademicsOverviewResponse getOverview(Long yearId);
}
