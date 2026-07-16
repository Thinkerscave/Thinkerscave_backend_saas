package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransportSummaryData {
    private String routeName;
    private String vehicleNumber;
    private String pickupTime;
    private String dropTime;
    private String liveStatus;
}
