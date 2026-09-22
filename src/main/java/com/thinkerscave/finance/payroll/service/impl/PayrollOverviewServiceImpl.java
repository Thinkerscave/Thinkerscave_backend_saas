package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.finance.payroll.dto.response.EmployeePayrollDetailResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollEmployeeRowResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollOverviewResponse;
import com.thinkerscave.finance.payroll.entity.EmployeePayroll;
import com.thinkerscave.finance.payroll.entity.EmployeeSalary;
import com.thinkerscave.finance.payroll.entity.PayrollRun;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryRepository;
import com.thinkerscave.finance.payroll.repository.PayrollPaymentRepository;
import com.thinkerscave.finance.payroll.repository.PayrollRunRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.PayrollOverviewService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollOverviewServiceImpl implements PayrollOverviewService {

    private final EmployeePayrollRepository employeePayrollRepository;
    private final PayrollRunRepository runRepository;
    private final PayrollPaymentRepository paymentRepository;
    private final EmployeeSalaryRepository employeeSalaryRepository;
    private final StaffRepository staffRepository;
    private final PayrollAccessGuard accessGuard;

    @Override
    public PayrollOverviewResponse overview(Integer year, Integer month) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_PAYROLL);
        validate(year, month);
        long totalStaff = staffRepository.countByActive(true);
        long generated = employeePayrollRepository.countByPayrollYearAndPayrollMonth(year, month);
        long pending = employeePayrollRepository.countByPayrollYearAndPayrollMonthAndStatus(year, month, EmployeePayrollStatus.PENDING_APPROVAL);
        long approved = employeePayrollRepository.countByPayrollYearAndPayrollMonthAndStatus(year, month, EmployeePayrollStatus.APPROVED);
        long partial = employeePayrollRepository.countByPayrollYearAndPayrollMonthAndStatus(year, month, EmployeePayrollStatus.PARTIALLY_PAID);
        long paid = employeePayrollRepository.countByPayrollYearAndPayrollMonthAndStatus(year, month, EmployeePayrollStatus.PAID);

        BigDecimal totalNet = employeePayrollRepository.sumNetByYearMonth(year, month);
        BigDecimal totalPaid = paymentRepository.sumByYearMonth(year, month);
        if (totalNet == null) totalNet = BigDecimal.ZERO;
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        PayrollRun run = runRepository.findByPayrollYearAndPayrollMonth(year, month).orElse(null);
        String label = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

        return PayrollOverviewResponse.builder()
                .year(year)
                .month(month)
                .periodLabel(label)
                .totalStaff(totalStaff)
                .generatedCount(generated)
                .pendingApprovalCount(pending)
                .approvedCount(approved)
                .partiallyPaidCount(partial)
                .paidCount(paid)
                .notGeneratedCount(Math.max(0, totalStaff - generated))
                .totalNetAmount(totalNet)
                .totalPaidAmount(totalPaid)
                .runId(run != null ? run.getPayrollRunId() : null)
                .runStatus(run != null ? run.getStatus().name() : null)
                .build();
    }

    @Override
    public PageResponse<PayrollEmployeeRowResponse> employees(Integer year, Integer month,
                                                              EmploymentCategory employmentCategory,
                                                              PaymentType paymentType,
                                                              EmployeePayrollStatus status,
                                                              String q,
                                                              Pageable pageable) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_PAYROLL);
        validate(year, month);

        Map<Long, Staff> staffById = staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE)
                .stream().collect(Collectors.toMap(Staff::getStaffId, Function.identity()));

        List<PayrollEmployeeRowResponse> all = new ArrayList<>();
        for (Staff staff : staffById.values()) {
            if (employmentCategory != null && staff.getEmploymentCategory() != employmentCategory) {
                continue;
            }
            if (StringUtils.hasText(q)) {
                String qq = q.trim().toLowerCase(Locale.ROOT);
                String name = (staff.getFirstName() + " " + staff.getLastName()).toLowerCase(Locale.ROOT);
                String code = staff.getStaffCode() != null ? staff.getStaffCode().toLowerCase(Locale.ROOT) : "";
                if (!name.contains(qq) && !code.contains(qq)) {
                    continue;
                }
            }
            EmployeePayroll ep = employeePayrollRepository
                    .findByStaffIdAndPayrollYearAndPayrollMonth(staff.getStaffId(), year, month)
                    .orElse(null);
            if (status != null) {
                if (ep == null || ep.getStatus() != status) {
                    continue;
                }
            }
            if (paymentType != null) {
                if (ep != null && ep.getPaymentType() != paymentType) {
                    continue;
                }
                if (ep == null) {
                    var salary = employeeSalaryRepository
                            .findFirstByStaffIdAndActiveTrueOrderByEffectiveFromDescEmployeeSalaryIdDesc(staff.getStaffId())
                            .orElse(null);
                    if (salary == null || salary.getPaymentType() != paymentType) {
                        continue;
                    }
                }
            }

            BigDecimal paid = ep != null ? paymentRepository.sumByEmployeePayrollId(ep.getEmployeePayrollId()) : BigDecimal.ZERO;
            PaymentType rowPaymentType = ep != null
                    ? ep.getPaymentType()
                    : employeeSalaryRepository
                            .findFirstByStaffIdAndActiveTrueOrderByEffectiveFromDescEmployeeSalaryIdDesc(staff.getStaffId())
                            .map(EmployeeSalary::getPaymentType)
                            .orElse(null);
            all.add(PayrollEmployeeRowResponse.builder()
                    .staffId(staff.getStaffId())
                    .staffCode(staff.getStaffCode())
                    .staffName(staff.getFirstName() + " " + staff.getLastName())
                    .employmentCategory(staff.getEmploymentCategory() != null ? staff.getEmploymentCategory().name() : null)
                    .designation(staff.getDesignation())
                    .paymentType(rowPaymentType)
                    .employeePayrollId(ep != null ? ep.getEmployeePayrollId() : null)
                    .status(ep != null ? ep.getStatus() : null)
                    .displayStatus(ep != null ? ep.getStatus().name() : "NOT_GENERATED")
                    .grossAmount(ep != null ? ep.getGrossAmount() : null)
                    .totalDeductions(ep != null ? ep.getTotalDeductions() : null)
                    .netAmount(ep != null ? ep.getNetAmount() : null)
                    .paidAmount(paid)
                    .build());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<PayrollEmployeeRowResponse> pageContent = start >= all.size() ? List.of() : all.subList(start, end);
        return PageResponse.of(new PageImpl<>(pageContent, pageable, all.size()));
    }

    @Override
    public PageResponse<EmployeePayrollDetailResponse> payrollHistory(Long staffId, Pageable pageable) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_PAYROLL);
        if (staffId == null) {
            throw new BadRequestException("staffId is required");
        }
        staffRepository.findById(staffId)
                .orElseThrow(() -> new BadRequestException("Staff not found: " + staffId));
        List<EmployeePayroll> rows = employeePayrollRepository
                .findByStaffIdOrderByPayrollYearDescPayrollMonthDesc(staffId);
        List<EmployeePayrollDetailResponse> mapped = rows.stream().map(ep -> {
            BigDecimal paid = paymentRepository.sumByEmployeePayrollId(ep.getEmployeePayrollId());
            return EmployeePayrollDetailResponse.builder()
                    .employeePayrollId(ep.getEmployeePayrollId())
                    .payrollRunId(ep.getPayrollRun() != null ? ep.getPayrollRun().getPayrollRunId() : null)
                    .staffId(ep.getStaffId())
                    .payrollYear(ep.getPayrollYear())
                    .payrollMonth(ep.getPayrollMonth())
                    .paymentType(ep.getPaymentType())
                    .grossAmount(ep.getGrossAmount())
                    .totalDeductions(ep.getTotalDeductions())
                    .netAmount(ep.getNetAmount())
                    .paidAmount(paid != null ? paid : BigDecimal.ZERO)
                    .status(ep.getStatus())
                    .payslipDocumentId(ep.getPayslipDocumentId())
                    .payslipNumber(ep.getPayslipNumber())
                    .build();
        }).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mapped.size());
        List<EmployeePayrollDetailResponse> pageContent = start >= mapped.size() ? List.of() : mapped.subList(start, end);
        return PageResponse.of(new PageImpl<>(pageContent, pageable, mapped.size()));
    }

    private static void validate(Integer year, Integer month) {
        if (year == null || month == null || month < 1 || month > 12) {
            throw new BadRequestException("Valid year and month are required");
        }
    }
}
