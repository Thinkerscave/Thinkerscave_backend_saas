package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.BulkResponsibilityAssignmentRequest;
import com.thinkerscave.access.dto.request.ResponsibilityAssignmentRequest;
import com.thinkerscave.access.dto.response.ResponsibilityAssignmentResponse;

import java.util.List;

public interface ResponsibilityAssignmentService {

    Long assignResponsibility(ResponsibilityAssignmentRequest request);

    void assignStaff(Long responsibilityId, BulkResponsibilityAssignmentRequest request);

    void removeAssignment(Long assignmentId);

    List<ResponsibilityAssignmentResponse> getResponsibilityStaff(Long responsibilityId);
}
