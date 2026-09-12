package com.thinkerscave.admission.service;

import com.thinkerscave.admission.dto.request.AdmissionReportFilterRequest;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse;

import java.util.List;
import java.util.Map;

public interface AdmissionReportService {

    /** Legacy unfiltered overview (Overview page). */
    Map<String, Object> overview();

    Map<String, Long> funnel();

    List<Map<String, Object>> counselorPerformance();

    Map<String, Long> sourceAnalysis();

    /** Filterable management dashboard — single source of truth for the Reports page. */
    AdmissionReportDashboardResponse dashboard(AdmissionReportFilterRequest filter);

    byte[] exportCsv(AdmissionReportFilterRequest filter);
}
