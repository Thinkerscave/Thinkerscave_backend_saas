package com.thinkerscave.finance.payroll.migration;

import com.thinkerscave.finance.payroll.dto.response.LegacyMigrationInventoryResponse;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryRepository;
import com.thinkerscave.finance.payroll.repository.PayrollRunRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.staff.repository.PayrollRepository;
import com.thinkerscave.staff.repository.StaffSalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inventory for legacy Staff payroll tables.
 * Schema/data migration is operator-owned (see scripts/postgres/archive/migration/).
 */
@Service
@RequiredArgsConstructor
public class LegacyPayrollMigrationService {

    private final StaffSalaryStructureRepository legacySalaryRepository;
    private final PayrollRepository legacyPayrollRepository;
    private final EmployeeSalaryRepository employeeSalaryRepository;
    private final EmployeePayrollRepository employeePayrollRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollAccessGuard accessGuard;

    @Transactional(readOnly = true)
    public LegacyMigrationInventoryResponse inventory() {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_SETTINGS);
        return LegacyMigrationInventoryResponse.builder()
                .legacySalaryStructures(legacySalaryRepository.count())
                .legacyPayrollRows(legacyPayrollRepository.count())
                .financeEmployeeSalaries(employeeSalaryRepository.count())
                .financeEmployeePayrolls(employeePayrollRepository.count())
                .financePayrollRuns(payrollRunRepository.count())
                .build();
    }

    /**
     * Disabled: the application must not apply SQL migrations at runtime.
     * Run {@code scripts/postgres/archive/migration/V1_41__migrate_legacy_staff_payroll.sql}
     * manually against the target schema when needed.
     */
    @Transactional(readOnly = true)
    public LegacyMigrationInventoryResponse migrate() {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_SETTINGS);
        throw new IllegalStateException(
                "Legacy payroll SQL is no longer applied by the application. "
                        + "Run scripts/postgres/archive/migration/V1_41__migrate_legacy_staff_payroll.sql "
                        + "manually (psql / DBA), then refresh inventory.");
    }
}
