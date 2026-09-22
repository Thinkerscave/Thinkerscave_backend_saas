package com.thinkerscave.finance.reports.controller;

import com.thinkerscave.finance.reports.dto.FinanceReportDtos.*;
import com.thinkerscave.finance.reports.service.FinanceReportExportService;
import com.thinkerscave.finance.reports.service.FinanceReportService;
import com.thinkerscave.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/finance/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FinanceReportController {
    private final FinanceReportService reportService;
    private final FinanceReportExportService exportService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Overview>> overview(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer recentLimit) {
        return ResponseEntity.ok(ApiResponse.success("Finance report", reportService.overview(
                academicYearId, period, from, to, recentLimit)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam String format,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Period period,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer recentLimit) {
        ExportFile file = exportService.export(format, academicYearId, period, from, to, recentLimit);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentLength(file.content().length)
                .body(file.content());
    }
}
