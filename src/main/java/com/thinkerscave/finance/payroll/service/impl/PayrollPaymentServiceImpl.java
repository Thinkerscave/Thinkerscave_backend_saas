package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.entity.FeePaymentMethod;
import com.thinkerscave.finance.payroll.dto.request.PayrollPaymentRequest;
import com.thinkerscave.finance.payroll.dto.response.PayrollPaymentResponse;
import com.thinkerscave.finance.payroll.entity.EmployeePayroll;
import com.thinkerscave.finance.payroll.entity.PayrollPayment;
import com.thinkerscave.finance.payroll.entity.PayrollRun;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PayrollRunStatus;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.repository.PayrollPaymentRepository;
import com.thinkerscave.finance.payroll.repository.PayrollRunRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.PayrollPaymentService;
import com.thinkerscave.finance.repository.FeePaymentMethodRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.IdempotencyConflictException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollPaymentServiceImpl implements PayrollPaymentService {

    private final PayrollPaymentRepository paymentRepository;
    private final EmployeePayrollRepository employeePayrollRepository;
    private final PayrollRunRepository runRepository;
    private final FeePaymentMethodRepository paymentMethodRepository;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    @Transactional
    public PayrollPaymentResponse recordPayment(Long employeePayrollId, PayrollPaymentRequest request, String idempotencyKey) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_PAYMENT);
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String key = idempotencyKey.trim();
        var existing = paymentRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            PayrollPayment prior = existing.get();
            if (!prior.getEmployeePayroll().getEmployeePayrollId().equals(employeePayrollId)
                    || prior.getAmount().compareTo(request.getAmount()) != 0) {
                throw new IdempotencyConflictException("Idempotency key reused with different payment payload");
            }
            return toResponse(prior);
        }

        EmployeePayroll ep = employeePayrollRepository.findById(employeePayrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee payroll not found: " + employeePayrollId));
        if (ep.getStatus() != EmployeePayrollStatus.APPROVED
                && ep.getStatus() != EmployeePayrollStatus.PARTIALLY_PAID) {
            throw new BadRequestException("Payments allowed only on approved or partially paid payroll");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        BigDecimal paidSoFar = paymentRepository.sumByEmployeePayrollId(employeePayrollId);
        BigDecimal remaining = ep.getNetAmount().subtract(paidSoFar);
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new BadRequestException("Payment exceeds remaining net amount");
        }

        String methodName = request.getPaymentMethodName();
        if (request.getPaymentMethodId() != null) {
            FeePaymentMethod method = paymentMethodRepository.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
            methodName = method.getName();
        }

        PayrollPayment payment = new PayrollPayment();
        payment.setEmployeePayroll(ep);
        payment.setAmount(request.getAmount());
        payment.setPaidOn(request.getPaidOn());
        payment.setPaymentMethodId(request.getPaymentMethodId());
        payment.setPaymentMethodName(methodName);
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setRemarks(request.getRemarks());
        payment.setIdempotencyKey(key);
        payment = paymentRepository.save(payment);

        BigDecimal totalPaid = paidSoFar.add(request.getAmount());
        if (totalPaid.compareTo(ep.getNetAmount()) >= 0) {
            ep.setStatus(EmployeePayrollStatus.PAID);
        } else {
            ep.setStatus(EmployeePayrollStatus.PARTIALLY_PAID);
        }
        employeePayrollRepository.save(ep);
        refreshRunStatus(ep.getPayrollRun().getPayrollRunId());

        auditWriteService.record(AuditEventType.CREATE, "PAYROLL_PAYMENT", "PayrollPayment",
                String.valueOf(payment.getPayrollPaymentId()),
                "Recorded payroll payment of " + request.getAmount());
        return toResponse(payment);
    }

    private void refreshRunStatus(Long runId) {
        PayrollRun run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            return;
        }
        List<EmployeePayroll> employees = employeePayrollRepository.findByPayrollRun_PayrollRunId(runId);
        boolean allPaid = employees.stream().allMatch(e -> e.getStatus() == EmployeePayrollStatus.PAID);
        boolean anyPaid = employees.stream().anyMatch(e ->
                e.getStatus() == EmployeePayrollStatus.PAID || e.getStatus() == EmployeePayrollStatus.PARTIALLY_PAID);
        if (allPaid) {
            run.setStatus(PayrollRunStatus.PAID);
        } else if (anyPaid) {
            run.setStatus(PayrollRunStatus.PARTIALLY_PAID);
        }
        runRepository.save(run);
    }

    private PayrollPaymentResponse toResponse(PayrollPayment payment) {
        Long epId = payment.getEmployeePayroll().getEmployeePayrollId();
        EmployeePayroll ep = payment.getEmployeePayroll();
        BigDecimal totalPaid = paymentRepository.sumByEmployeePayrollId(epId);
        return PayrollPaymentResponse.builder()
                .payrollPaymentId(payment.getPayrollPaymentId())
                .employeePayrollId(epId)
                .amount(payment.getAmount())
                .paidOn(payment.getPaidOn())
                .paymentMethodId(payment.getPaymentMethodId())
                .paymentMethodName(payment.getPaymentMethodName())
                .referenceNumber(payment.getReferenceNumber())
                .remarks(payment.getRemarks())
                .totalPaid(totalPaid)
                .remaining(ep.getNetAmount().subtract(totalPaid).max(BigDecimal.ZERO))
                .employeePayrollStatus(ep.getStatus())
                .build();
    }
}
