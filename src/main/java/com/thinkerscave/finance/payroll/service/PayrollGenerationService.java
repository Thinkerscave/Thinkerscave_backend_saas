package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.request.PayrollGenerateRequest;
import com.thinkerscave.finance.payroll.dto.response.PayrollGenerateResult;
import com.thinkerscave.finance.payroll.dto.response.PayrollRunResponse;

public interface PayrollGenerationService {
    PayrollGenerateResult generate(PayrollGenerateRequest request, String idempotencyKey);
    PayrollGenerateResult recalculate(Long runId, PayrollGenerateRequest request);
    PayrollRunResponse getRun(Long runId);
}
