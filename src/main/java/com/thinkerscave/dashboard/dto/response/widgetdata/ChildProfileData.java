package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** List-based, future-ready for multiple children per parent. */
@Data
@Builder
public class ChildProfileData {
    private List<ChildItem> children;
}
