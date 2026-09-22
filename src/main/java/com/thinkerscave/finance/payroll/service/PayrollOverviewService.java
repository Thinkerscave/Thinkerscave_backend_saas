package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.response.EmployeePayrollDetailResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollEmployeeRowResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollOverviewResponse;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.staff.enums.EmploymentCategory;
import org.springframework.data.domain.Pageable;

public interface PayrollOverviewService {
    PayrollOverviewResponse overview(Integer year, Integer month);
    PageResponse<PayrollEmployeeRowResponse> employees(Integer year, Integer month,
                                                       EmploymentCategory employmentCategory,
                                                       PaymentType paymentType,
                                                       EmployeePayrollStatus status,
                                                       String q,
                                                       Pageable pageable);
    PageResponse<EmployeePayrollDetailResponse> payrollHistory(Long staffId, Pageable pageable);
}
