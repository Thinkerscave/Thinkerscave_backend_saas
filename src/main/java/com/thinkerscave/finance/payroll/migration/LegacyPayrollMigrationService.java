package com.thinkerscave.finance.payroll.migration;

import com.thinkerscave.finance.payroll.dto.response.LegacyMigrationInventoryResponse;
import com.thinkerscave.finance.payroll.repository.EmployeePayrollRepository;
import com.thinkerscave.finance.payroll.repository.EmployeeSalaryRepository;
import com.thinkerscave.finance.payroll.repository.PayrollRunRepository;
import com.thinkerscave.finance.payroll.security.PayrollAccessGuard;
import com.thinkerscave.staff.repository.PayrollRepository;
import com.thinkerscave.staff.repository.StaffSalaryStructureRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Inventory + callable migrator for legacy Staff payroll tables.
 * SQL migration V1_41 applies the same transforms at schema patch time.
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

    @PersistenceContext
    private EntityManager entityManager;

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
     * Re-applies V1_41 SQL against the current tenant connection (idempotent inserts).
     */
    @Transactional
    public LegacyMigrationInventoryResponse migrate() {
        accessGuard.requireManage(PayrollAccessGuard.RESOURCE_SETTINGS);
        try {
            String sql = StreamUtils.copyToString(
                    new ClassPathResource("db/migration/V1_41__migrate_legacy_staff_payroll.sql").getInputStream(),
                    StandardCharsets.UTF_8);
            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Legacy payroll migration failed: " + ex.getMessage(), ex);
        }
        return inventory();
    }
}
