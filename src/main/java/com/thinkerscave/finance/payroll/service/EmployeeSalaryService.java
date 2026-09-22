package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.request.EmployeeSalaryRequest;
import com.thinkerscave.finance.payroll.dto.response.EmployeeSalaryResponse;

import java.util.List;

public interface EmployeeSalaryService {
    EmployeeSalaryResponse getCurrent(Long staffId);
    List<EmployeeSalaryResponse> history(Long staffId);
    EmployeeSalaryResponse assign(Long staffId, EmployeeSalaryRequest request);
}
