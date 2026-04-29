package com.thinkerscave.common.leave.service;

import com.thinkerscave.common.leave.dto.LeaveRequestDTO;
import com.thinkerscave.common.leave.dto.LeaveResponseDTO;

import java.util.List;

public interface LeaveService {
    LeaveResponseDTO applyLeave(LeaveRequestDTO dto, String appliedBy);

    LeaveResponseDTO approveLeave(Long id, String approvedBy);

    LeaveResponseDTO rejectLeave(Long id, String reason, String approvedBy);

    void cancelLeave(Long id, String requestedBy);

    List<LeaveResponseDTO> getAllLeaveRequests();

    List<LeaveResponseDTO> getMyLeaveRequests(String username);
}
