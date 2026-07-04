package com.thinkerscave.admission.service;

import java.util.List;
import java.util.Map;

public interface AdmissionReportService {

    Map<String, Object> overview();

    Map<String, Long> funnel();

    List<Map<String, Object>> counselorPerformance();

    Map<String, Long> sourceAnalysis();
}