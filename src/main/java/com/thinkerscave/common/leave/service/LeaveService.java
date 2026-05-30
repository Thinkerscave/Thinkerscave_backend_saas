package com.thinkerscave.common.leave.service;

import com.thinkerscave.common.leave.dto.LeaveRequestDTO;
import com.thinkerscave.common.leave.dto.LeaveResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LeaveService {
    LeaveResponseDTO applyLeave(LeaveRequestDTO dto, String appliedBy);

    LeaveResponseDTO approveLeave(Long id, String approvedBy);

    LeaveResponseDTO rejectLeave(Long id, String reason, String approvedBy);

    void cancelLeave(Long id, String requestedBy);

    List<LeaveResponseDTO> getAllLeaveRequests();

    Page<LeaveResponseDTO> getAllLeaveRequests(Pageable pageable);

    List<LeaveResponseDTO> getMyLeaveRequests(String username);

    Page<LeaveResponseDTO> getMyLeaveRequests(String username, Pageable pageable);
}
