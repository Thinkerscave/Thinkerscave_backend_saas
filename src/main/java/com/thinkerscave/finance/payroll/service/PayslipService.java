package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.entity.EmployeePayroll;

public interface PayslipService {
    void freezeAndStore(EmployeePayroll employeePayroll);
    byte[] loadStoredPayslip(Long employeePayrollId);
    void clearPayslipIfUnpaid(EmployeePayroll employeePayroll);
}
