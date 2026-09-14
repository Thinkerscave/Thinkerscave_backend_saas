package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.response.PayrollRunResponse;

public interface PayrollApprovalService {
    PayrollRunResponse approve(Long runId);
    PayrollRunResponse returnForCorrection(Long runId);
}
