package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.finance.payroll.dto.response.EmployeePayrollDetailResponse;
import com.thinkerscave.finance.payroll.dto.response.MyPayrollSummaryResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollEmployeeRowResponse;
import com.thinkerscave.finance.payroll.entity.EmployeePayroll;
import com.thinkerscave.finance.payroll.entity.EmployeePayrollLine;
import com.thinkerscave.finance.payroll.entity.PayrollPayment;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollLineRepository;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.repository.PayrollPaymentRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.PayrollMeService;
import com.thinkerscave.finance.payroll.service.PayslipService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollMeServiceImpl implements PayrollMeService {

    private final EmployeePayrollRepository employeePayrollRepository;
    private final EmployeePayrollLineRepository lineRepository;
    private final PayrollPaymentRepository paymentRepository;
    private final PayslipService payslipService;
    private final PayrollAccessGuard accessGuard;

    @Override
    public MyPayrollSummaryResponse summary() {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_MY);
        Staff self = accessGuard.requireLinkedStaff();
        List<EmployeePayroll> history = employeePayrollRepository
                .findByStaffIdOrderByPayrollYearDescPayrollMonthDesc(self.getStaffId());
        EmployeePayroll latest = history.isEmpty() ? null : history.get(0);
        List<PayrollEmployeeRowResponse> recent = history.stream().limit(6).map(ep -> toRow(ep, self)).toList();
        return MyPayrollSummaryResponse.builder()
                .staffId(self.getStaffId())
                .staffName(self.getFirstName() + " " + self.getLastName())
                .latestYear(latest != null ? latest.getPayrollYear() : null)
                .latestMonth(latest != null ? latest.getPayrollMonth() : null)
                .latestNetAmount(latest != null ? latest.getNetAmount() : null)
                .latestStatus(latest != null ? latest.getStatus().name() : null)
                .recent(recent)
                .build();
    }

    @Override
    public PageResponse<PayrollEmployeeRowResponse> history(Pageable pageable) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_MY);
        Staff self = accessGuard.requireLinkedStaff();
        List<PayrollEmployeeRowResponse> all = employeePayrollRepository
                .findByStaffIdOrderByPayrollYearDescPayrollMonthDesc(self.getStaffId())
                .stream().map(ep -> toRow(ep, self)).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<PayrollEmployeeRowResponse> content = start >= all.size() ? List.of() : all.subList(start, end);
        Page<PayrollEmployeeRowResponse> page = new PageImpl<>(content, pageable, all.size());
        return PageResponse.of(page);
    }

    @Override
    public EmployeePayrollDetailResponse detail(Long employeePayrollId) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_MY);
        Staff self = accessGuard.requireLinkedStaff();
        EmployeePayroll ep = employeePayrollRepository.findById(employeePayrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee payroll not found"));
        accessGuard.assertSelfStaff(ep.getStaffId());
        return toDetail(ep, self);
    }

    @Override
    public byte[] payslip(Long employeePayrollId) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_MY);
        EmployeePayroll ep = employeePayrollRepository.findById(employeePayrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee payroll not found"));
        accessGuard.assertSelfStaff(ep.getStaffId());
        return payslipService.loadStoredPayslip(employeePayrollId);
    }

    private PayrollEmployeeRowResponse toRow(EmployeePayroll ep, Staff staff) {
        BigDecimal paid = paymentRepository.sumByEmployeePayrollId(ep.getEmployeePayrollId());
        return PayrollEmployeeRowResponse.builder()
                .staffId(ep.getStaffId())
                .staffCode(staff.getStaffCode())
                .staffName(staff.getFirstName() + " " + staff.getLastName())
                .employmentCategory(ep.getEmploymentCategory())
                .designation(ep.getDesignation())
                .paymentType(ep.getPaymentType())
                .employeePayrollId(ep.getEmployeePayrollId())
                .status(ep.getStatus())
                .displayStatus(ep.getStatus().name())
                .grossAmount(ep.getGrossAmount())
                .totalDeductions(ep.getTotalDeductions())
                .netAmount(ep.getNetAmount())
                .paidAmount(paid)
                .build();
    }

    private EmployeePayrollDetailResponse toDetail(EmployeePayroll ep, Staff staff) {
        List<EmployeePayrollLine> lines = lineRepository
                .findByEmployeePayroll_EmployeePayrollIdOrderBySortOrderAsc(ep.getEmployeePayrollId());
        List<PayrollPayment> payments = paymentRepository
                .findByEmployeePayroll_EmployeePayrollIdOrderByPaidOnAsc(ep.getEmployeePayrollId());
        BigDecimal paid = paymentRepository.sumByEmployeePayrollId(ep.getEmployeePayrollId());
        return EmployeePayrollDetailResponse.builder()
                .employeePayrollId(ep.getEmployeePayrollId())
                .payrollRunId(ep.getPayrollRun().getPayrollRunId())
                .staffId(ep.getStaffId())
                .staffCode(staff.getStaffCode())
                .staffName(staff.getFirstName() + " " + staff.getLastName())
                .payrollYear(ep.getPayrollYear())
                .payrollMonth(ep.getPayrollMonth())
                .paymentType(ep.getPaymentType())
                .employmentCategory(ep.getEmploymentCategory())
                .designation(ep.getDesignation())
                .workingDays(ep.getWorkingDays())
                .presentDays(ep.getPresentDays())
                .paidLeaveDays(ep.getPaidLeaveDays())
                .lopDays(ep.getLopDays())
                .grossAmount(ep.getGrossAmount())
                .totalDeductions(ep.getTotalDeductions())
                .netAmount(ep.getNetAmount())
                .paidAmount(paid)
                .status(ep.getStatus())
                .payslipDocumentId(ep.getPayslipDocumentId())
                .payslipNumber(ep.getPayslipNumber())
                .lines(lines.stream().map(l -> EmployeePayrollDetailResponse.Line.builder()
                        .employeePayrollLineId(l.getEmployeePayrollLineId())
                        .componentCode(l.getComponentCode())
                        .componentName(l.getComponentName())
                        .componentType(l.getComponentType())
                        .calculationMethod(l.getCalculationMethod())
                        .rateOrPercent(l.getRateOrPercent())
                        .amount(l.getAmount())
                        .sortOrder(l.getSortOrder())
                        .build()).toList())
                .payments(payments.stream().map(p -> EmployeePayrollDetailResponse.Payment.builder()
                        .payrollPaymentId(p.getPayrollPaymentId())
                        .amount(p.getAmount())
                        .paidOn(p.getPaidOn())
                        .paymentMethodId(p.getPaymentMethodId())
                        .paymentMethodName(p.getPaymentMethodName())
                        .referenceNumber(p.getReferenceNumber())
                        .remarks(p.getRemarks())
                        .build()).toList())
                .build();
    }
}
