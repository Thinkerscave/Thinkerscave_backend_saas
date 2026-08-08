package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PayrollReportResponse {

    private Integer year;
    private Integer month;
    private long totalRecords;
    private long paidCount;
    private long pendingCount;
    private BigDecimal totalGross;
    private BigDecimal totalDeductions;
    private BigDecimal totalNet;
    private List<PayrollResponse> records;
}
