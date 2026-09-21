package com.thinkerscave.attendance.service.impl;

import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.attendance.dto.request.MarkStaffAttendanceRequest;
import com.thinkerscave.attendance.dto.request.StaffSignInRequest;
import com.thinkerscave.attendance.dto.request.StaffSignOutRequest;
import com.thinkerscave.attendance.dto.response.StaffAttendanceHistoryDayResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceResponse;
import com.thinkerscave.attendance.dto.response.StaffAttendanceTodayResponse;
import com.thinkerscave.attendance.entity.StaffAttendance;
import com.thinkerscave.attendance.entity.StaffAttendanceRegularization;
import com.thinkerscave.attendance.enums.RegularizationRequestStatus;
import com.thinkerscave.attendance.enums.StaffAttendanceStatus;
import com.thinkerscave.attendance.enums.StaffAttendanceWidgetState;
import com.thinkerscave.attendance.repository.StaffAttendanceRegularizationRepository;
import com.thinkerscave.attendance.repository.StaffAttendanceRepository;
import com.thinkerscave.attendance.service.AttendanceFreezeService;
import com.thinkerscave.attendance.service.StaffAttendanceDayContextService;
import com.thinkerscave.attendance.service.StaffAttendanceDayContextService.DayContext;
import com.thinkerscave.attendance.service.StaffAttendanceService;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.shared.constants.ErrorCodes;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    public static final Duration AUTO_CLOSE_AFTER = Duration.ofHours(24);
    public static final int AUTO_CLOSE_WORKING_MINUTES = (int) AUTO_CLOSE_AFTER.toMinutes();

    private final StaffAttendanceRepository staffAttendanceRepository;
    private final StaffAttendanceRegularizationRepository regularizationRepository;
    private final StaffRepository staffRepository;
    private final AttendanceFreezeService attendanceFreezeService;
    private final UserRepository userRepository;
    private final StaffAttendanceDayContextService dayContextService;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public StaffAttendanceResponse markAttendance(MarkStaffAttendanceRequest request) {
        Long orgId = OrganizationContext.getOrganizationId();
        validateNotFrozen(orgId, request.getAttendanceDate());

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + request.getStaffId()));

        StaffAttendance attendance = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, request.getStaffId(), request.getAttendanceDate())
                .orElseGet(StaffAttendance::new);

        populateStaffFields(attendance, staff, orgId);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setStatus(request.getStatus());
        if (request.getSignInTime() != null) attendance.setSignInTime(request.getSignInTime());
        if (request.getSignOutTime() != null) attendance.setSignOutTime(request.getSignOutTime());
        attendance.setShift(request.getShift());
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(currentUser());
        computeWorkingMinutes(attendance);

        return toLegacyResponse(staffAttendanceRepository.save(attendance));
    }

    @Override
    @Transactional
    public StaffAttendanceTodayResponse signIn(StaffSignInRequest request) {
        Staff staff = resolveAuthenticatedStaff(request != null ? request.getStaffId() : null);
        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        autoCloseExpiredForStaff(orgId, staff.getStaffId(), now);
        validateNotFrozen(orgId, today);

        StaffAttendance active = findActiveSession(orgId, staff.getStaffId()).orElse(null);
        if (active != null) {
            log.debug("Sign-in idempotent: returning existing active session {}", active.getAttendanceId());
            return toTodayResponse(staff, active, DayContext.workingDay());
        }

        StaffAttendance todayRecord = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, staff.getStaffId(), today)
                .orElse(null);
        if (todayRecord != null && todayRecord.getSignOutTime() != null) {
            throw new BadRequestException(
                    ErrorCodes.ATTENDANCE_ALREADY_COMPLETED,
                    "Today's attendance is already completed. Use regularization for corrections.");
        }

        DayContext day = dayContextService.resolve(orgId, staff.getStaffId(), today);
        if (!day.attendanceRequired()) {
            throw new BadRequestException(
                    ErrorCodes.ATTENDANCE_NOT_REQUIRED,
                    "Attendance is not required today (" + day.reason() + ").");
        }

        StaffAttendance attendance = todayRecord != null ? todayRecord : new StaffAttendance();
        populateStaffFields(attendance, staff, orgId);
        attendance.setAttendanceDate(today);
        attendance.setSignInTime(now);
        attendance.setSignOutTime(null);
        attendance.setWorkingMinutes(null);
        attendance.setAutoClosed(false);
        attendance.setStatus(StaffAttendanceStatus.PRESENT);
        if (request != null) {
            attendance.setRemarks(request.getRemarks());
        }
        attendance.setMarkedBy(currentUser());

        StaffAttendance saved = staffAttendanceRepository.save(attendance);
        log.info("Staff {} signed in at {} (attendanceId={})", staff.getStaffId(), now, saved.getAttendanceId());
        return toTodayResponse(staff, saved, day);
    }

    @Override
    @Transactional
    public StaffAttendanceTodayResponse signOut(StaffSignOutRequest request) {
        Staff staff = resolveAuthenticatedStaff(request != null ? request.getStaffId() : null);
        Long orgId = OrganizationContext.getOrganizationId();
        LocalDateTime now = LocalDateTime.now();

        autoCloseExpiredForStaff(orgId, staff.getStaffId(), now);

        StaffAttendance attendance = findActiveSession(orgId, staff.getStaffId())
                .orElseThrow(() -> new BadRequestException(
                        ErrorCodes.ATTENDANCE_NOT_ACTIVE,
                        "No active attendance session to sign out of."));

        attendance.setSignOutTime(now);
        attendance.setAutoClosed(false);
        if (request != null && request.getRemarks() != null) {
            attendance.setRemarks(request.getRemarks());
        }
        computeWorkingMinutes(attendance);

        StaffAttendance saved = staffAttendanceRepository.save(attendance);
        log.info("Staff {} signed out at {} (workingMinutes={})",
                staff.getStaffId(), now, saved.getWorkingMinutes());

        DayContext day = DayContext.workingDay();
        return toTodayResponse(staff, saved, day);
    }

    @Override
    public List<StaffAttendanceResponse> getTodayAttendance(LocalDate date) {
        Long orgId = OrganizationContext.getOrganizationId();
        return staffAttendanceRepository
                .findByOrganizationIdAndAttendanceDateOrderByStaffName(orgId, date)
                .stream()
                .map(this::toLegacyResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<StaffAttendanceResponse> getStaffHistory(Long staffId, Pageable pageable) {
        Long orgId = OrganizationContext.getOrganizationId();
        return staffAttendanceRepository
                .findByOrganizationIdAndStaffIdOrderByAttendanceDateDesc(orgId, staffId, pageable)
                .map(this::toLegacyResponse);
    }

    @Override
    public List<StaffAttendanceResponse> getStaffAttendanceByRange(Long staffId, LocalDate from, LocalDate to) {
        Long orgId = OrganizationContext.getOrganizationId();
        return staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(orgId, staffId, from, to)
                .stream()
                .map(this::toLegacyResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StaffAttendanceTodayResponse getMyTodayStatus(String username) {
        Staff staff = userRepository.findByUsername(username)
                .flatMap(u -> staffRepository.findByUser_Id(u.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + username));
        return buildTodayStatus(staff);
    }

    @Override
    @Transactional
    public StaffAttendanceTodayResponse buildTodayStatus(Staff staff) {
        if (staff == null) {
            return StaffAttendanceTodayResponse.builder()
                    .date(LocalDate.now())
                    .state(StaffAttendanceWidgetState.NOT_STARTED)
                    .attendanceRequired(true)
                    .active(false)
                    .autoClosed(false)
                    .workingMinutes(0)
                    .build();
        }

        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        autoCloseExpiredForStaff(orgId, staff.getStaffId(), now);

        // Priority: Leave → Holiday → Weekend → Active → Completed → Not started
        DayContext day = dayContextService.resolve(orgId, staff.getStaffId(), today);
        if (day.onLeave()) {
            return nonWorkingResponse(staff, today, StaffAttendanceWidgetState.ON_LEAVE, day);
        }
        if (day.holiday()) {
            return nonWorkingResponse(staff, today, StaffAttendanceWidgetState.HOLIDAY, day);
        }
        if (day.weekend()) {
            return nonWorkingResponse(staff, today, StaffAttendanceWidgetState.WEEKEND, day);
        }

        StaffAttendance active = findActiveSession(orgId, staff.getStaffId()).orElse(null);
        if (active != null) {
            return toTodayResponse(staff, active, day);
        }

        StaffAttendance todayRecord = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDate(orgId, staff.getStaffId(), today)
                .orElse(null);
        if (todayRecord != null && todayRecord.getSignOutTime() != null) {
            return toTodayResponse(staff, todayRecord, day);
        }

        return StaffAttendanceTodayResponse.builder()
                .staffId(staff.getStaffId())
                .staffName(staffDisplayName(staff))
                .organizationName(resolveOrganizationName(orgId))
                .date(today)
                .state(StaffAttendanceWidgetState.NOT_STARTED)
                .status(null)
                .attendanceRequired(true)
                .active(false)
                .autoClosed(false)
                .workingMinutes(0)
                .build();
    }

    @Override
    @Transactional
    public int autoCloseExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(AUTO_CLOSE_AFTER);
        List<StaffAttendance> expired = staffAttendanceRepository.findExpiredActiveSessions(cutoff);
        for (StaffAttendance attendance : expired) {
            closeExpired(attendance);
        }
        if (!expired.isEmpty()) {
            staffAttendanceRepository.saveAll(expired);
            log.info("Auto-closed {} expired staff attendance session(s)", expired.size());
        }
        return expired.size();
    }

    @Override
    @Transactional
    public List<StaffAttendanceHistoryDayResponse> getMyHistory(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("Invalid date range");
        }
        Staff staff = resolveAuthenticatedStaff(null);
        Long orgId = OrganizationContext.getOrganizationId();
        LocalDate today = LocalDate.now();

        autoCloseExpiredForStaff(orgId, staff.getStaffId(), LocalDateTime.now());

        Map<LocalDate, StaffAttendance> byDate = staffAttendanceRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        orgId, staff.getStaffId(), from, to)
                .stream()
                .collect(Collectors.toMap(StaffAttendance::getAttendanceDate, a -> a, (a, b) -> a));

        Map<LocalDate, StaffAttendanceRegularization> regs = regularizationRepository
                .findByOrganizationIdAndStaffIdAndAttendanceDateBetween(orgId, staff.getStaffId(), from, to)
                .stream()
                .collect(Collectors.toMap(
                        StaffAttendanceRegularization::getAttendanceDate,
                        r -> r,
                        (a, b) -> preferReg(a, b)));

        List<StaffAttendanceHistoryDayResponse> days = new java.util.ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(buildHistoryDay(staff, orgId, date, today, byDate.get(date), regs.get(date)));
        }
        return days;
    }

    private StaffAttendanceRegularization preferReg(
            StaffAttendanceRegularization a, StaffAttendanceRegularization b) {
        // Prefer PENDING, then latest requested
        if (a.getStatus() == RegularizationRequestStatus.PENDING) return a;
        if (b.getStatus() == RegularizationRequestStatus.PENDING) return b;
        if (a.getRequestedAt() != null && b.getRequestedAt() != null) {
            return a.getRequestedAt().isAfter(b.getRequestedAt()) ? a : b;
        }
        return a;
    }

    private StaffAttendanceHistoryDayResponse buildHistoryDay(
            Staff staff,
            Long orgId,
            LocalDate date,
            LocalDate today,
            StaffAttendance attendance,
            StaffAttendanceRegularization reg) {
        DayContext day = dayContextService.resolve(orgId, staff.getStaffId(), date);

        String displayStatus;
        StaffAttendanceStatus status = null;
        boolean canRegularize = false;

        if (day.onLeave()) {
            displayStatus = "ON_LEAVE";
            status = StaffAttendanceStatus.ON_LEAVE;
        } else if (day.holiday()) {
            displayStatus = "HOLIDAY";
        } else if (day.weekend()) {
            displayStatus = "WEEKEND";
        } else if (attendance != null && attendance.getSignInTime() != null) {
            status = attendance.getStatus() != null ? attendance.getStatus() : StaffAttendanceStatus.PRESENT;
            if (attendance.getSignOutTime() == null && date.equals(today)) {
                displayStatus = "PRESENT"; // active today
            } else {
                displayStatus = status.name();
            }
            canRegularize = true;
        } else if (date.isAfter(today)) {
            displayStatus = "FUTURE";
        } else if (date.equals(today)) {
            displayStatus = "NOT_STARTED";
            canRegularize = false; // use sign-in for today
        } else {
            displayStatus = "ABSENT";
            status = StaffAttendanceStatus.ABSENT;
            canRegularize = true;
        }

        if (!day.attendanceRequired()) {
            canRegularize = false;
        }
        if (reg != null && reg.getStatus() == RegularizationRequestStatus.PENDING) {
            canRegularize = false;
        }
        if (reg != null && reg.getStatus() == RegularizationRequestStatus.APPROVED) {
            canRegularize = false;
        }

        return StaffAttendanceHistoryDayResponse.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek().name())
                .displayStatus(displayStatus)
                .status(status)
                .attendanceId(attendance != null ? attendance.getAttendanceId() : null)
                .signInTime(attendance != null ? attendance.getSignInTime() : null)
                .signOutTime(attendance != null ? attendance.getSignOutTime() : null)
                .workingMinutes(attendance != null ? attendance.getWorkingMinutes() : null)
                .attendanceRequired(day.attendanceRequired())
                .autoClosed(attendance != null && attendance.isAutoClosed())
                .holidayName(day.holidayName())
                .leaveLabel(day.leaveLabel())
                .regularizationStatus(reg != null ? reg.getStatus() : null)
                .regularizationRequestId(reg != null ? reg.getRequestId() : null)
                .canRegularize(canRegularize)
                .build();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void autoCloseExpiredForStaff(Long orgId, Long staffId, LocalDateTime now) {
        findActiveSession(orgId, staffId).ifPresent(active -> {
            if (active.getSignInTime() != null
                    && !active.getSignInTime().plus(AUTO_CLOSE_AFTER).isAfter(now)) {
                closeExpired(active);
                staffAttendanceRepository.save(active);
            }
        });
    }

    private void closeExpired(StaffAttendance attendance) {
        LocalDateTime signOut = attendance.getSignInTime().plus(AUTO_CLOSE_AFTER);
        attendance.setSignOutTime(signOut);
        attendance.setWorkingMinutes(AUTO_CLOSE_WORKING_MINUTES);
        attendance.setAutoClosed(true);
        if (attendance.getStatus() == null) {
            attendance.setStatus(StaffAttendanceStatus.PRESENT);
        }
        log.info("Auto-closed attendance {} for staff {} (signIn={}, signOut={})",
                attendance.getAttendanceId(), attendance.getStaffId(),
                attendance.getSignInTime(), signOut);
    }

    private java.util.Optional<StaffAttendance> findActiveSession(Long orgId, Long staffId) {
        return staffAttendanceRepository
                .findFirstByOrganizationIdAndStaffIdAndSignOutTimeIsNullAndSignInTimeIsNotNullOrderBySignInTimeDesc(
                        orgId, staffId);
    }

    private Staff resolveAuthenticatedStaff(Long requestedStaffId) {
        String username = currentUser();
        Staff staff = userRepository.findByUsername(username)
                .flatMap(u -> staffRepository.findByUser_Id(u.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user: " + username));

        if (!Boolean.TRUE.equals(staff.getActive())) {
            throw new BadRequestException("Staff profile is not active.");
        }
        if (requestedStaffId != null && !requestedStaffId.equals(staff.getStaffId())) {
            throw new BadRequestException(
                    ErrorCodes.FORBIDDEN,
                    "Cannot sign in/out on behalf of another staff member.");
        }
        return staff;
    }

    private void populateStaffFields(StaffAttendance attendance, Staff staff, Long orgId) {
        attendance.setOrganizationId(orgId);
        attendance.setStaffId(staff.getStaffId());
        attendance.setStaffName(staffDisplayName(staff));
        attendance.setStaffCode(staff.getStaffCode());
        if (staff.getDesignation() != null) {
            attendance.setDepartment(staff.getDesignation());
            attendance.setDesignation(staff.getDesignation());
        }
    }

    private StaffAttendanceTodayResponse nonWorkingResponse(
            Staff staff, LocalDate today, StaffAttendanceWidgetState state, DayContext day) {
        return StaffAttendanceTodayResponse.builder()
                .staffId(staff.getStaffId())
                .staffName(staffDisplayName(staff))
                .organizationName(resolveOrganizationName(OrganizationContext.getOrganizationId()))
                .date(today)
                .state(state)
                .status(state == StaffAttendanceWidgetState.ON_LEAVE ? StaffAttendanceStatus.ON_LEAVE : null)
                .attendanceRequired(false)
                .active(false)
                .autoClosed(false)
                .workingMinutes(0)
                .holidayName(day.holidayName())
                .leaveLabel(day.leaveLabel())
                .reason(day.reason())
                .build();
    }

    private StaffAttendanceTodayResponse toTodayResponse(Staff staff, StaffAttendance attendance, DayContext day) {
        boolean completed = attendance.getSignOutTime() != null;
        boolean active = attendance.getSignInTime() != null && !completed;

        StaffAttendanceWidgetState state;
        if (active) {
            state = StaffAttendanceWidgetState.ACTIVE;
        } else if (completed) {
            state = StaffAttendanceWidgetState.COMPLETED;
        } else {
            state = StaffAttendanceWidgetState.NOT_STARTED;
        }

        Integer workingMinutes = attendance.getWorkingMinutes();
        if (workingMinutes == null && active && attendance.getSignInTime() != null) {
            workingMinutes = (int) Math.max(0,
                    Duration.between(attendance.getSignInTime(), LocalDateTime.now()).toMinutes());
        }
        if (workingMinutes == null) {
            workingMinutes = 0;
        }

        return StaffAttendanceTodayResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .staffId(staff.getStaffId())
                .staffName(staffDisplayName(staff))
                .organizationName(resolveOrganizationName(attendance.getOrganizationId()))
                .date(attendance.getAttendanceDate() != null ? attendance.getAttendanceDate() : LocalDate.now())
                .state(state)
                .status(attendance.getStatus())
                .attendanceRequired(day == null || day.attendanceRequired())
                .active(active)
                .autoClosed(attendance.isAutoClosed())
                .signInTime(attendance.getSignInTime())
                .signOutTime(attendance.getSignOutTime())
                .workingMinutes(workingMinutes)
                .holidayName(day != null ? day.holidayName() : null)
                .leaveLabel(day != null ? day.leaveLabel() : null)
                .reason(day != null ? day.reason() : null)
                .build();
    }

    private void computeWorkingMinutes(StaffAttendance attendance) {
        if (attendance.getSignInTime() != null && attendance.getSignOutTime() != null) {
            long minutes = Duration.between(attendance.getSignInTime(), attendance.getSignOutTime()).toMinutes();
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

    private String staffDisplayName(Staff staff) {
        return ((staff.getFirstName() != null ? staff.getFirstName() : "")
                + " "
                + (staff.getLastName() != null ? staff.getLastName() : "")).trim();
    }

    private String resolveOrganizationName(Long orgId) {
        if (orgId == null) {
            return null;
        }
        return organizationRepository.findById(orgId)
                .map(org -> org.getOrganizationName())
                .orElse(null);
    }

    private StaffAttendanceResponse toLegacyResponse(StaffAttendance attendance) {
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
                .autoClosed(attendance.isAutoClosed())
                .active(attendance.getSignInTime() != null && attendance.getSignOutTime() == null)
                .state(resolveLegacyState(attendance))
                .build();
    }

    private StaffAttendanceWidgetState resolveLegacyState(StaffAttendance attendance) {
        if (attendance.getSignInTime() != null && attendance.getSignOutTime() == null) {
            return StaffAttendanceWidgetState.ACTIVE;
        }
        if (attendance.getSignOutTime() != null) {
            return StaffAttendanceWidgetState.COMPLETED;
        }
        return StaffAttendanceWidgetState.NOT_STARTED;
    }
}
