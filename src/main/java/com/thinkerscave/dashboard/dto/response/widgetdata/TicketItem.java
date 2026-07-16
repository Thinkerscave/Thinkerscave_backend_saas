package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketItem {
    private String subject;
    private String status;
    private String priority;
    private String raisedAgo;
}
