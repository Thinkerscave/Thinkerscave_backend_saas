package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.MaintenanceScheduleRequest;
import com.thinkerscave.platform.dto.response.MaintenanceScheduleResponse;

import java.util.List;

public interface MaintenanceService {

    List<MaintenanceScheduleResponse> getAllMaintenanceSchedules();

    MaintenanceScheduleResponse createMaintenanceSchedule(MaintenanceScheduleRequest request);

    MaintenanceScheduleResponse updateMaintenanceSchedule(Long id, MaintenanceScheduleRequest request);

    void deleteMaintenanceSchedule(Long id);
}
