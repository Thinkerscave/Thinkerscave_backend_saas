package com.thinkerscave.staff.service.impl;

import com.thinkerscave.staff.dto.request.BulkMarkPaidRequest;
import com.thinkerscave.staff.dto.request.PayrollGenerateRequest;
import com.thinkerscave.staff.dto.response.PayrollDashboardResponse;
import com.thinkerscave.staff.dto.response.PayrollGenerateResult;
import com.thinkerscave.staff.dto.response.PayrollReportResponse;
import com.thinkerscave.staff.dto.response.PayrollResponse;
import com.thinkerscave.staff.enums.PayrollStatus;
import com.thinkerscave.staff.service.PayrollService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @deprecated Staff-owned payroll is retired. Use Finance Payroll APIs under {@code /api/v1/payroll}.
 * Not a Spring bean — retained only as a compile-time stub.
 */
@Deprecated
public class PayrollServiceImpl implements PayrollService {

    private static final String MSG = "Use Finance Payroll APIs";

    @Override
    public PayrollDashboardResponse getDashboard() {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public PayrollGenerateResult generatePayroll(PayrollGenerateRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public Page<PayrollResponse> getPayrollList(Integer year, Integer month, PayrollStatus status,
                                                 Long staffId, Pageable pageable) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public PayrollResponse getPayrollDetail(Long payrollId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void markPaid(Long payrollId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void bulkMarkPaid(BulkMarkPaidRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<PayrollResponse> getMyPayrollHistory(String username) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public byte[] downloadPayslipPdf(Long payrollId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public PayrollReportResponse getMonthlyReport(Integer year, Integer month) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public byte[] exportMonthlyReportExcel(Integer year, Integer month) {
        throw new UnsupportedOperationException(MSG);
    }
}
