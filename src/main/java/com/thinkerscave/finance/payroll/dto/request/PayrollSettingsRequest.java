package com.thinkerscave.finance.payroll.dto.request;

import com.thinkerscave.finance.payroll.enums.LopHandling;
import com.thinkerscave.finance.payroll.enums.SalaryRounding;
import com.thinkerscave.finance.payroll.enums.WorkingDaysBasis;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PayrollSettingsRequest {
    @Min(1) @Max(28)
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
