package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.EmployeePayrollLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeePayrollLineRepository extends JpaRepository<EmployeePayrollLine, Long> {
    List<EmployeePayrollLine> findByEmployeePayroll_EmployeePayrollIdOrderBySortOrderAsc(Long employeePayrollId);
    void deleteByEmployeePayroll_EmployeePayrollId(Long employeePayrollId);
}
