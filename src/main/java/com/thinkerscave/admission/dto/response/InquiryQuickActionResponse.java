package com.thinkerscave.admission.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InquiryQuickActionResponse {

    private long overdue;
    private long dueToday;
    private long dueTomorrow;
    private long dueThisWeek;
}