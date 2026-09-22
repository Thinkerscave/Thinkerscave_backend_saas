package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.response.EmployeePayrollDetailResponse;
import com.thinkerscave.finance.payroll.dto.response.MyPayrollSummaryResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollEmployeeRowResponse;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface PayrollMeService {
    MyPayrollSummaryResponse summary();
    PageResponse<PayrollEmployeeRowResponse> history(Pageable pageable);
    EmployeePayrollDetailResponse detail(Long employeePayrollId);
    byte[] payslip(Long employeePayrollId);
}
