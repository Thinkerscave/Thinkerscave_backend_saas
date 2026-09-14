package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.request.PayrollPaymentRequest;
import com.thinkerscave.finance.payroll.dto.response.PayrollPaymentResponse;

public interface PayrollPaymentService {
    PayrollPaymentResponse recordPayment(Long employeePayrollId, PayrollPaymentRequest request, String idempotencyKey);
}
