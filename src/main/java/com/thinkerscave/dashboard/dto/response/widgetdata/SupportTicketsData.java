package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SupportTicketsData {
    private int openCount;
    private List<TicketItem> items;
}
