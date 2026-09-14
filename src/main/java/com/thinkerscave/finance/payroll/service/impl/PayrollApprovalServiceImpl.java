package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.payroll.dto.response.PayrollRunResponse;
import com.thinkerscave.finance.payroll.entity.EmployeePayroll;
import com.thinkerscave.finance.payroll.entity.PayrollRun;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PayrollRunStatus;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.repository.PayrollPaymentRepository;
import com.thinkerscave.finance.payroll.repository.PayrollRunRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.PayrollApprovalService;
import com.thinkerscave.finance.payroll.service.PayrollGenerationService;
import com.thinkerscave.finance.payroll.service.PayslipService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollApprovalServiceImpl implements PayrollApprovalService {

    private final PayrollRunRepository runRepository;
    private final EmployeePayrollRepository employeePayrollRepository;
    private final PayrollPaymentRepository paymentRepository;
    private final PayslipService payslipService;
    private final PayrollGenerationService generationService;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    @Transactional
    public PayrollRunResponse approve(Long runId) {
        accessGuard.requireApprove(PayrollAccessGuard.RESOURCE_RUN);
        PayrollRun run = require(runId);
        if (run.getStatus() != PayrollRunStatus.PENDING_APPROVAL
                && run.getStatus() != PayrollRunStatus.GENERATED) {
            throw new BadRequestException("Only pending payroll runs can be approved");
        }
        List<EmployeePayroll> employees = employeePayrollRepository.findByPayrollRun_PayrollRunId(runId);
        for (EmployeePayroll ep : employees) {
            if (ep.getStatus() == EmployeePayrollStatus.PENDING_APPROVAL
                    || ep.getStatus() == EmployeePayrollStatus.GENERATED) {
                ep.setStatus(EmployeePayrollStatus.APPROVED);
                employeePayrollRepository.save(ep);
                payslipService.freezeAndStore(ep);
            }
        }
        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedOn(OffsetDateTime.now());
        run.setApprovedBy(accessGuard.currentUsernameOrNull());
        run.setReturnedOn(null);
        runRepository.save(run);

        auditWriteService.record(AuditEventType.UPDATE, "PAYROLL_APPROVE", "PayrollRun",
                String.valueOf(runId), "Approved payroll run");
        return generationService.getRun(runId);
    }

    @Override
    @Transactional
    public PayrollRunResponse returnForCorrection(Long runId) {
        accessGuard.requireApprove(PayrollAccessGuard.RESOURCE_RUN);
        PayrollRun run = require(runId);
        if (run.getStatus() != PayrollRunStatus.APPROVED) {
            throw new BadRequestException("Only approved unpaid runs can be returned");
        }
        List<EmployeePayroll> employees = employeePayrollRepository.findByPayrollRun_PayrollRunId(runId);
        for (EmployeePayroll ep : employees) {
            if (paymentRepository.existsByEmployeePayroll_EmployeePayrollId(ep.getEmployeePayrollId())) {
                throw new BadRequestException("Cannot return payroll after any payment has been recorded");
            }
        }
        for (EmployeePayroll ep : employees) {
            payslipService.clearPayslipIfUnpaid(ep);
            ep.setStatus(EmployeePayrollStatus.GENERATED);
            employeePayrollRepository.save(ep);
        }
        run.setStatus(PayrollRunStatus.GENERATED);
        run.setReturnedOn(OffsetDateTime.now());
        run.setApprovedOn(null);
        run.setApprovedBy(null);
        runRepository.save(run);

        auditWriteService.record(AuditEventType.UPDATE, "PAYROLL_RETURN", "PayrollRun",
                String.valueOf(runId), "Returned payroll run for correction");
        return generationService.getRun(runId);
    }

    private PayrollRun require(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
    }
}
