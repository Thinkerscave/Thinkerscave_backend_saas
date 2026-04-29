package com.thinkerscave.common.leave.service.impl;

import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.leave.domain.LeaveRequest;
import com.thinkerscave.common.leave.domain.LeaveRequest.LeaveStatus;
import com.thinkerscave.common.leave.dto.LeaveRequestDTO;
import com.thinkerscave.common.leave.dto.LeaveResponseDTO;
import com.thinkerscave.common.leave.repository.LeaveRepository;
import com.thinkerscave.common.leave.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRepository;

    /**
     * Returns the current organization ID from the request context.
     * Throws if no org context is set.
     */
    private Long requireOrgId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException(
                    "No organization context set. Ensure X-Organization-ID header is provided or auto-detected.");
        }
        return orgId;
    }

    @Override
    @Transactional
    public LeaveResponseDTO applyLeave(LeaveRequestDTO dto, String appliedBy) {
        Long orgId = requireOrgId();
        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;

        LeaveRequest leave = LeaveRequest.builder()
                .organizationId(orgId)
                .staffId(dto.getStaffId())
                .staffName(dto.getStaffName())
                .department(dto.getDepartment())
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .days((int) days)
                .reason(dto.getReason())
                .status(LeaveStatus.PENDING)
                .appliedBy(appliedBy)
                .build();

        return toDTO(leaveRepository.save(leave));
    }

    @Override
    @Transactional
    public LeaveResponseDTO approveLeave(Long id, String approvedBy) {
        LeaveRequest leave = findOrThrow(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only PENDING leave requests can be approved");
        }
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(approvedBy);
        return toDTO(leaveRepository.save(leave));
    }

    @Override
    @Transactional
    public LeaveResponseDTO rejectLeave(Long id, String reason, String approvedBy) {
        LeaveRequest leave = findOrThrow(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only PENDING leave requests can be rejected");
        }
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setApprovedBy(approvedBy);
        leave.setRejectionReason(reason);
        return toDTO(leaveRepository.save(leave));
    }

    @Override
    @Transactional
    public void cancelLeave(Long id, String requestedBy) {
        LeaveRequest leave = findOrThrow(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException("Only PENDING leave requests can be cancelled");
        }
        leave.setStatus(LeaveStatus.CANCELLED);
        leaveRepository.save(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDTO> getAllLeaveRequests() {
        Long orgId = requireOrgId();
        return leaveRepository.findByOrganizationIdOrderByCreatedDateDesc(orgId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDTO> getMyLeaveRequests(String username) {
        Long orgId = requireOrgId();
        return leaveRepository.findByOrganizationIdAndAppliedByOrderByCreatedDateDesc(orgId, username)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private LeaveRequest findOrThrow(Long id) {
        Long orgId = requireOrgId();
        LeaveRequest leave = leaveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found: " + id));
        // Validate org ownership — prevent cross-org access
        if (!orgId.equals(leave.getOrganizationId())) {
            throw new RuntimeException("Access denied to leave request: " + id);
        }
        return leave;
    }

    private LeaveResponseDTO toDTO(LeaveRequest l) {
        return LeaveResponseDTO.builder()
                .id(l.getId())
                .organizationId(l.getOrganizationId())
                .staffId(l.getStaffId())
                .staffName(l.getStaffName())
                .department(l.getDepartment())
                .leaveType(l.getLeaveType())
                .startDate(l.getStartDate())
                .endDate(l.getEndDate())
                .days(l.getDays())
                .reason(l.getReason())
                .status(l.getStatus())
                .appliedBy(l.getAppliedBy())
                .approvedBy(l.getApprovedBy())
                .rejectionReason(l.getRejectionReason())
                .createdAt(l.getCreatedDate() != null
                        ? l.getCreatedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .build();
    }
}
