package com.thinkerscave.staff.service.impl;

import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.StaffProfileUpdateRequest;
import com.thinkerscave.staff.dto.response.PayrollResponse;
import com.thinkerscave.staff.dto.response.ResponsibilityAssignmentResponse;
import com.thinkerscave.staff.dto.response.StaffDetailResponse;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.PayrollRepository;
import com.thinkerscave.staff.repository.ResponsibilityAssignmentRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.service.StaffProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffProfileServiceImpl implements StaffProfileService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PayrollRepository payrollRepository;
    private final ResponsibilityAssignmentRepository assignmentRepository;
    private final StaffServiceImpl staffServiceImpl;
    private final PayrollServiceImpl payrollServiceImpl;

    @Override
    @Transactional(readOnly = true)
    public StaffDetailResponse getMyProfile(String username) {
        Staff staff = getStaffByUsername(username);
        return staffServiceImpl.buildDetailResponse(staff);
    }

    @Override
    @Transactional
    public void updateMyProfile(String username, StaffProfileUpdateRequest request) {
        Staff staff = getStaffByUsername(username);
        if (request.getMobileNumber() != null) staff.setMobileNumber(request.getMobileNumber());
        if (request.getEmergencyContactName() != null) staff.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactRelation() != null) staff.setEmergencyContactRelation(request.getEmergencyContactRelation());
        if (request.getEmergencyContactNumber() != null) staff.setEmergencyContactNumber(request.getEmergencyContactNumber());
        if (request.getReligion() != null) staff.setReligion(request.getReligion());
        if (request.getNationality() != null) staff.setNationality(request.getNationality());
        if (request.getBloodGroup() != null) staff.setBloodGroup(request.getBloodGroup());
        if (request.getPhotoUrl() != null) staff.setPhotoUrl(request.getPhotoUrl());
        staffRepository.save(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsibilityAssignmentResponse> getMyResponsibilities(String username) {
        Staff staff = getStaffByUsername(username);
        return assignmentRepository
                .findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(staff.getStaffId())
                .stream()
                .map(a -> ResponsibilityAssignmentResponse.builder()
                        .assignmentId(a.getAssignmentId())
                        .staffId(staff.getStaffId())
                        .staffName(staff.getFirstName() + " " + staff.getLastName())
                        .staffCode(staff.getStaffCode())
                        .responsibilityId(a.getResponsibility().getResponsibilityId())
                        .responsibilityCode(a.getResponsibility().getResponsibilityCode())
                        .responsibilityName(a.getResponsibility().getResponsibilityName())
                        .scope(a.getScope())
                        .effectiveFrom(a.getEffectiveFrom())
                        .effectiveTo(a.getEffectiveTo())
                        .active(a.getActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> getMyPayrollHistory(String username) {
        return payrollServiceImpl.getMyPayrollHistory(username);
    }

    private Staff getStaffByUsername(String username) {
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username))
                .getId();
        return staffRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + username));
    }
}
