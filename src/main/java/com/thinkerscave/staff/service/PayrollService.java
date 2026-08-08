package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.request.BulkMarkPaidRequest;
import com.thinkerscave.staff.dto.request.PayrollGenerateRequest;
import com.thinkerscave.staff.dto.response.PayrollDashboardResponse;
import com.thinkerscave.staff.dto.response.PayrollGenerateResult;
import com.thinkerscave.staff.dto.response.PayrollReportResponse;
import com.thinkerscave.staff.dto.response.PayrollResponse;
import com.thinkerscave.staff.enums.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PayrollService {

    PayrollDashboardResponse getDashboard();

    /**
     * Generate payroll for all active staff in the <strong>current tenant schema only</strong>.
     * Requires a real organization tenant context (not platform/{@code public}).
     */
    PayrollGenerateResult generatePayroll(PayrollGenerateRequest request);

    Page<PayrollResponse> getPayrollList(Integer year, Integer month, PayrollStatus status, Long staffId, Pageable pageable);

    PayrollResponse getPayrollDetail(Long payrollId);

    void markPaid(Long payrollId);

    void bulkMarkPaid(BulkMarkPaidRequest request);

    List<PayrollResponse> getMyPayrollHistory(String username);

    byte[] downloadPayslipPdf(Long payrollId);

    PayrollReportResponse getMonthlyReport(Integer year, Integer month);

    byte[] exportMonthlyReportExcel(Integer year, Integer month);
}
