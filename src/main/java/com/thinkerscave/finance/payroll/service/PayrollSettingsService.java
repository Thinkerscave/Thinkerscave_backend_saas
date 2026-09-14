package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.request.PayrollSettingsRequest;
import com.thinkerscave.finance.payroll.dto.response.PayrollSettingsResponse;
import com.thinkerscave.finance.payroll.entity.PayrollConfiguration;

public interface PayrollSettingsService {
    PayrollSettingsResponse get();
    PayrollSettingsResponse update(PayrollSettingsRequest request);
    PayrollConfiguration requireConfig();
}
