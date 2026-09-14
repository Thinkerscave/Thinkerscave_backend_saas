package com.thinkerscave.finance.payroll.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.payroll.dto.request.PayrollSettingsRequest;
import com.thinkerscave.finance.payroll.dto.response.PayrollSettingsResponse;
import com.thinkerscave.finance.payroll.entity.PayrollConfiguration;
import com.thinkerscave.finance.payroll.enums.LopHandling;
import com.thinkerscave.finance.payroll.enums.PayrollFrequency;
import com.thinkerscave.finance.payroll.enums.SalaryRounding;
import com.thinkerscave.finance.payroll.enums.WorkingDaysBasis;
import com.thinkerscave.finance.payroll.repository.PayrollConfigurationRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.finance.payroll.service.PayrollSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollSettingsServiceImpl implements PayrollSettingsService {

    private final PayrollConfigurationRepository repository;
    private final PayrollAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    public PayrollSettingsResponse get() {
        accessGuard.requireView(PayrollAccessGuard.RESOURCE_SETTINGS);
        return repository.findFirstByOrderByPayrollConfigurationIdAsc()
                .map(this::toResponse)
                .orElseGet(() -> toResponse(defaults()));
    }

    @Override
    @Transactional
    public PayrollSettingsResponse update(PayrollSettingsRequest request) {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_SETTINGS);
        PayrollConfiguration cfg = requireConfig();
        if (request.getDefaultGenerationDay() != null) {
            cfg.setDefaultGenerationDay(request.getDefaultGenerationDay());
        }
        if (request.getApprovalRequired() != null) {
            cfg.setApprovalRequired(request.getApprovalRequired());
        }
        if (request.getDefaultPaymentMethodId() != null) {
            cfg.setDefaultPaymentMethodId(request.getDefaultPaymentMethodId());
        }
        if (request.getPfEnabled() != null) {
            cfg.setPfEnabled(request.getPfEnabled());
        }
        if (request.getEsiEnabled() != null) {
            cfg.setEsiEnabled(request.getEsiEnabled());
        }
        if (request.getProfessionalTaxEnabled() != null) {
            cfg.setProfessionalTaxEnabled(request.getProfessionalTaxEnabled());
        }
        if (request.getTdsEnabled() != null) {
            cfg.setTdsEnabled(request.getTdsEnabled());
        }
        if (request.getWorkingDaysBasis() != null) {
            cfg.setWorkingDaysBasis(request.getWorkingDaysBasis());
        }
        if (request.getIncludePaidLeave() != null) {
            cfg.setIncludePaidLeave(request.getIncludePaidLeave());
        }
        if (request.getLopHandling() != null) {
            cfg.setLopHandling(request.getLopHandling());
        }
        if (request.getSalaryRounding() != null) {
            cfg.setSalaryRounding(request.getSalaryRounding());
        }
        if (request.getPayslipNumberFormat() != null) {
            cfg.setPayslipNumberFormat(request.getPayslipNumberFormat());
        }
        if (request.getPayslipOrgName() != null) {
            cfg.setPayslipOrgName(request.getPayslipOrgName());
        }
        if (request.getShowOrgLogo() != null) {
            cfg.setShowOrgLogo(request.getShowOrgLogo());
        }
        if (request.getShowAuthorizedSignatory() != null) {
            cfg.setShowAuthorizedSignatory(request.getShowAuthorizedSignatory());
        }
        PayrollConfiguration saved = repository.save(cfg);
        auditWriteService.record(AuditEventType.UPDATE, "PAYROLL_SETTINGS_UPDATE", "PayrollConfiguration",
                String.valueOf(saved.getPayrollConfigurationId()), "Updated payroll settings");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayrollConfiguration requireConfig() {
        return repository.findFirstByOrderByPayrollConfigurationIdAsc().orElseGet(() -> repository.save(defaults()));
    }

    private PayrollConfiguration defaults() {
        PayrollConfiguration cfg = new PayrollConfiguration();
        cfg.setFrequency(PayrollFrequency.MONTHLY);
        cfg.setDefaultGenerationDay((short) 1);
        cfg.setApprovalRequired(true);
        cfg.setPfEnabled(false);
        cfg.setEsiEnabled(false);
        cfg.setProfessionalTaxEnabled(false);
        cfg.setTdsEnabled(false);
        cfg.setWorkingDaysBasis(WorkingDaysBasis.CALENDAR);
        cfg.setIncludePaidLeave(true);
        cfg.setLopHandling(LopHandling.PRORATE_GROSS);
        cfg.setSalaryRounding(SalaryRounding.NEAREST_RUPEE);
        cfg.setShowOrgLogo(true);
        cfg.setShowAuthorizedSignatory(true);
        return cfg;
    }

    private PayrollSettingsResponse toResponse(PayrollConfiguration cfg) {
        return PayrollSettingsResponse.builder()
                .payrollConfigurationId(cfg.getPayrollConfigurationId())
                .frequency(cfg.getFrequency())
                .defaultGenerationDay(cfg.getDefaultGenerationDay())
                .approvalRequired(cfg.getApprovalRequired())
                .defaultPaymentMethodId(cfg.getDefaultPaymentMethodId())
                .pfEnabled(cfg.getPfEnabled())
                .esiEnabled(cfg.getEsiEnabled())
                .professionalTaxEnabled(cfg.getProfessionalTaxEnabled())
                .tdsEnabled(cfg.getTdsEnabled())
                .workingDaysBasis(cfg.getWorkingDaysBasis())
                .includePaidLeave(cfg.getIncludePaidLeave())
                .lopHandling(cfg.getLopHandling())
                .salaryRounding(cfg.getSalaryRounding())
                .payslipNumberFormat(cfg.getPayslipNumberFormat())
                .payslipOrgName(cfg.getPayslipOrgName())
                .showOrgLogo(cfg.getShowOrgLogo())
                .showAuthorizedSignatory(cfg.getShowAuthorizedSignatory())
                .build();
    }
}
