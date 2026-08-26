package com.thinkerscave.staff.service.impl;

import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.BulkResponsibilityAssignmentRequest;
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

import java.time.LocalDate;
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
        return upsertAssignment(request.getStaffId(), request.getResponsibilityId(), request.getScope(),
                request.getEffectiveFrom(), request.getEffectiveTo(), request.getRemarks());
    }

    @Override
    @Transactional
    public void assignStaff(Long responsibilityId, BulkResponsibilityAssignmentRequest request) {
        findResponsibility(responsibilityId);
        LocalDate from = LocalDate.now();
        for (Long staffId : request.getStaffIds()) {
            upsertAssignment(staffId, responsibilityId, null, from, null, null);
        }
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

    @Override
    @Transactional(readOnly = true)
    public List<ResponsibilityAssignmentResponse> getResponsibilityStaff(Long responsibilityId) {
        findResponsibility(responsibilityId);
        return assignmentRepository.findActiveByResponsibilityId(responsibilityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Long upsertAssignment(Long staffId, Long responsibilityId, String scope,
                                  LocalDate effectiveFrom, LocalDate effectiveTo, String remarks) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
        Responsibility responsibility = findResponsibility(responsibilityId);

        ResponsibilityAssignment assignment = assignmentRepository
                .findFirstByStaff_StaffIdAndResponsibility_ResponsibilityIdOrderByAssignmentIdDesc(staffId, responsibilityId)
                .orElseGet(ResponsibilityAssignment::new);
        assignment.setStaff(staff);
        assignment.setResponsibility(responsibility);
        assignment.setScope(scope);
        assignment.setEffectiveFrom(effectiveFrom != null ? effectiveFrom : LocalDate.now());
        assignment.setEffectiveTo(effectiveTo);
        assignment.setRemarks(remarks);
        assignment.setActive(true);
        ResponsibilityAssignment saved = assignmentRepository.save(assignment);
        log.info("Responsibility assigned: {} to staff: {}", responsibility.getResponsibilityCode(), staff.getStaffCode());
        return saved.getAssignmentId();
    }

    private Responsibility findResponsibility(Long responsibilityId) {
        return responsibilityRepository.findById(responsibilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Responsibility not found: " + responsibilityId));
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
                .userId(a.getStaff().getUser() != null ? a.getStaff().getUser().getId() : null)
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
