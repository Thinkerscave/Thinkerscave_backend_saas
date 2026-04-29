package com.thinkerscave.common.payroll.service.impl;

import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.payroll.domain.StaffPayroll;
import com.thinkerscave.common.payroll.dto.PayrollDTO;
import com.thinkerscave.common.payroll.repository.PayrollRepository;
import com.thinkerscave.common.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;

    /**
     * Returns the current organization ID from the request context.
     */
    private Long requireOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException(
                    "No organization context set. Ensure X-Organization-ID header is provided or auto-detected.");
        }
        return orgId;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollDTO> getAllPayroll() {
        Long orgId = requireOrgId();
        return payrollRepository.findByOrganizationId(orgId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDTO getByStaffId(Long staffId) {
        Long orgId = requireOrgId();
        return payrollRepository.findByOrganizationIdAndStaffId(orgId, staffId)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException(
                        "No payroll record for staff: " + staffId + " in this organization"));
    }

    @Override
    @Transactional
    public PayrollDTO saveOrUpdate(PayrollDTO dto, String updatedBy) {
        Long orgId = requireOrgId();

        StaffPayroll payroll = payrollRepository.findByOrganizationIdAndStaffId(orgId, dto.getStaffId())
                .orElse(StaffPayroll.builder()
                        .organizationId(orgId)
                        .staffId(dto.getStaffId())
                        .staffName(dto.getStaffName())
                        .department(dto.getDepartment())
                        .designation(dto.getDesignation())
                        .build());

        // Update earnings
        if (dto.getBasic() != null)
            payroll.setBasic(dto.getBasic());
        if (dto.getHra() != null)
            payroll.setHra(dto.getHra());
        if (dto.getSpecialAllowance() != null)
            payroll.setSpecialAllowance(dto.getSpecialAllowance());
        if (dto.getAcademicAllowance() != null)
            payroll.setAcademicAllowance(dto.getAcademicAllowance());
        if (dto.getMedicalAllowance() != null)
            payroll.setMedicalAllowance(dto.getMedicalAllowance());
        if (dto.getTravelAllowance() != null)
            payroll.setTravelAllowance(dto.getTravelAllowance());
        if (dto.getDearnessAllowance() != null)
            payroll.setDearnessAllowance(dto.getDearnessAllowance());
        if (dto.getOtherAllowance() != null)
            payroll.setOtherAllowance(dto.getOtherAllowance());
        // Update deductions
        if (dto.getProfessionalTax() != null)
            payroll.setProfessionalTax(dto.getProfessionalTax());
        if (dto.getIncomeTax() != null)
            payroll.setIncomeTax(dto.getIncomeTax());
        if (dto.getProvidentFund() != null)
            payroll.setProvidentFund(dto.getProvidentFund());

        payroll.setEffectiveFrom(dto.getEffectiveFrom() != null ? dto.getEffectiveFrom() : LocalDate.now());
        payroll.setUpdatedBy(updatedBy);

        return toDTO(payrollRepository.save(payroll));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> runPayroll(String runBy) {
        Long orgId = requireOrgId();
        List<StaffPayroll> all = payrollRepository.findByOrganizationId(orgId);

        BigDecimal totalGross = all.stream()
                .map(StaffPayroll::getGrossSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = all.stream()
                .map(StaffPayroll::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate now = LocalDate.now();
        String monthYear = Month.of(now.getMonthValue()).name() + " " + now.getYear();

        Map<String, Object> result = new HashMap<>();
        result.put("month", monthYear);
        result.put("totalStaff", all.size());
        result.put("totalGross", totalGross);
        result.put("totalNet", totalNet);
        result.put("runBy", runBy);
        result.put("status", "PROCESSED");
        result.put("processedAt", java.time.LocalDateTime.now().toString());

        log.info("Payroll run for {} (org={}) by {}: {} staff, total net ₹{}", monthYear, orgId, runBy, all.size(),
                totalNet);
        return result;
    }

    private PayrollDTO toDTO(StaffPayroll p) {
        return PayrollDTO.builder()
                .id(p.getId())
                .organizationId(p.getOrganizationId())
                .staffId(p.getStaffId())
                .staffName(p.getStaffName())
                .department(p.getDepartment())
                .designation(p.getDesignation())
                .basic(p.getBasic())
                .hra(p.getHra())
                .specialAllowance(p.getSpecialAllowance())
                .academicAllowance(p.getAcademicAllowance())
                .medicalAllowance(p.getMedicalAllowance())
                .travelAllowance(p.getTravelAllowance())
                .dearnessAllowance(p.getDearnessAllowance())
                .otherAllowance(p.getOtherAllowance())
                .professionalTax(p.getProfessionalTax())
                .incomeTax(p.getIncomeTax())
                .providentFund(p.getProvidentFund())
                .grossSalary(p.getGrossSalary())
                .totalDeductions(p.getTotalDeductions())
                .netSalary(p.getNetSalary())
                .ctcAnnual(p.getGrossSalary().multiply(BigDecimal.valueOf(12)))
                .effectiveFrom(p.getEffectiveFrom())
                .build();
    }
}
