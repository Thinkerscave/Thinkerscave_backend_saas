package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.attendance.dto.request.MarkStaffAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StaffSignInRequest;
import com.thinkerscave.attendance.dto.request.StaffSignOutRequest;
import com.thinkerscave.attendance.dto.response.StaffAttendanceResponse;
import com.thinkerscave.attendance.entity.StaffAttendance;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.attendance.repository.StaffAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.attendance.service.StaffAttendanceService;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private final StaffAttendanceRepository staffAttendanceRepository;
    private final StaffRepository staffRepository;
    private final AttendanceFreezeService attendanceFreezeService;

    @Override
    @Transactional
    public StaffAttendanceResponse markAttendance(MarkStaffAttendanceRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, request.getAttendanceDate());

        Staff staff = staffRepository.findById(request.getStaffId())
                .filter(s -> orgId.equals(s.getOrganizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + request.getStaffId()));

        StaffAttendance attendance = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, request.getStaffId(), request.getAttendanceDate())
                .orElseGet(StaffAttendance::new);

        attendance.setOrganizationId(orgId);
        attendance.setStaffId(staff.getId());
        attendance.setStaffName(staff.getFirstName() + " " + staff.getLastName());
        attendance.setStaffCode(staff.getStaffCode());
        if (staff.getDepartment() != null) {
            attendance.setDepartment(staff.getDepartment().getDepartmentName());
        }
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        if (request.getSignInTime() != null) attendance.setSignInTime(request.getSignInTime());
        if (request.getSignOutTime() != null) attendance.setSignOutTime(request.getSignOutTime());
        attendance.setShift(request.getShift());
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(currentUser());

        computeWorkingMinutes(attendance);

        return toResponse(staffAttendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public StaffAttendanceResponse signIn(StaffSignInRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate today = LocalDate.now();
        validateNotFrozen(orgId, today);

        Staff staff = staffRepository.findById(request.getStaffId())
                .filter(s -> orgId.equals(s.getOrganizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + request.getStaffId()));

        StaffAttendance attendance = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, request.getStaffId(), today)
                .orElseGet(StaffAttendance::new);

        attendance.setOrganizationId(orgId);
        attendance.setStaffId(staff.getId());
        attendance.setStaffName(staff.getFirstName() + " " + staff.getLastName());
        attendance.setStaffCode(staff.getStaffCode());
        if (staff.getDepartment() != null) {
            attendance.setDepartment(staff.getDepartment().getDepartmentName());
        }
        attendance.setAttendanceDate(today);
        attendance.setSignInTime(LocalDateTime.now());
        attendance.setStatus(StaffAttendanceStatus.PRESENT);
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(currentUser());

        return toResponse(staffAttendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public StaffAttendanceResponse signOut(StaffSignOutRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate today = LocalDate.now();

        StaffAttendance attendance = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, request.getStaffId(), today)
                .orElseThrow(() -> new BadRequestException(
                        "No sign-in record found for staff " + request.getStaffId() + " today"));

        attendance.setSignOutTime(LocalDateTime.now());
        if (request.getRemarks() != null) attendance.setRemarks(request.getRemarks());
        computeWorkingMinutes(attendance);

        return toResponse(staffAttendanceRepository.save(attendance));
    }

    @Override
    public List<StaffAttendanceResponse> getTodayAttendance(LocalDate date) {
        Long orgId = OrganizationContext.getOrganizationId();
        return staffAttendanceRepository
                .findByOrganizationIdAndAttendanceDateOrderByStaffName(orgId, date)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<StaffAttendanceResponse> getStaffHistory(Long staffId, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return staffAttendanceRepository
                .findByOrganizationIdAndStaffIdOrderByAttendanceDateDesc(orgId, staffId, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<StaffAttendanceResponse> getStaffAttendanceByRange(Long staffId, LocalDate from, LocalDate to) {
        Long orgId = OrganizationContext.getOrganizationId();
        return staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(orgId, staffId, from, to)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void computeWorkingMinutes(StaffAttendance attendance) {
        if (attendance.getSignInTime() != null && attendance.getSignOutTime() != null) {
            long minutes = java.time.Duration.between(
                    attendance.getSignInTime(), attendance.getSignOutTime()).toMinutes();
            attendance.setWorkingMinutes((int) Math.max(0, minutes));
        }
    }

    private void validateNotFrozen(Long orgId, LocalDate date) {
        if (attendanceFreezeService.isDateFrozen(orgId, date)) {
            throw new BadRequestException("Attendance is frozen for date: " + date);
        }
    }

    private String currentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private StaffAttendanceResponse toResponse(StaffAttendance attendance) {
        return StaffAttendanceResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .staffId(attendance.getStaffId())
                .staffName(attendance.getStaffName())
                .staffCode(attendance.getStaffCode())
                .department(attendance.getDepartment())
                .designation(attendance.getDesignation())
                .attendanceDate(attendance.getAttendanceDate())
                .signInTime(attendance.getSignInTime())
                .signOutTime(attendance.getSignOutTime())
                .workingMinutes(attendance.getWorkingMinutes())
                .shift(attendance.getShift())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .markedBy(attendance.getMarkedBy())
                .build();
    }
}
