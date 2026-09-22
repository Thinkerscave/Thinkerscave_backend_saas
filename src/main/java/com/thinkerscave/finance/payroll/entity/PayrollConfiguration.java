package com.thinkerscave.finance.payroll.entity;

import com.thinkerscave.finance.payroll.enums.LopHandling;
import com.thinkerscave.finance.payroll.enums.PayrollFrequency;
import com.thinkerscave.finance.payroll.enums.SalaryRounding;
import com.thinkerscave.finance.payroll.enums.WorkingDaysBasis;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "payroll_configuration")
public class PayrollConfiguration extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_configuration_id")
    @EqualsAndHashCode.Include
    private Long payrollConfigurationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private PayrollFrequency frequency = PayrollFrequency.MONTHLY;

    @Column(name = "default_generation_day", nullable = false)
    private Short defaultGenerationDay = 1;

    @Column(name = "approval_required", nullable = false)
    private Boolean approvalRequired = true;

    @Column(name = "default_payment_method_id")
    private Long defaultPaymentMethodId;

    @Column(name = "pf_enabled", nullable = false)
    private Boolean pfEnabled = false;

    @Column(name = "esi_enabled", nullable = false)
    private Boolean esiEnabled = false;

    @Column(name = "professional_tax_enabled", nullable = false)
    private Boolean professionalTaxEnabled = false;

    @Column(name = "tds_enabled", nullable = false)
    private Boolean tdsEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "working_days_basis", nullable = false, length = 30)
    private WorkingDaysBasis workingDaysBasis = WorkingDaysBasis.CALENDAR;

    @Column(name = "include_paid_leave", nullable = false)
    private Boolean includePaidLeave = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "lop_handling", nullable = false, length = 40)
    private LopHandling lopHandling = LopHandling.PRORATE_GROSS;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_rounding", nullable = false, length = 30)
    private SalaryRounding salaryRounding = SalaryRounding.NEAREST_RUPEE;

    @Column(name = "payslip_number_format", length = 80)
    private String payslipNumberFormat;

    @Column(name = "payslip_org_name", length = 200)
    private String payslipOrgName;

    @Column(name = "show_org_logo", nullable = false)
    private Boolean showOrgLogo = true;

    @Column(name = "show_authorized_signatory", nullable = false)
    private Boolean showAuthorizedSignatory = true;
}
