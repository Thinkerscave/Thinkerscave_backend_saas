package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PayrollDashboardResponse {

    private String currentMonth;
    private long totalStaff;
    private long generatedPayroll;
    private long pendingPayroll;
    private long paidPayroll;
}
