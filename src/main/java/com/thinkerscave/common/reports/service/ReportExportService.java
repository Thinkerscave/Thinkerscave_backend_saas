package com.thinkerscave.common.reports.service;

import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enrollment.domain.AcademicEnrollment;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import com.thinkerscave.common.enrollment.repository.AcademicEnrollmentRepository;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.fee.domain.FeeInvoice;
import com.thinkerscave.common.fee.domain.InvoiceStatus;
import com.thinkerscave.common.fee.repository.FeeInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * CSV export of common report datasets. Stays intentionally small; richer
 * formats (PDF, XLSX) can be added later behind the same interface.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReportExportService {

    private static final int DEFAULT_PAGE_SIZE = 1000;

    private final AcademicEnrollmentRepository enrollmentRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;

    public String enrollmentsCsv(Long academicYearId) {
        if (academicYearId == null) throw new BadRequestException("academicYearId is required");
        Long orgId = requireOrg();
        Pageable page = PageRequest.of(0, DEFAULT_PAGE_SIZE);
        List<AcademicEnrollment> rows = enrollmentRepository
                .findByOrganizationIdAndAcademicYearId(orgId, academicYearId, page)
                .getContent();
        StringBuilder sb = new StringBuilder(
                "enrollmentNumber,studentId,academicYearId,classId,sectionId,rollNumber,status,enrollmentDate,exitDate\n");
        for (AcademicEnrollment e : rows) {
            sb.append(csv(e.getEnrollmentNumber())).append(',')
                    .append(nullable(e.getStudentId())).append(',')
                    .append(nullable(e.getAcademicYearId())).append(',')
                    .append(nullable(e.getClassId())).append(',')
                    .append(nullable(e.getSectionId())).append(',')
                    .append(csv(e.getRollNumber())).append(',')
                    .append(csv(Objects.toString(e.getStatus(), ""))).append(',')
                    .append(nullable(e.getEnrollmentDate())).append(',')
                    .append(nullable(e.getExitDate())).append('\n');
        }
        return sb.toString();
    }

    public String invoicesCsv(InvoiceStatus status) {
        Long orgId = requireOrg();
        InvoiceStatus filter = status != null ? status : InvoiceStatus.ISSUED;
        Pageable page = PageRequest.of(0, DEFAULT_PAGE_SIZE);
        List<FeeInvoice> rows = feeInvoiceRepository
                .findByOrganizationIdAndStatus(orgId, filter, page).getContent();
        StringBuilder sb = new StringBuilder(
                "invoiceNumber,studentId,issueDate,dueDate,totalAmount,balanceAmount,status\n");
        for (FeeInvoice inv : rows) {
            sb.append(csv(inv.getInvoiceNumber())).append(',')
                    .append(nullable(inv.getStudentId())).append(',')
                    .append(nullable(inv.getIssueDate())).append(',')
                    .append(nullable(inv.getDueDate())).append(',')
                    .append(nullable(inv.getTotalAmount())).append(',')
                    .append(nullable(inv.getBalanceAmount())).append(',')
                    .append(csv(Objects.toString(inv.getStatus(), ""))).append('\n');
        }
        return sb.toString();
    }

    public long activeEnrollmentCount(Long academicYearId) {
        Long orgId = requireOrg();
        return enrollmentRepository.countByOrganizationIdAndAcademicYearIdAndStatus(
                orgId, academicYearId, EnrollmentStatus.ACTIVE);
    }

    private static String csv(String value) {
        if (value == null) return "";
        boolean needsQuote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needsQuote ? '"' + escaped + '"' : escaped;
    }

    private static String nullable(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long requireOrg() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) throw new BadRequestException("Organization context is required");
        return orgId;
    }
}
