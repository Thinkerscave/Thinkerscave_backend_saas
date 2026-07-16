package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExaminationSummaryData {
    private List<StatListItem> upcoming;
}
