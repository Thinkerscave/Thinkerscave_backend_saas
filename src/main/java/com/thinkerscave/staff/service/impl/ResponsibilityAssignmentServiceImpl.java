package com.thinkerscave.staff.service.impl;

import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.ResponsibilityAssignmentRequest;
import com.thinkerscave.staff.dto.response.ResponsibilityAssignmentResponse;
import com.thinkerscave.staff.entity.Responsibility;
import com.thinkerscave.staff.entity.ResponsibilityAssignment;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.ResponsibilityRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.service.ResponsibilityAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponsibilityAssignmentServiceImpl implements ResponsibilityAssignmentService {

    private final ResponsibilityAssignmentRepository assignmentRepository;
    private final StaffRepository staffRepository;
    private final ResponsibilityRepository responsibilityRepository;

    @Override
    @Transactional
    public Long assignResponsibility(ResponsibilityAssignmentRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + request.getStaffId()));
        Responsibility responsibility = responsibilityRepository.findById(request.getResponsibilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Responsibility not found: " + request.getResponsibilityId()));

        ResponsibilityAssignment assignment = new ResponsibilityAssignment();
        assignment.setStaff(staff);
        assignment.setResponsibility(responsibility);
        assignment.setScope(request.getScope());
        assignment.setEffectiveFrom(request.getEffectiveFrom());
        assignment.setEffectiveTo(request.getEffectiveTo());
        assignment.setRemarks(request.getRemarks());
        assignment.setActive(true);

        ResponsibilityAssignment saved = assignmentRepository.save(assignment);
        log.info("Responsibility assigned: {} to staff: {}", responsibility.getResponsibilityCode(), staff.getStaffCode());
        return saved.getAssignmentId();
    }

    @Override
    @Transactional
    public void removeAssignment(Long assignmentId) {
        ResponsibilityAssignment assignment = getEntity(assignmentId);
        assignment.setActive(false);
        assignmentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsibilityAssignmentResponse> getStaffResponsibilities(Long staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff not found: " + staffId);
        }
        return assignmentRepository
                .findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(staffId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ResponsibilityAssignment getEntity(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + id));
    }

    private ResponsibilityAssignmentResponse toResponse(ResponsibilityAssignment a) {
        return ResponsibilityAssignmentResponse.builder()
                .assignmentId(a.getAssignmentId())
                .staffId(a.getStaff().getStaffId())
                .staffName(a.getStaff().getFirstName() + " " + a.getStaff().getLastName())
                .staffCode(a.getStaff().getStaffCode())
                .responsibilityId(a.getResponsibility().getResponsibilityId())
                .responsibilityCode(a.getResponsibility().getResponsibilityCode())
                .responsibilityName(a.getResponsibility().getResponsibilityName())
                .scope(a.getScope())
                .effectiveFrom(a.getEffectiveFrom())
                .effectiveTo(a.getEffectiveTo())
                .active(a.getActive())
                .remarks(a.getRemarks())
                .createdOn(a.getCreatedOn())
                .build();
    }
}
