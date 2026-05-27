package com.thinkerscave.common.reports.controller;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.fee.domain.InvoiceStatus;
import com.thinkerscave.common.reports.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "CSV report exports")
@RequiredArgsConstructor
@Slf4j
public class ReportExportController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportExportService reportExportService;
    private final AuditPublisher auditPublisher;

    @GetMapping("/enrollments.csv")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('REPORT_EXPORT')")
    @Operation(summary = "Export academic enrollments for a given academic year as CSV")
    public ResponseEntity<byte[]> enrollments(@RequestParam Long academicYearId) {
        byte[] body = reportExportService.enrollmentsCsv(academicYearId).getBytes(StandardCharsets.UTF_8);
        auditPublisher.publish(AuditEventType.EXPORT, "REPORT_EXPORT_ENROLLMENTS",
                "AcademicEnrollment", academicYearId, "Exported enrollments for year " + academicYearId);
        return csvResponse(body, "enrollments-year-" + academicYearId + ".csv");
    }

    @GetMapping("/invoices.csv")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('REPORT_EXPORT')")
    @Operation(summary = "Export fee invoices filtered by status as CSV")
    public ResponseEntity<byte[]> invoices(@RequestParam(required = false) InvoiceStatus status) {
        InvoiceStatus filter = status != null ? status : InvoiceStatus.ISSUED;
        byte[] body = reportExportService.invoicesCsv(filter).getBytes(StandardCharsets.UTF_8);
        auditPublisher.publish(AuditEventType.EXPORT, "REPORT_EXPORT_INVOICES",
                "FeeInvoice", filter.name(), "Exported invoices with status " + filter);
        return csvResponse(body, "invoices-" + filter.name().toLowerCase() + ".csv");
    }

    private ResponseEntity<byte[]> csvResponse(byte[] body, String filename) {
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
