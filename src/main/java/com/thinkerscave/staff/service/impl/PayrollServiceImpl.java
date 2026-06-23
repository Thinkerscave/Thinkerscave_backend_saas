package com.thinkerscave.staff.service.impl;

import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.BulkMarkPaidRequest;
import com.thinkerscave.staff.dto.request.PayrollGenerateRequest;
import com.thinkerscave.staff.dto.response.PayrollDashboardResponse;
import com.thinkerscave.staff.dto.response.PayrollResponse;
import com.thinkerscave.staff.entity.Payroll;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.entity.StaffSalaryStructure;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.PayrollStatus;
import com.thinkerscave.staff.repository.PayrollRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.repository.StaffSalaryStructureRepository;
import com.thinkerscave.staff.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final StaffRepository staffRepository;
    private final StaffSalaryStructureRepository salaryStructureRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PayrollDashboardResponse getDashboard() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        long totalStaff = staffRepository.countByActive(true);
        long generated = payrollRepository.countByYearAndMonth(year, month);
        long paid = payrollRepository.countByYearAndMonthAndStatus(year, month, PayrollStatus.PAID);
        long pending = generated - paid;

        String currentMonth = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase()
                + "-" + year;

        return PayrollDashboardResponse.builder()
                .currentMonth(currentMonth)
                .totalStaff(totalStaff)
                .generatedPayroll(generated)
                .pendingPayroll(Math.max(0, pending))
                .paidPayroll(paid)
                .build();
    }

    @Override
    @Transactional
    public int generatePayroll(PayrollGenerateRequest request) {
        int year = request.getYear();
        int month = request.getMonth();
        log.info("Generating payroll for {}/{}", year, month);

        int workingDays = YearMonth.of(year, month).lengthOfMonth();
        List<Staff> activeStaff = staffRepository.findAll().stream()
                .filter(s -> s.getActive() && s.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .collect(Collectors.toList());

        int generated = 0;
        for (Staff staff : activeStaff) {
            if (payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(
                    staff.getStaffId(), year, month)) {
                continue;
            }

            StaffSalaryStructure structure = salaryStructureRepository
                    .findByStaff_StaffIdAndActiveTrue(staff.getStaffId())
                    .orElse(null);
            if (structure == null) {
                log.warn("No active salary structure for staff: {}", staff.getStaffCode());
                continue;
            }

            BigDecimal grossSalary = structure.getGrossSalary();
            // Simplified: assume full attendance (LOP can be adjusted later)
            int lopDays = 0;
            BigDecimal dailyRate = grossSalary.divide(
                    BigDecimal.valueOf(workingDays), 4, RoundingMode.HALF_UP);
            BigDecimal deductions = dailyRate.multiply(BigDecimal.valueOf(lopDays));
            BigDecimal netSalary = grossSalary.subtract(deductions);

            Payroll payroll = new Payroll();
            payroll.setStaff(staff);
            payroll.setPayrollYear(year);
            payroll.setPayrollMonth(month);
            payroll.setWorkingDays(workingDays);
            payroll.setPresentDays(workingDays - lopDays);
            payroll.setLeaveWithoutPayDays(lopDays);
            payroll.setGrossSalary(grossSalary);
            payroll.setTotalDeductions(deductions);
            payroll.setNetSalary(netSalary);
            payroll.setStatus(PayrollStatus.GENERATED);
            payroll.setGeneratedOn(LocalDate.now());

            payrollRepository.save(payroll);
            generated++;
        }

        log.info("Payroll generation complete. Records created: {}", generated);
        return generated;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollResponse> getPayrollList(Integer year, Integer month, PayrollStatus status,
                                                 Long staffId, Pageable pageable) {
        return payrollRepository.searchPayroll(year, month, status, staffId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getPayrollDetail(Long payrollId) {
        Payroll payroll = getEntity(payrollId);
        return toResponse(payroll);
    }

    @Override
    @Transactional
    public void markPaid(Long payrollId) {
        Payroll payroll = getEntity(payrollId);
        if (payroll.getStatus() == PayrollStatus.PAID) {
            throw new BadRequestException("Payroll is already marked as paid");
        }
        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaidOn(LocalDate.now());
        payrollRepository.save(payroll);
    }

    @Override
    @Transactional
    public void bulkMarkPaid(BulkMarkPaidRequest request) {
        for (Long payrollId : request.getPayrollIds()) {
            payrollRepository.findById(payrollId).ifPresent(p -> {
                if (p.getStatus() != PayrollStatus.PAID) {
                    p.setStatus(PayrollStatus.PAID);
                    p.setPaidOn(LocalDate.now());
                    payrollRepository.save(p);
                }
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> getMyPayrollHistory(String username) {
        Staff staff = staffRepository.findByUser_Id(
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username))
                        .getId()
        ).orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + username));

        return payrollRepository.findByStaff_StaffIdOrderByPayrollYearDescPayrollMonthDesc(staff.getStaffId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Payroll getEntity(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found: " + id));
    }

    private PayrollResponse toResponse(Payroll p) {
        return PayrollResponse.builder()
                .payrollId(p.getPayrollId())
                .staffId(p.getStaff().getStaffId())
                .staffName(p.getStaff().getFirstName() + " " + p.getStaff().getLastName())
                .staffCode(p.getStaff().getStaffCode())
                .payrollYear(p.getPayrollYear())
                .payrollMonth(p.getPayrollMonth())
                .workingDays(p.getWorkingDays())
                .presentDays(p.getPresentDays())
                .leaveWithoutPayDays(p.getLeaveWithoutPayDays())
                .grossSalary(p.getGrossSalary())
                .totalDeductions(p.getTotalDeductions())
                .netSalary(p.getNetSalary())
                .status(p.getStatus())
                .generatedOn(p.getGeneratedOn())
                .paidOn(p.getPaidOn())
                .remarks(p.getRemarks())
                .createdOn(p.getCreatedOn())
                .build();
    }
}
