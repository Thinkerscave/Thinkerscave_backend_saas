package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.request.AttendanceFreezeRequest;
import com.thinkerscave.attendance.dto.response.AttendanceFreezeResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceFreezeService {

    /**
     * Create a new freeze period.
     */
    AttendanceFreezeResponse createFreeze(AttendanceFreezeRequest request);

    /**
     * Get all active freeze periods for the current organization.
     */
    List<AttendanceFreezeResponse> getAllFreezes();

    /**
     * Deactivate a freeze by ID.
     */
    void deleteFreeze(Long freezeId);

    /**
     * Check if a given date is within a freeze window.
     */
    boolean isDateFrozen(Long organizationId, LocalDate date);
}
