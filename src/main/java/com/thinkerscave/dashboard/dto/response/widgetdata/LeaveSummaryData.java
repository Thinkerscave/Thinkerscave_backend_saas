package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaveSummaryData {
    private int availableDays;
    private int usedDays;
    private int pendingRequests;
    private String lastRequestStatus;
}
