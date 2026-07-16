package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecentRecordsData {
    private List<String> columns;
    private List<RecordItem> items;
}
