package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChildItem {
    private Long studentId;
    private String displayName;
    private String className;
    private String sectionName;
    private String rollNumber;
    private String photoUrl;
    private boolean selected;
}
