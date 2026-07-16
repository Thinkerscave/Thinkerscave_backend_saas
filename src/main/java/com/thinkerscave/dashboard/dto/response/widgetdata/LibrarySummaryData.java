package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LibrarySummaryData {
    private int booksIssued;
    private int booksOverdue;
    private double fineDue;
}
