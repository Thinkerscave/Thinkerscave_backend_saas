package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class FeeSummaryData {
    private double totalDue;
    private double totalPaid;
    private double pendingAmount;
    private LocalDate nextDueDate;
    private int pendingInvoices;
    private String currency;
}
