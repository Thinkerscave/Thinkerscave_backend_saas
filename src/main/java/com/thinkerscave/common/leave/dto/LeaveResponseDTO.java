package com.thinkerscave.common.leave.dto;

import com.thinkerscave.common.leave.domain.LeaveRequest.LeaveStatus;
import com.thinkerscave.common.leave.domain.LeaveRequest.LeaveType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LeaveResponseDTO {
    private Long id;
    private Long organizationId;
    private Long staffId;
    private String staffName;
    private String department;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private String reason;
    private LeaveStatus status;
    private String appliedBy;
    private String approvedBy;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
