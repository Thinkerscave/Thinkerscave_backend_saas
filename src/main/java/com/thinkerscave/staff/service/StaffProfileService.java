package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.request.StaffProfileUpdateRequest;
import com.thinkerscave.staff.dto.response.ResponsibilityAssignmentResponse;
import com.thinkerscave.staff.dto.response.StaffDetailResponse;

import java.util.List;

public interface StaffProfileService {

    StaffDetailResponse getMyProfile(String username);

    void updateMyProfile(String username, StaffProfileUpdateRequest request);

    List<ResponsibilityAssignmentResponse> getMyResponsibilities(String username);
}
