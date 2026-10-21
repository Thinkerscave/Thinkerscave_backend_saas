package com.thinkerscave.attendance.service;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.academics.entity.TeacherAllocationTeacher;
import com.thinkerscave.academics.entity.TeacherArrangement;
import com.thinkerscave.academics.entity.TimetableEntry;
import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.enums.TeacherArrangementStatus;
import com.thinkerscave.academics.enums.TimetableEntryType;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.academics.repository.TeacherArrangementRepository;
import com.thinkerscave.academics.repository.TimetableEntryRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.attendance.enums.AttendanceMode;
import com.thinkerscave.attendance.security.AttendanceAccessGuard;
import com.thinkerscave.settings.ModuleSettingCatalog;
import com.thinkerscave.settings.ModuleSettingsService;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeacherAttendanceAuthorizationService {

    private final AttendanceAccessGuard accessGuard;
    private final StaffRepository staffRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TimetableVersionRepository timetableVersionRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TeacherArrangementRepository arrangementRepository;
    private final ModuleSettingsService moduleSettingsService;

    @Transactional(readOnly = true)
    public List<TeacherTeachingSlot> slotsForTeacher(Long staffId, LocalDate date) {
        var year = academicYearRepository.findByCurrentYearTrue().orElse(null);
        if (year == null) {
            return List.of();
        }
        TimetableVersion version = timetableVersionRepository
                .findByAcademicYear_AcademicYearIdAndStatus(year.getAcademicYearId(), TimetableStatus.PUBLISHED)
                .orElse(null);
        if (version == null) {
            return List.of();
        }

        AttendanceMode mode = attendanceMode();
        DayOfWeek day = DayOfWeek.valueOf(date.getDayOfWeek().name());
        List<TimetableEntry> entries = timetableEntryRepository
                .findByTimetableVersion_TimetableVersionIdAndDayOfWeek(version.getTimetableVersionId(), day)
                .stream()
                .filter(this::isTeachingEntry)
                .toList();

        Map<Long, Long> primaryStaffByAllocation = new HashMap<>();
        for (TeacherAllocationTeacher assignment : allocationTeacherRepository.findActiveByYear(year.getAcademicYearId())) {
            Staff assignedStaff = assignment.getStaff();
            if (assignment.getRole() == TeacherAllocationTeacherRole.PRIMARY
                    && assignedStaff != null
                    && Boolean.TRUE.equals(assignedStaff.getActive())
                    && assignedStaff.getEmploymentStatus() == EmploymentStatus.ACTIVE) {
                primaryStaffByAllocation.put(
                        assignment.getTeacherAllocation().getTeacherAllocationId(), assignedStaff.getStaffId());
            }
        }

        Map<Long, Long> substituteByEntry = new HashMap<>();
        for (TeacherArrangement arrangement : arrangementRepository
                .findByAcademicYear_AcademicYearIdAndAbsenceDateAndStatusOrderByTeacherArrangementIdAsc(
                        year.getAcademicYearId(), date, TeacherArrangementStatus.APPROVED)) {
            arrangement.getSlots().forEach(slot -> substituteByEntry.put(
                    slot.getTimetableEntry().getTimetableEntryId(), slot.getSubstituteTeacher().getStaffId()));
        }

        Map<Long, Long> firstEntryBySection = firstEntryBySection(entries);
        List<TeacherTeachingSlot> result = new ArrayList<>();
        for (TimetableEntry entry : entries) {
            Long entryId = entry.getTimetableEntryId();
            Long substituteStaffId = substituteByEntry.get(entryId);
            Long assignedStaffId = substituteStaffId != null
                    ? substituteStaffId
                    : primaryStaffByAllocation.get(entry.getTeacherAllocation().getTeacherAllocationId());
            if (!Objects.equals(staffId, assignedStaffId)) {
                continue;
            }

            Long sectionId = entry.getSection().getSectionId();
            boolean attendanceAllowed = mode == AttendanceMode.PERIOD_WISE
                    || Objects.equals(entryId, firstEntryBySection.get(sectionId));
            result.add(new TeacherTeachingSlot(entry, substituteStaffId != null, mode, attendanceAllowed));
        }
        result.sort(Comparator.comparing(slot -> slot.entry().getTimetablePeriod().getPeriodNumber()));
        return result;
    }

    @Transactional(readOnly = true)
    public void requireRosterAccess(Long classId, Long sectionId, LocalDate date, Long periodId) {
        if (accessGuard.hasElevatedRole() || !hasStaffRole()) {
            return;
        }
        requireAssignment(classId, sectionId, date, periodId);
    }

    @Transactional(readOnly = true)
    public void requireDailyAttendanceAccess(Long classId, Long sectionId, LocalDate date) {
        if (attendanceMode() != AttendanceMode.DAILY) {
            throw new BadRequestException("This organization uses period-wise attendance.");
        }
        if (accessGuard.hasElevatedRole()) {
            return;
        }
        requireAssignment(classId, sectionId, date, null);
    }

    @Transactional(readOnly = true)
    public void requirePeriodSectionAccess(Long classId, Long sectionId, LocalDate date) {
        if (accessGuard.hasElevatedRole() || !hasStaffRole()) {
            throw new BadRequestException("Choose a scheduled period before loading period-wise attendance.");
        }
        if (sectionId == null) {
            throw new AccessDeniedException("Select a section assigned to you before taking attendance.");
        }
        User user = accessGuard.requireCurrentUser();
        Staff staff = staffRepository.findByUser_Id(user.getId())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive())
                        && candidate.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("Your account is not linked to an active teacher profile."));
        boolean assigned = slotsForTeacher(staff.getStaffId(), date).stream()
                .anyMatch(slot -> Objects.equals(slot.entry().getSection().getSectionId(), sectionId)
                        && Objects.equals(slot.entry().getSection().getAcademicClass().getClassId(), classId));
        if (!assigned) {
            throw new AccessDeniedException("You are not assigned to teach this section today.");
        }
        throw new BadRequestException("Choose one of your scheduled periods before loading attendance.");
    }

    @Transactional(readOnly = true)
    public TimetableEntry requirePeriodAttendanceAccess(
            Long classId, Long sectionId, LocalDate date, Long periodId) {
        if (attendanceMode() != AttendanceMode.PERIOD_WISE) {
            throw new BadRequestException("This organization uses daily attendance.");
        }
        if (sectionId == null || periodId == null) {
            throw new BadRequestException("Select a section and a scheduled period before loading attendance.");
        }

        TimetableEntry scheduledEntry = scheduledEntry(classId, sectionId, date, periodId);
        if (accessGuard.hasElevatedRole()) {
            return scheduledEntry;
        }
        requireAssignment(classId, sectionId, date, periodId);
        return scheduledEntry;
    }

    public AttendanceMode attendanceMode() {
        Long orgId = com.thinkerscave.shared.context.OrganizationContext.getOrganizationId();
        return AttendanceMode.valueOf(moduleSettingsService.getString(
                orgId, ModuleSettingCatalog.ATTENDANCE, ModuleSettingCatalog.ATTENDANCE_MODE));
    }

    private void requireAssignment(Long classId, Long sectionId, LocalDate date, Long periodId) {
        if (sectionId == null) {
            throw new AccessDeniedException("Select a section assigned to you before taking attendance.");
        }
        User user = accessGuard.requireCurrentUser();
        Staff staff = staffRepository.findByUser_Id(user.getId())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive())
                        && candidate.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("Your account is not linked to an active teacher profile."));

        AttendanceMode mode = attendanceMode();
        boolean assigned = slotsForTeacher(staff.getStaffId(), date).stream()
                .filter(slot -> Objects.equals(slot.entry().getSection().getSectionId(), sectionId))
                .filter(slot -> Objects.equals(slot.entry().getSection().getAcademicClass().getClassId(), classId))
                .anyMatch(slot -> mode == AttendanceMode.DAILY
                        ? slot.attendanceAllowed()
                        : Objects.equals(slot.entry().getTimetablePeriod().getTimetablePeriodId(), periodId));

        if (!assigned) {
            String message = mode == AttendanceMode.DAILY
                    ? "You are not authorized to take daily attendance for this section. The teacher assigned to its first class must mark it."
                    : "You are not assigned to this section and period on the selected date.";
            throw new AccessDeniedException(message);
        }
    }

    private TimetableEntry scheduledEntry(Long classId, Long sectionId, LocalDate date, Long periodId) {
        var year = academicYearRepository.findByCurrentYearTrue()
                .orElseThrow(() -> new BadRequestException("There is no current academic year."));
        TimetableVersion version = timetableVersionRepository
                .findByAcademicYear_AcademicYearIdAndStatus(year.getAcademicYearId(), TimetableStatus.PUBLISHED)
                .orElseThrow(() -> new BadRequestException("There is no published timetable for the current academic year."));
        DayOfWeek day = DayOfWeek.valueOf(date.getDayOfWeek().name());
        return timetableEntryRepository.findByTimetableVersion_TimetableVersionIdAndDayOfWeek(
                        version.getTimetableVersionId(), day).stream()
                .filter(this::isTeachingEntry)
                .filter(entry -> Objects.equals(entry.getSection().getSectionId(), sectionId))
                .filter(entry -> Objects.equals(entry.getSection().getAcademicClass().getClassId(), classId))
                .filter(entry -> Objects.equals(entry.getTimetablePeriod().getTimetablePeriodId(), periodId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("The selected period is not scheduled for this section on that date."));
    }

    private Map<Long, Long> firstEntryBySection(List<TimetableEntry> entries) {
        Map<Long, Long> first = new HashMap<>();
        Map<Long, Integer> firstPeriodBySection = new HashMap<>();
        for (TimetableEntry entry : entries) {
            Long sectionId = entry.getSection().getSectionId();
            Integer periodNumber = entry.getTimetablePeriod().getPeriodNumber().intValue();
            Integer currentPeriod = firstPeriodBySection.get(sectionId);
            if (currentPeriod == null || periodNumber < currentPeriod) {
                first.put(sectionId, entry.getTimetableEntryId());
                firstPeriodBySection.put(sectionId, periodNumber);
            }
        }
        return first;
    }

    private boolean isTeachingEntry(TimetableEntry entry) {
        return entry.getEntryType() == TimetableEntryType.SUBJECT
                && entry.getTeacherAllocation() != null
                && entry.getSection() != null
                && entry.getSection().getAcademicClass() != null
                && entry.getTimetablePeriod() != null
                && entry.getTimetablePeriod().getPeriodNumber() != null;
    }

    private boolean hasStaffRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(role -> "STAFF".equals(role) || "TEACHER".equals(role));
    }

    public record TeacherTeachingSlot(
            TimetableEntry entry,
            boolean substitute,
            AttendanceMode attendanceMode,
            boolean attendanceAllowed) {}
}