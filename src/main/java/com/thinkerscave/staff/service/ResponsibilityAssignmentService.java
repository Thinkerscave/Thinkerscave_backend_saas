package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.request.ResponsibilityAssignmentRequest;
import com.thinkerscave.staff.dto.response.ResponsibilityAssignmentResponse;

import java.util.List;

public interface ResponsibilityAssignmentService {

    Long assignResponsibility(ResponsibilityAssignmentRequest request);

    void removeAssignment(Long assignmentId);

    List<ResponsibilityAssignmentResponse> getStaffResponsibilities(Long staffId);
}
