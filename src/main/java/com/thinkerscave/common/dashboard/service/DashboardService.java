package com.thinkerscave.common.dashboard.service;

import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.dashboard.dto.DashboardSummaryDTO;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import com.thinkerscave.common.enrollment.repository.AcademicEnrollmentRepository;
import com.thinkerscave.common.fee.domain.InvoiceStatus;
import com.thinkerscave.common.fee.repository.FeeInvoiceRepository;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates lightweight KPI counts for the organization dashboard.
 *
 * <p>Returns zero where the underlying repository cannot answer (e.g. when
 * no academic year is supplied). All counts are organization-scoped.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final AcademicEnrollmentRepository enrollmentRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;

    public DashboardSummaryDTO summary(Long academicYearId) {
        Long orgId = currentOrgId();
        long totalStudents = orgId == null ? 0L
                : safe(studentRepository.countByOrganizationId(orgId));
        long activeStudents = orgId == null ? 0L
                : studentRepository.findByOrganizationIdAndIsActive(orgId, true).size();

        List<Staff> staff = orgId == null ? List.of() : staffRepository.findByOrganizationId(orgId);
        long totalStaff = staff.size();
        long activeStaff = orgId == null ? 0L
                : staffRepository.findByOrganizationIdAndIsActive(orgId, Boolean.TRUE).size();

        long activeEnrollments = (orgId != null && academicYearId != null)
                ? enrollmentRepository.countByOrganizationIdAndAcademicYearIdAndStatus(
                        orgId, academicYearId, EnrollmentStatus.ACTIVE)
                : 0L;

        long unpaidInvoices = orgId == null ? 0L
                : feeInvoiceRepository.countByOrganizationIdAndStatus(orgId, InvoiceStatus.ISSUED)
                + feeInvoiceRepository.countByOrganizationIdAndStatus(orgId, InvoiceStatus.PARTIALLY_PAID);
        long overdueInvoices = orgId == null ? 0L
                : feeInvoiceRepository.countByOrganizationIdAndStatus(orgId, InvoiceStatus.OVERDUE);

        return DashboardSummaryDTO.builder()
                .organizationId(orgId)
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .totalStaff(totalStaff)
                .activeStaff(activeStaff)
                .activeEnrollments(activeEnrollments)
                .openInquiries(0L)
                .pendingAdmissions(0L)
                .unpaidInvoices(unpaidInvoices)
                .overdueInvoices(overdueInvoices)
                .build();
    }

    private static long safe(Long value) {
        return value == null ? 0L : value;
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
