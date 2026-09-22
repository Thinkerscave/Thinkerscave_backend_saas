package com.thinkerscave.finance.payroll.dto.response;

import com.thinkerscave.finance.payroll.enums.LopHandling;
import com.thinkerscave.finance.payroll.enums.PayrollFrequency;
import com.thinkerscave.finance.payroll.enums.SalaryRounding;
import com.thinkerscave.finance.payroll.enums.WorkingDaysBasis;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayrollSettingsResponse {
    private Long payrollConfigurationId;
    private PayrollFrequency frequency;
    private Short defaultGenerationDay;
    private Boolean approvalRequired;
    private Long defaultPaymentMethodId;
    private Boolean pfEnabled;
    private Boolean esiEnabled;
    private Boolean professionalTaxEnabled;
    private Boolean tdsEnabled;
    private WorkingDaysBasis workingDaysBasis;
    private Boolean includePaidLeave;
    private LopHandling lopHandling;
    private SalaryRounding salaryRounding;
    private String payslipNumberFormat;
    private String payslipOrgName;
    private Boolean showOrgLogo;
    private Boolean showAuthorizedSignatory;
}
