package com.thinkerscave.attendance.service;

import com.thinkerscave.attendance.dto.request.CreateRegularizationRequest;
import com.thinkerscave.attendance.dto.request.RegularizationDecisionRequest;
import com.thinkerscave.attendance.dto.response.RegularizationRequestResponse;
import com.thinkerscave.attendance.enums.RegularizationRequestStatus;

import java.util.List;

public interface StaffAttendanceRegularizationService {

    RegularizationRequestResponse create(CreateRegularizationRequest request);

    List<RegularizationRequestResponse> listMine(RegularizationRequestStatus statusFilter);

    List<RegularizationRequestResponse> listPendingForApprover();

    long countPendingForApprover();

    RegularizationRequestResponse getById(Long requestId);

    RegularizationRequestResponse approve(Long requestId, RegularizationDecisionRequest decision);

    RegularizationRequestResponse reject(Long requestId, RegularizationDecisionRequest decision);
}
