package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.request.BulkMarkPaidRequest;
import com.thinkerscave.staff.dto.request.PayrollGenerateRequest;
import com.thinkerscave.staff.dto.response.PayrollDashboardResponse;
import com.thinkerscave.staff.dto.response.PayrollResponse;
import com.thinkerscave.staff.enums.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PayrollService {

    PayrollDashboardResponse getDashboard();

    int generatePayroll(PayrollGenerateRequest request);

    Page<PayrollResponse> getPayrollList(Integer year, Integer month, PayrollStatus status, Long staffId, Pageable pageable);

    PayrollResponse getPayrollDetail(Long payrollId);

    void markPaid(Long payrollId);

    void bulkMarkPaid(BulkMarkPaidRequest request);

    List<PayrollResponse> getMyPayrollHistory(String username);
}
