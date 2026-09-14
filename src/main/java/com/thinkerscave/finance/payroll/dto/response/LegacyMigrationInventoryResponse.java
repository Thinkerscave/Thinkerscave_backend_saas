package com.thinkerscave.finance.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LegacyMigrationInventoryResponse {
    private long legacySalaryStructures;
    private long legacyPayrollRows;
    private long financeEmployeeSalaries;
    private long financeEmployeePayrolls;
    private long financePayrollRuns;
}
