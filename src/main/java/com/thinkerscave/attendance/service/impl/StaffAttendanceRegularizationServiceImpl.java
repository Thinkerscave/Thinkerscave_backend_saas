package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.attendance.dto.request.CreateRegularizationRequest;
import com.thinkerscave.attendance.dto.request.RegularizationDecisionRequest;
import com.thinkerscave.attendance.dto.response.RegularizationRequestResponse;
import com.thinkerscave.attendance.entity.StaffAttendance;
import com.thinkerscave.attendance.entity.StaffAttendanceRegularization;
import com.thinkerscave.attendance.entity.StaffAttendanceRegularizationAudit;
import com.thinkerscave.attendance.enums.RegularizationRequestStatus;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.attendance.repository.StaffAttendanceRegularizationAuditRepository;
import com.thinkerscave.attendance.repository.StaffAttendanceRegularizationRepository;
import com.thinkerscave.attendance.repository.StaffAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.attendance.service.StaffAttendanceDayContextService;
import com.thinkerscave.attendance.service.StaffAttendanceDayContextService.DayContext;
import com.thinkerscave.attendance.service.StaffAttendanceRegularizationService;
import com.thinkerscave.shared.constants.ErrorCodes;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StaffAttendanceRegularizationServiceImpl implements StaffAttendanceRegularizationService {

    private final StaffAttendanceRegularizationRepository regularizationRepository;
    private final StaffAttendanceRegularizationAuditRepository auditRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final StaffAttendanceDayContextService dayContextService;
    private final AttendanceFreezeService attendanceFreezeService;

    @Override
    @Transactional
    public RegularizationRequestResponse create(CreateRegularizationRequest request) {
        Staff staff = resolveAuthenticatedStaff();
        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate date = request.getAttendanceDate();

        if (date == null) {
            throw new BadRequestException("Attendance date is required");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Cannot regularize a future date");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("Reason is required");
        }

        DayContext day = dayContextService.resolve(orgId, staff.getStaffId(), date);
        if (!day.attendanceRequired()) {
            throw new BadRequestException(
                    ErrorCodes.ATTENDANCE_NOT_REQUIRED,
                    "Regularization is not allowed for " + day.reason());
        }

        if (attendanceFreezeService.isDateFrozen(orgId, date)) {
            throw new BadRequestException("Attendance is frozen for date: " + date);
        }

        if (regularizationRepository.existsByOrganizationIdAndStaffIdAndAttendanceDateAndStatus(
                orgId, staff.getStaffId(), date, RegularizationRequestStatus.PENDING)) {
            throw new BadRequestException(
                    ErrorCodes.DUPLICATE_RECORD,
                    "A pending regularization request already exists for this date");
        }

        validateRequestedTimes(request);

        StaffAttendance existing = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, staff.getStaffId(), date)
                .orElse(null);

        Integer workingMinutes = computeRequestedMinutes(
                request.getRequestedSignInTime(), request.getRequestedSignOutTime());

        StaffAttendanceRegularization row = new StaffAttendanceRegularization();
        row.setOrganizationId(orgId);
        row.setStaffId(staff.getStaffId());
        row.setStaffName(displayName(staff));
        row.setDepartment(staff.getDesignation());
        row.setAttendanceId(existing != null ? existing.getAttendanceId() : null);
        row.setAttendanceDate(date);
        row.setRequestedStatus(request.getRequestedStatus());
        row.setRequestedSignInTime(request.getRequestedSignInTime());
        row.setRequestedSignOutTime(request.getRequestedSignOutTime());
        row.setRequestedWorkingMinutes(workingMinutes);
        row.setReason(request.getReason().trim());
        row.setRemarks(request.getRemarks());
        row.setStatus(RegularizationRequestStatus.PENDING);
        row.setRequestedBy(currentUser());
        row.setRequestedAt(LocalDateTime.now());

        if (existing != null) {
            row.setPreviousStatus(existing.getStatus());
            row.setPreviousSignInTime(existing.getSignInTime());
            row.setPreviousSignOutTime(existing.getSignOutTime());
            row.setPreviousWorkingMinutes(existing.getWorkingMinutes());
        } else {
            row.setPreviousStatus(StaffAttendanceStatus.ABSENT);
        }

        StaffAttendanceRegularization saved = regularizationRepository.save(row);
        log.info("Regularization request {} created for staff {} on {}",
                saved.getRequestId(), staff.getStaffId(), date);
        return toResponse(saved, existing);
    }

    @Override
    public List<RegularizationRequestResponse> listMine(RegularizationRequestStatus statusFilter) {
        Staff staff = resolveAuthenticatedStaff();
        Long orgId = OrganizationContext.getOrganizationId();
        List<StaffAttendanceRegularization> rows = statusFilter == null
                ? regularizationRepository.findByOrganizationIdAndStaffIdOrderByRequestedAtDesc(orgId, staff.getStaffId())
                : regularizationRepository.findByOrganizationIdAndStaffIdAndStatusOrderByRequestedAtDesc(
                        orgId, staff.getStaffId(), statusFilter);
        return rows.stream().map(r -> toResponse(r, null)).collect(Collectors.toList());
    }

    @Override
    public List<RegularizationRequestResponse> listPendingForApprover() {
        Long orgId = OrganizationContext.getOrganizationId();
        return regularizationRepository
                .findByOrganizationIdAndStatusOrderByRequestedAtAsc(orgId, RegularizationRequestStatus.PENDING)
                .stream()
                .map(r -> toResponse(r, null))
                .collect(Collectors.toList());
    }

    @Override
    public long countPendingForApprover() {
        Long orgId = OrganizationContext.getOrganizationId();
        return regularizationRepository.countByOrganizationIdAndStatus(orgId, RegularizationRequestStatus.PENDING);
    }

    @Override
    public RegularizationRequestResponse getById(Long requestId) {
        Long orgId = OrganizationContext.getOrganizationId();
        StaffAttendanceRegularization row = regularizationRepository
                .findByOrganizationIdAndRequestId(orgId, requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Regularization request not found: " + requestId));
        StaffAttendance current = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, row.getStaffId(), row.getAttendanceDate())
                .orElse(null);
        return toResponse(row, current);
    }

    @Override
    @Transactional
    public RegularizationRequestResponse approve(Long requestId, RegularizationDecisionRequest decision) {
        return decide(requestId, decision, RegularizationRequestStatus.APPROVED);
    }

    @Override
    @Transactional
    public RegularizationRequestResponse reject(Long requestId, RegularizationDecisionRequest decision) {
        return decide(requestId, decision, RegularizationRequestStatus.REJECTED);
    }

    private RegularizationRequestResponse decide(
            Long requestId,
            RegularizationDecisionRequest decision,
            RegularizationRequestStatus action) {
        if (decision == null || decision.getComment() == null || decision.getComment().isBlank()) {
            throw new BadRequestException("Comment is required");
        }

        Long orgId = OrganizationContext.getOrganizationId();
        StaffAttendanceRegularization row = regularizationRepository
                .findByOrganizationIdAndRequestId(orgId, requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Regularization request not found: " + requestId));

        if (row.getStatus() != RegularizationRequestStatus.PENDING) {
            throw new BadRequestException(
                    ErrorCodes.CONFLICT,
                    "This request has already been processed. Please refresh the list.");
        }

        LocalDateTime now = LocalDateTime.now();
        String actor = currentUser();

        StaffAttendance attendance = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, row.getStaffId(), row.getAttendanceDate())
                .orElse(null);

        // Snapshot previous for audit (prefer live attendance, else stored previous)
        StaffAttendanceStatus previousStatus = attendance != null
                ? attendance.getStatus()
                : row.getPreviousStatus();
        LocalDateTime previousIn = attendance != null ? attendance.getSignInTime() : row.getPreviousSignInTime();
        LocalDateTime previousOut = attendance != null ? attendance.getSignOutTime() : row.getPreviousSignOutTime();

        if (action == RegularizationRequestStatus.APPROVED) {
            attendance = applyAttendanceUpdate(row, attendance, orgId);
        }

        row.setStatus(action);
        row.setDecisionBy(actor);
        row.setDecisionAt(now);
        row.setDecisionComment(decision.getComment().trim());

        try {
            row = regularizationRepository.saveAndFlush(row);
        } catch (OptimisticLockingFailureException ex) {
            throw new BadRequestException(
                    ErrorCodes.CONFLICT,
                    "This request has already been processed. Please refresh the list.");
        }

        StaffAttendanceRegularizationAudit audit = new StaffAttendanceRegularizationAudit();
        audit.setOrganizationId(orgId);
        audit.setRequestId(row.getRequestId());
        audit.setStaffId(row.getStaffId());
        audit.setStaffName(row.getStaffName());
        audit.setAttendanceDate(row.getAttendanceDate());
        audit.setPreviousStatus(previousStatus);
        audit.setPreviousSignInTime(previousIn);
        audit.setPreviousSignOutTime(previousOut);
        audit.setRequestedStatus(row.getRequestedStatus());
        audit.setRequestedSignInTime(row.getRequestedSignInTime());
        audit.setRequestedSignOutTime(row.getRequestedSignOutTime());
        audit.setAction(action);
        audit.setComment(decision.getComment().trim());
        audit.setActionBy(actor);
        audit.setActionAt(now);
        auditRepository.save(audit);

        log.info("Regularization {} {} by {}", requestId, action, actor);
        return toResponse(row, attendance);
    }

    private StaffAttendance applyAttendanceUpdate(
            StaffAttendanceRegularization row,
            StaffAttendance attendance,
            Long orgId) {
        if (attendance == null) {
            attendance = new StaffAttendance();
            attendance.setOrganizationId(orgId);
            attendance.setStaffId(row.getStaffId());
            attendance.setStaffName(row.getStaffName());
            attendance.setDepartment(row.getDepartment());
            attendance.setAttendanceDate(row.getAttendanceDate());
        }
        attendance.setStatus(row.getRequestedStatus());
        attendance.setSignInTime(row.getRequestedSignInTime());
        attendance.setSignOutTime(row.getRequestedSignOutTime());
        attendance.setWorkingMinutes(row.getRequestedWorkingMinutes());
        attendance.setAutoClosed(false);
        attendance.setMarkedBy(currentUser());
        attendance.setRemarks("Regularization #" + row.getRequestId());
        return staffAttendanceRepository.save(attendance);
    }

    private void validateRequestedTimes(CreateRegularizationRequest request) {
        StaffAttendanceStatus status = request.getRequestedStatus();
        if (status == StaffAttendanceStatus.PRESENT
                || status == StaffAttendanceStatus.LATE
                || status == StaffAttendanceStatus.HALF_DAY
                || status == StaffAttendanceStatus.WFH) {
            if (request.getRequestedSignInTime() == null || request.getRequestedSignOutTime() == null) {
                throw new BadRequestException("Sign-in and sign-out times are required for " + status);
            }
            if (!request.getRequestedSignOutTime().isAfter(request.getRequestedSignInTime())) {
                throw new BadRequestException("Sign-out must be after sign-in");
            }
        }
    }

    private Integer computeRequestedMinutes(LocalDateTime in, LocalDateTime out) {
        if (in == null || out == null) {
            return null;
        }
        return (int) Math.max(0, Duration.between(in, out).toMinutes());
    }

    private Staff resolveAuthenticatedStaff() {
        String username = currentUser();
        return userRepository.findByUsername(username)
                .flatMap(u -> staffRepository.findByUser_Id(u.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + username));
    }

    private String displayName(Staff staff) {
        return ((staff.getFirstName() != null ? staff.getFirstName() : "")
                + " "
                + (staff.getLastName() != null ? staff.getLastName() : "")).trim();
    }

    private String currentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    private RegularizationRequestResponse toResponse(StaffAttendanceRegularization row, StaffAttendance current) {
        return RegularizationRequestResponse.builder()
                .requestId(row.getRequestId())
                .staffId(row.getStaffId())
                .staffName(row.getStaffName())
                .department(row.getDepartment())
                .attendanceId(row.getAttendanceId())
                .attendanceDate(row.getAttendanceDate())
                .requestedStatus(row.getRequestedStatus())
                .requestedSignInTime(row.getRequestedSignInTime())
                .requestedSignOutTime(row.getRequestedSignOutTime())
                .requestedWorkingMinutes(row.getRequestedWorkingMinutes())
                .reason(row.getReason())
                .remarks(row.getRemarks())
                .status(row.getStatus())
                .requestedBy(row.getRequestedBy())
                .requestedAt(row.getRequestedAt())
                .decisionBy(row.getDecisionBy())
                .decisionAt(row.getDecisionAt())
                .decisionComment(row.getDecisionComment())
                .previousStatus(row.getPreviousStatus())
                .previousSignInTime(row.getPreviousSignInTime())
                .previousSignOutTime(row.getPreviousSignOutTime())
                .previousWorkingMinutes(row.getPreviousWorkingMinutes())
                .currentStatus(current != null ? current.getStatus() : row.getPreviousStatus())
                .currentSignInTime(current != null ? current.getSignInTime() : row.getPreviousSignInTime())
                .currentSignOutTime(current != null ? current.getSignOutTime() : row.getPreviousSignOutTime())
                .currentWorkingMinutes(current != null ? current.getWorkingMinutes() : row.getPreviousWorkingMinutes())
                .build();
    }
}
