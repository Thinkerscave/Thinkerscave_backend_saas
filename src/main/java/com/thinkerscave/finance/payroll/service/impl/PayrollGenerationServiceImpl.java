package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.payroll.dto.request.PayrollGenerateRequest;
import com.thinkerscave.finance.payroll.dto.response.PayrollEmployeeRowResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollGenerateResult;
import com.thinkerscave.finance.payroll.dto.response.PayrollRunResponse;
import com.thinkerscave.finance.payroll.entity.EmployeePayroll;
import com.thinkerscave.finance.payroll.entity.EmployeePayrollLine;
import com.thinkerscave.finance.payroll.entity.EmployeeSalary;
import com.thinkerscave.finance.payroll.entity.EmployeeSalaryComponent;
import com.thinkerscave.finance.payroll.entity.PayrollConfiguration;
import com.thinkerscave.finance.payroll.entity.PayrollRun;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PayrollRunStatus;
import com.thinkerscave.finance.payroll.enums.WorkingDaysBasis;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollLineRepository;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryComponentRepository;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryRepository;
import com.thinkerscave.finance.payroll.repository.PayrollPaymentRepository;
import com.thinkerscave.finance.payroll.repository.PayrollRunRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.PayrollCalculationService;
import com.thinkerscave.finance.payroll.service.PayrollGenerationService;
import com.thinkerscave.finance.payroll.service.PayrollSettingsService;
import com.thinkerscave.finance.payroll.service.PayslipService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.IdempotencyConflictException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollGenerationServiceImpl implements PayrollGenerationService {

    private final PayrollRunRepository runRepository;
    private final EmployeePayrollRepository employeePayrollRepository;
    private final EmployeePayrollLineRepository lineRepository;
    private final EmployeeSalaryRepository employeeSalaryRepository;
    private final EmployeeSalaryComponentRepository salaryComponentRepository;
    private final PayrollPaymentRepository paymentRepository;
    private final StaffRepository staffRepository;
    private final PayrollSettingsService settingsService;
    private final PayslipService payslipService;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    @Transactional
    public PayrollGenerateResult generate(PayrollGenerateRequest request, String idempotencyKey) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_RUN);
        validatePeriod(request.getYear(), request.getMonth());
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String key = idempotencyKey.trim();
        var byKey = runRepository.findByIdempotencyKey(key);
        if (byKey.isPresent()) {
            PayrollRun prior = byKey.get();
            if (!Objects.equals(prior.getPayrollYear(), request.getYear())
                    || !Objects.equals(prior.getPayrollMonth(), request.getMonth())) {
                throw new IdempotencyConflictException("Idempotency key reused with different period");
            }
            return resultFromRun(prior, List.of());
        }

        var existingPeriod = runRepository.findByPayrollYearAndPayrollMonth(request.getYear(), request.getMonth());
        if (existingPeriod.isPresent()) {
            PayrollRun prior = existingPeriod.get();
            if (StringUtils.hasText(prior.getIdempotencyKey()) && !key.equals(prior.getIdempotencyKey())) {
                throw new IdempotencyConflictException("Payroll already generated for period with a different idempotency key");
            }
            if (!StringUtils.hasText(prior.getIdempotencyKey())) {
                prior.setIdempotencyKey(key);
                runRepository.save(prior);
            }
            return resultFromRun(prior, List.of());
        }

        PayrollConfiguration cfg = settingsService.requireConfig();
        PayrollRun run = new PayrollRun();
        run.setPayrollYear(request.getYear());
        run.setPayrollMonth(request.getMonth());
        run.setStatus(PayrollRunStatus.PROCESSING);
        run.setGeneratedOn(OffsetDateTime.now());
        run.setIdempotencyKey(key);
        run = runRepository.save(run);

        List<PayrollGenerateResult.SkipInfo> skipped = new ArrayList<>();
        int generated = processEmployees(run, cfg, request, skipped, false);

        boolean approvalRequired = Boolean.TRUE.equals(cfg.getApprovalRequired());
        run.setStatus(approvalRequired ? PayrollRunStatus.PENDING_APPROVAL : PayrollRunStatus.APPROVED);
        if (!approvalRequired) {
            run.setApprovedOn(OffsetDateTime.now());
            run.setApprovedBy("SYSTEM_AUTO");
        }
        run = runRepository.save(run);

        auditWriteService.record(AuditEventType.CREATE, "PAYROLL_GENERATE", "PayrollRun",
                String.valueOf(run.getPayrollRunId()),
                "Generated payroll " + request.getYear() + "-" + request.getMonth());

        return PayrollGenerateResult.builder()
                .runId(run.getPayrollRunId())
                .status(run.getStatus().name())
                .generatedCount(generated)
                .skippedCount(skipped.size())
                .skipped(skipped)
                .build();
    }

    @Override
    @Transactional
    public PayrollGenerateResult recalculate(Long runId, PayrollGenerateRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_RUN);
        PayrollRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
        if (run.getStatus() != PayrollRunStatus.GENERATED
                && run.getStatus() != PayrollRunStatus.PENDING_APPROVAL
                && run.getStatus() != PayrollRunStatus.PROCESSING) {
            throw new BadRequestException("Recalculate only allowed before approval");
        }
        List<EmployeePayroll> existing = employeePayrollRepository.findByPayrollRun_PayrollRunId(runId);
        for (EmployeePayroll ep : existing) {
            if (paymentRepository.existsByEmployeePayroll_EmployeePayrollId(ep.getEmployeePayrollId())) {
                throw new BadRequestException("Cannot recalculate after payments have been recorded");
            }
        }

        PayrollGenerateRequest effective = request != null ? request : new PayrollGenerateRequest();
        if (effective.getYear() == null) {
            effective.setYear(run.getPayrollYear());
        }
        if (effective.getMonth() == null) {
            effective.setMonth(run.getPayrollMonth());
        }

        for (EmployeePayroll ep : existing) {
            lineRepository.deleteByEmployeePayroll_EmployeePayrollId(ep.getEmployeePayrollId());
            payslipService.clearPayslipIfUnpaid(ep);
            employeePayrollRepository.delete(ep);
        }

        PayrollConfiguration cfg = settingsService.requireConfig();
        List<PayrollGenerateResult.SkipInfo> skipped = new ArrayList<>();
        int generated = processEmployees(run, cfg, effective, skipped, true);

        boolean approvalRequired = Boolean.TRUE.equals(cfg.getApprovalRequired());
        run.setStatus(approvalRequired ? PayrollRunStatus.PENDING_APPROVAL : PayrollRunStatus.APPROVED);
        run.setGeneratedOn(OffsetDateTime.now());
        run.setReturnedOn(null);
        if (!approvalRequired) {
            run.setApprovedOn(OffsetDateTime.now());
            run.setApprovedBy("SYSTEM_AUTO");
        } else {
            run.setApprovedOn(null);
            run.setApprovedBy(null);
        }
        runRepository.save(run);

        auditWriteService.record(AuditEventType.UPDATE, "PAYROLL_RECALCULATE", "PayrollRun",
                String.valueOf(runId), "Recalculated payroll run");

        return PayrollGenerateResult.builder()
                .runId(run.getPayrollRunId())
                .status(run.getStatus().name())
                .generatedCount(generated)
                .skippedCount(skipped.size())
                .skipped(skipped)
                .build();
    }

    @Override
    public PayrollRunResponse getRun(Long runId) {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_RUN);
        PayrollRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
        List<PayrollEmployeeRowResponse> employees = employeePayrollRepository.findByPayrollRun_PayrollRunId(runId)
                .stream().map(this::toRow).toList();
        return PayrollRunResponse.builder()
                .payrollRunId(run.getPayrollRunId())
                .payrollYear(run.getPayrollYear())
                .payrollMonth(run.getPayrollMonth())
                .status(run.getStatus())
                .generatedOn(run.getGeneratedOn())
                .approvedOn(run.getApprovedOn())
                .approvedBy(run.getApprovedBy())
                .returnedOn(run.getReturnedOn())
                .notes(run.getNotes())
                .employees(employees)
                .build();
    }

    private int processEmployees(PayrollRun run, PayrollConfiguration cfg, PayrollGenerateRequest request,
                                 List<PayrollGenerateResult.SkipInfo> skipped, boolean force) {
        YearMonth ym = YearMonth.of(run.getPayrollYear(), run.getPayrollMonth());
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd = ym.atEndOfMonth();
        int workingDays = cfg.getWorkingDaysBasis() == WorkingDaysBasis.CALENDAR
                ? ym.lengthOfMonth() : 26;

        Set<Long> scope = request.getStaffIds() != null && !request.getStaffIds().isEmpty()
                ? new HashSet<>(request.getStaffIds()) : null;
        Map<Long, Integer> lopOverrides = request.getLopOverrides() != null ? request.getLopOverrides() : Map.of();

        List<Staff> staffList = staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE).stream()
                .filter(s -> scope == null || scope.contains(s.getStaffId()))
                .toList();

        boolean approvalRequired = Boolean.TRUE.equals(cfg.getApprovalRequired());
        EmployeePayrollStatus empStatus = approvalRequired
                ? EmployeePayrollStatus.PENDING_APPROVAL
                : EmployeePayrollStatus.APPROVED;

        int generated = 0;
        for (Staff staff : staffList) {
            if (!force && employeePayrollRepository
                    .findByStaffIdAndPayrollYearAndPayrollMonth(staff.getStaffId(), run.getPayrollYear(), run.getPayrollMonth())
                    .isPresent()) {
                skipped.add(PayrollGenerateResult.SkipInfo.builder()
                        .staffId(staff.getStaffId()).reason("ALREADY_GENERATED").build());
                continue;
            }
            List<EmployeeSalary> covering = employeeSalaryRepository.findCoveringPeriod(
                    staff.getStaffId(), periodStart, periodEnd);
            if (covering.isEmpty()) {
                skipped.add(PayrollGenerateResult.SkipInfo.builder()
                        .staffId(staff.getStaffId()).reason("NO_SALARY_CONFIG").build());
                continue;
            }
            EmployeeSalary salary = covering.get(0);
            List<EmployeeSalaryComponent> components = salaryComponentRepository
                    .findByEmployeeSalary_EmployeeSalaryId(salary.getEmployeeSalaryId());
            if (components.isEmpty()) {
                skipped.add(PayrollGenerateResult.SkipInfo.builder()
                        .staffId(staff.getStaffId()).reason("NO_SALARY_LINES").build());
                continue;
            }

            Integer lop = lopOverrides.getOrDefault(staff.getStaffId(), 0);
            List<PayrollCalculationService.InputLine> inputs = components.stream()
                    .map(c -> new PayrollCalculationService.InputLine(
                            c.getSalaryComponent().getSalaryComponentId(),
                            c.getSalaryComponent().getCode(),
                            c.getSalaryComponent().getName(),
                            c.getComponentType(),
                            c.getCalculationMethod(),
                            c.getValue(),
                            Boolean.TRUE.equals(c.getApplicable()),
                            c.getSalaryComponent().getStatutoryCode(),
                            c.getSalaryComponent().getSortOrder() != null ? c.getSalaryComponent().getSortOrder() : 0
                    )).toList();

            PayrollCalculationService.Result calc = PayrollCalculationService.calculate(inputs,
                    new PayrollCalculationService.CalcContext(
                            salary.getPaymentType(),
                            Boolean.TRUE.equals(cfg.getPfEnabled()),
                            Boolean.TRUE.equals(cfg.getEsiEnabled()),
                            Boolean.TRUE.equals(cfg.getProfessionalTaxEnabled()),
                            Boolean.TRUE.equals(cfg.getTdsEnabled()),
                            workingDays,
                            lop,
                            cfg.getLopHandling(),
                            cfg.getSalaryRounding()
                    ));

            EmployeePayroll ep = new EmployeePayroll();
            ep.setPayrollRun(run);
            ep.setStaffId(staff.getStaffId());
            ep.setPayrollYear(run.getPayrollYear());
            ep.setPayrollMonth(run.getPayrollMonth());
            ep.setPaymentType(salary.getPaymentType());
            ep.setEmploymentCategory(staff.getEmploymentCategory() != null ? staff.getEmploymentCategory().name() : null);
            ep.setDesignation(staff.getDesignation());
            ep.setWorkingDays(workingDays);
            ep.setPresentDays(Math.max(0, workingDays - lop));
            ep.setPaidLeaveDays(0);
            ep.setLopDays(lop);
            ep.setGrossAmount(calc.grossAmount());
            ep.setTotalDeductions(calc.totalDeductions());
            ep.setNetAmount(calc.netAmount());
            ep.setStatus(empStatus);
            ep.setEmployeeSalaryId(salary.getEmployeeSalaryId());
            ep = employeePayrollRepository.save(ep);

            List<EmployeePayrollLine> lines = new ArrayList<>();
            for (PayrollCalculationService.OutputLine ol : calc.lines()) {
                EmployeePayrollLine line = new EmployeePayrollLine();
                line.setEmployeePayroll(ep);
                line.setSalaryComponentId(ol.salaryComponentId());
                line.setComponentCode(ol.code());
                line.setComponentName(ol.name());
                line.setComponentType(ol.componentType());
                line.setCalculationMethod(ol.calculationMethod());
                line.setRateOrPercent(ol.rateOrPercent());
                line.setAmount(ol.amount());
                line.setSortOrder(ol.sortOrder());
                lines.add(line);
            }
            lineRepository.saveAll(lines);

            if (!approvalRequired) {
                payslipService.freezeAndStore(ep);
            }
            generated++;
        }
        return generated;
    }

    private PayrollGenerateResult resultFromRun(PayrollRun run, List<PayrollGenerateResult.SkipInfo> skipped) {
        int count = employeePayrollRepository.findByPayrollRun_PayrollRunId(run.getPayrollRunId()).size();
        return PayrollGenerateResult.builder()
                .runId(run.getPayrollRunId())
                .status(run.getStatus().name())
                .generatedCount(count)
                .skippedCount(skipped.size())
                .skipped(skipped)
                .build();
    }

    private PayrollEmployeeRowResponse toRow(EmployeePayroll ep) {
        Staff staff = staffRepository.findById(ep.getStaffId()).orElse(null);
        BigDecimal paid = paymentRepository.sumByEmployeePayrollId(ep.getEmployeePayrollId());
        return PayrollEmployeeRowResponse.builder()
                .staffId(ep.getStaffId())
                .staffCode(staff != null ? staff.getStaffCode() : null)
                .staffName(staff != null ? staff.getFirstName() + " " + staff.getLastName() : null)
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

    private static void validatePeriod(Integer year, Integer month) {
        if (year == null || month == null || month < 1 || month > 12) {
            throw new BadRequestException("Valid year and month (1-12) are required");
        }
        YearMonth period = YearMonth.of(year, month);
        if (period.isAfter(YearMonth.now())) {
            throw new BadRequestException("Cannot generate payroll for a future period");
        }
    }
}
