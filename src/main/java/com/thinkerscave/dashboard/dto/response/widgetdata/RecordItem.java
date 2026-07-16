package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/** Generic row for a lightweight "recent records" table widget. */
@Data
@Builder
public class RecordItem {
    private String primaryLabel;
    private String secondaryLabel;
    private String statusLabel;
    private String statusTone;
    private String timestampLabel;
    private Map<String, String> extra;
}
