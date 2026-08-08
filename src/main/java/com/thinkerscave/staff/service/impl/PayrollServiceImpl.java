package com.thinkerscave.staff.service.impl;

import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.BulkMarkPaidRequest;
import com.thinkerscave.staff.dto.request.PayrollGenerateRequest;
import com.thinkerscave.staff.dto.response.PayrollDashboardResponse;
import com.thinkerscave.staff.dto.response.PayrollGenerateResult;
import com.thinkerscave.staff.dto.response.PayrollReportResponse;
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
import com.thinkerscave.staff.service.PayslipPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
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
    private final PayslipPdfService payslipPdfService;

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
    public PayrollGenerateResult generatePayroll(PayrollGenerateRequest request) {
        String tenant = requireOrganizationTenant();
        int year = request.getYear();
        int month = request.getMonth();
        log.info("Generating payroll for {}/{} under tenant={}", year, month, tenant);

        int workingDays = YearMonth.of(year, month).lengthOfMonth();
        // Schema-per-tenant: this query only sees staff in the active tenant catalog.
        List<Staff> activeStaff = staffRepository
                .findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE);

        int generated = 0;
        int skippedAlreadyExists = 0;
        List<String> skippedNoSalary = new ArrayList<>();

        for (Staff staff : activeStaff) {
            if (payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(
                    staff.getStaffId(), year, month)) {
                skippedAlreadyExists++;
                continue;
            }

            StaffSalaryStructure structure = salaryStructureRepository
                    .findByStaff_StaffIdAndActiveTrue(staff.getStaffId())
                    .orElse(null);
            if (structure == null) {
                log.warn("No active salary structure for staff: {} (tenant={})", staff.getStaffCode(), tenant);
                skippedNoSalary.add(staff.getStaffCode());
                continue;
            }

            BigDecimal grossSalary = orZero(structure.getGrossSalary());
            // Leave Management does not exist yet — LOP stays 0 (full attendance assumed).
            int lopDays = 0;
            BigDecimal dailyRate = workingDays > 0
                    ? grossSalary.divide(BigDecimal.valueOf(workingDays), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal lopDeduction = dailyRate.multiply(BigDecimal.valueOf(lopDays));

            BigDecimal pf = orZero(structure.getPfEmployee());
            BigDecimal esi = orZero(structure.getEsiEmployee());
            BigDecimal pt = orZero(structure.getProfessionalTax());
            BigDecimal otherDed = orZero(structure.getOtherDeduction());
            BigDecimal totalDeductions = pf.add(esi).add(pt).add(otherDed).add(lopDeduction);
            BigDecimal netSalary = grossSalary.subtract(totalDeductions);
            if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
                netSalary = BigDecimal.ZERO;
            }

            Payroll payroll = new Payroll();
            payroll.setStaff(staff);
            payroll.setPayrollYear(year);
            payroll.setPayrollMonth(month);
            payroll.setWorkingDays(workingDays);
            payroll.setPresentDays(workingDays - lopDays);
            payroll.setLeaveWithoutPayDays(lopDays);
            payroll.setGrossSalary(grossSalary);
            payroll.setPfAmount(pf);
            payroll.setEsiAmount(esi);
            payroll.setProfessionalTaxAmount(pt);
            payroll.setOtherDeductionAmount(otherDed);
            payroll.setTotalDeductions(totalDeductions);
            payroll.setNetSalary(netSalary);
            payroll.setStatus(PayrollStatus.GENERATED);
            payroll.setGeneratedOn(LocalDate.now());

            payrollRepository.save(payroll);
            generated++;
        }

        log.info("Payroll generation complete for tenant={}. created={}, skippedExists={}, skippedNoSalary={}",
                tenant, generated, skippedAlreadyExists, skippedNoSalary.size());

        return PayrollGenerateResult.builder()
                .generatedRecords(generated)
                .skippedAlreadyExists(skippedAlreadyExists)
                .skippedNoSalaryStructure(List.copyOf(skippedNoSalary))
                .tenantIdentifier(tenant)
                .build();
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

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadPayslipPdf(Long payrollId) {
        Payroll payroll = getEntity(payrollId);
        StaffSalaryStructure structure = salaryStructureRepository
                .findByStaff_StaffIdAndActiveTrue(payroll.getStaff().getStaffId())
                .orElse(null);
        return payslipPdfService.buildPayslip(payroll, structure);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollReportResponse getMonthlyReport(Integer year, Integer month) {
        requireYearMonth(year, month);
        List<Payroll> rows = payrollRepository.findByPayrollYearAndPayrollMonth(year, month);
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDed = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        long paid = 0;
        for (Payroll p : rows) {
            totalGross = totalGross.add(orZero(p.getGrossSalary()));
            totalDed = totalDed.add(orZero(p.getTotalDeductions()));
            totalNet = totalNet.add(orZero(p.getNetSalary()));
            if (p.getStatus() == PayrollStatus.PAID) {
                paid++;
            }
        }
        return PayrollReportResponse.builder()
                .year(year)
                .month(month)
                .totalRecords(rows.size())
                .paidCount(paid)
                .pendingCount(rows.size() - paid)
                .totalGross(totalGross)
                .totalDeductions(totalDed)
                .totalNet(totalNet)
                .records(rows.stream().map(this::toResponse).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportMonthlyReportExcel(Integer year, Integer month) {
        PayrollReportResponse report = getMonthlyReport(year, month);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Payroll " + year + "-" + month);
            Row header = sheet.createRow(0);
            String[] cols = {
                    "Staff Code", "Staff Name", "Gross", "PF", "ESI", "Prof Tax",
                    "Other Deduction", "Total Deductions", "Net", "Status", "Paid On"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            int r = 1;
            for (PayrollResponse p : report.getRecords()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(nullToEmpty(p.getStaffCode()));
                row.createCell(1).setCellValue(nullToEmpty(p.getStaffName()));
                row.createCell(2).setCellValue(toDouble(p.getGrossSalary()));
                row.createCell(3).setCellValue(toDouble(p.getPfAmount()));
                row.createCell(4).setCellValue(toDouble(p.getEsiAmount()));
                row.createCell(5).setCellValue(toDouble(p.getProfessionalTaxAmount()));
                row.createCell(6).setCellValue(toDouble(p.getOtherDeductionAmount()));
                row.createCell(7).setCellValue(toDouble(p.getTotalDeductions()));
                row.createCell(8).setCellValue(toDouble(p.getNetSalary()));
                row.createCell(9).setCellValue(p.getStatus() != null ? p.getStatus().name() : "");
                row.createCell(10).setCellValue(p.getPaidOn() != null ? p.getPaidOn().toString() : "");
            }
            Row totals = sheet.createRow(r + 1);
            totals.createCell(0).setCellValue("TOTALS");
            totals.createCell(2).setCellValue(toDouble(report.getTotalGross()));
            totals.createCell(7).setCellValue(toDouble(report.getTotalDeductions()));
            totals.createCell(8).setCellValue(toDouble(report.getTotalNet()));
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to export payroll report", ex);
        }
    }

    /**
     * Payroll must never run against the platform catalog. Under schema-per-tenant,
     * isolation is catalog-based; generating with {@code public}/blank would write
     * into the platform schema.
     */
    private String requireOrganizationTenant() {
        String tenant = TenantContext.getTenant();
        if (!StringUtils.hasText(tenant)
                || "public".equalsIgnoreCase(tenant)
                || "platform".equalsIgnoreCase(tenant)) {
            throw new BadRequestException(
                    "Payroll generation requires an active organization tenant context");
        }
        return tenant;
    }

    private void requireYearMonth(Integer year, Integer month) {
        if (year == null || month == null || month < 1 || month > 12) {
            throw new BadRequestException("year and month (1-12) are required");
        }
    }

    private Payroll getEntity(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found: " + id));
    }

    private static BigDecimal orZero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private static double toDouble(BigDecimal val) {
        return orZero(val).doubleValue();
    }

    private static String nullToEmpty(String val) {
        return val != null ? val : "";
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
                .pfAmount(p.getPfAmount())
                .esiAmount(p.getEsiAmount())
                .professionalTaxAmount(p.getProfessionalTaxAmount())
                .otherDeductionAmount(p.getOtherDeductionAmount())
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
