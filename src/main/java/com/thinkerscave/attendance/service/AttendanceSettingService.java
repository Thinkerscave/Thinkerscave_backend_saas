package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.request.AttendanceSettingRequest;
import com.thinkerscave.attendance.dto.response.AttendanceSettingResponse;

public interface AttendanceSettingService {

    /**
     * Get the attendance settings for the current organization.
     */
    AttendanceSettingResponse getSettings();

    /**
     * Create or update attendance settings.
     */
    AttendanceSettingResponse saveSettings(AttendanceSettingRequest request);

    /**
     * Reset settings to platform defaults.
     */
    AttendanceSettingResponse resetToDefaults();
}
