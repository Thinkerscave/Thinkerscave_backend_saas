package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.AdmissionReportFilterRequest;
import com.thinkerscave.admission.dto.response.AdmissionReportDashboardResponse;
import com.thinkerscave.admission.service.AdmissionReportService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admissions/reports")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Reports")
public class AdmissionsReportController {

    private final AdmissionReportService reportService;

    @PostMapping("/dashboard")
    @Operation(summary = "Filterable admissions management dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<AdmissionReportDashboardResponse>> dashboard(
            @RequestBody(required = false) AdmissionReportFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(
                "Admissions report loaded",
                reportService.dashboard(filter != null ? filter : new AdmissionReportFilterRequest())));
    }

    @PostMapping("/export")
    @Operation(summary = "Export admissions report as CSV")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<byte[]> export(@RequestBody(required = false) AdmissionReportFilterRequest filter) {
        byte[] csv = reportService.exportCsv(filter != null ? filter : new AdmissionReportFilterRequest());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admissions-report.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/overview")
    @Operation(summary = "Admissions overview metrics")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> overview() {
        return ResponseEntity.ok(ApiResponse.success("Overview report loaded", reportService.overview()));
    }

    @GetMapping("/funnel")
    @Operation(summary = "Admissions funnel report")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> funnel() {
        return ResponseEntity.ok(ApiResponse.success("Funnel report loaded", reportService.funnel()));
    }

    @GetMapping("/counselor-performance")
    @Operation(summary = "Counselor performance report")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> counselorPerformance() {
        return ResponseEntity.ok(ApiResponse.success("Counselor performance loaded", reportService.counselorPerformance()));
    }

    @GetMapping("/source-analysis")
    @Operation(summary = "Lead source analysis report")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> sourceAnalysis() {
        return ResponseEntity.ok(ApiResponse.success("Source analysis loaded", reportService.sourceAnalysis()));
    }
}
