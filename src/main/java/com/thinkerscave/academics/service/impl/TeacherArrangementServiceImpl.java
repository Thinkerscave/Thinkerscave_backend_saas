package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.TeacherArrangementRequest;
import com.thinkerscave.academics.dto.response.TeacherArrangementEligibleSlotResponse;
import com.thinkerscave.academics.dto.response.TeacherArrangementResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.enums.TeacherArrangementStatus;
import com.thinkerscave.academics.enums.TimetableEntryType;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TeacherArrangementService;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.exceptions.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherArrangementServiceImpl implements TeacherArrangementService {

    private static final String RESOURCE = AcademicsAccessGuard.RESOURCE_TIMETABLE;
    private static final List<TeacherArrangementStatus> RESERVED_STATUSES =
            List.of(TeacherArrangementStatus.PENDING, TeacherArrangementStatus.APPROVED);

    private final AcademicYearRepository yearRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TeacherArrangementRepository arrangementRepository;
    private final TeacherArrangementSlotRepository arrangementSlotRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherArrangementResponse> list(Long academicYearId) {
        accessGuard.requireView(RESOURCE);
        return arrangementRepository.findByAcademicYear_AcademicYearIdOrderByAbsenceDateDesc(academicYearId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherArrangementResponse> listByDate(Long academicYearId, LocalDate absenceDate) {
        accessGuard.requireView(RESOURCE);
        return arrangementRepository.findByAcademicYear_AcademicYearIdAndAbsenceDateAndStatusOrderByTeacherArrangementIdAsc(
                        academicYearId, absenceDate, TeacherArrangementStatus.APPROVED)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherArrangementEligibleSlotResponse.StaffOption> teachers(Long academicYearId) {
        accessGuard.requireView(RESOURCE);
        requireYear(academicYearId);
        return activePrimaryTeachers(academicYearId).values().stream()
                .distinct()
                .map(staff -> new TeacherArrangementEligibleSlotResponse.StaffOption(
                        staff.getStaffId(), TimetablePlanningData.staffName(staff)))
                .sorted(Comparator.comparing(TeacherArrangementEligibleSlotResponse.StaffOption::name,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherArrangementEligibleSlotResponse> eligibleSlots(
            Long academicYearId, LocalDate absenceDate, Long absentTeacherId) {
        accessGuard.requireView(RESOURCE);
        return eligibleSlots(academicYearId, absenceDate, absentTeacherId, null, true);
    }

    @Override
    @Transactional
    public TeacherArrangementResponse create(TeacherArrangementRequest request) {
        accessGuard.requireManage(RESOURCE);
        validateRequest(request);
        ensureNoActiveArrangement(request.academicYearId(), request.absentTeacherId(), request.absenceDate());

        List<TeacherArrangementEligibleSlotResponse> eligible = eligibleSlots(
                request.academicYearId(), request.absenceDate(), request.absentTeacherId(), null, false);
        Map<Long, TeacherArrangementEligibleSlotResponse> eligibleByEntry = eligible.stream()
                .collect(Collectors.toMap(TeacherArrangementEligibleSlotResponse::timetableEntryId, Function.identity()));
        Set<Long> requestedEntryIds = request.slots().stream()
                .map(TeacherArrangementRequest.Slot::timetableEntryId)
                .collect(Collectors.toSet());
        if (requestedEntryIds.size() != request.slots().size()
                || !requestedEntryIds.equals(eligibleByEntry.keySet())) {
            throw new IllegalArgumentException("Every scheduled period for the absent teacher must have one substitute.");
        }

        TeacherArrangement arrangement = new TeacherArrangement();
        arrangement.setAcademicYear(requireYear(request.academicYearId()));
        arrangement.setTimetableVersion(requirePublishedVersion(request.academicYearId()));
        arrangement.setAbsentTeacher(requireStaff(request.absentTeacherId()));
        arrangement.setAbsenceDate(request.absenceDate());
        arrangement.setReason(request.reason().trim());
        arrangement.setStatus(TeacherArrangementStatus.PENDING);

        Map<Long, TimetableEntry> entriesById = allDayEntries(arrangement.getTimetableVersion(), request.absenceDate())
                .stream().collect(Collectors.toMap(TimetableEntry::getTimetableEntryId, Function.identity()));
        for (TeacherArrangementRequest.Slot requested : request.slots()) {
            TeacherArrangementEligibleSlotResponse slot = eligibleByEntry.get(requested.timetableEntryId());
            boolean candidateAvailable = slot.candidates().stream()
                    .anyMatch(candidate -> Objects.equals(candidate.staffId(), requested.substituteStaffId()));
            if (!candidateAvailable) {
                throw new IllegalArgumentException("A selected substitute is no longer available for that period.");
            }
            TeacherArrangementSlot detail = new TeacherArrangementSlot();
            detail.setArrangement(arrangement);
            detail.setTimetableEntry(entriesById.get(requested.timetableEntryId()));
            detail.setSubstituteTeacher(requireStaff(requested.substituteStaffId()));
            arrangement.getSlots().add(detail);
        }
        return toResponse(arrangementRepository.save(arrangement));
    }

    @Override
    @Transactional
    public TeacherArrangementResponse approve(Long arrangementId) {
        accessGuard.requireApprove(RESOURCE);
        TeacherArrangement arrangement = requireArrangement(arrangementId);
        if (arrangement.getStatus() != TeacherArrangementStatus.PENDING) {
            throw new ConflictException("Only pending arrangements can be approved.");
        }
        ensureNoActiveArrangement(arrangement.getAcademicYear().getAcademicYearId(),
                arrangement.getAbsentTeacher().getStaffId(), arrangement.getAbsenceDate(), arrangementId);

        Map<Long, TeacherArrangementEligibleSlotResponse> eligibleByEntry = eligibleSlots(
                        arrangement.getAcademicYear().getAcademicYearId(), arrangement.getAbsenceDate(),
                        arrangement.getAbsentTeacher().getStaffId(), arrangementId, false)
                .stream().collect(Collectors.toMap(TeacherArrangementEligibleSlotResponse::timetableEntryId, Function.identity()));
        for (TeacherArrangementSlot slot : arrangement.getSlots()) {
            TeacherArrangementEligibleSlotResponse eligible = eligibleByEntry.get(
                    slot.getTimetableEntry().getTimetableEntryId());
            if (eligible == null || eligible.candidates().stream().noneMatch(candidate ->
                    Objects.equals(candidate.staffId(), slot.getSubstituteTeacher().getStaffId()))) {
                throw new ConflictException("A substitute is no longer available; reject and recreate this arrangement.");
            }
        }

        arrangement.setStatus(TeacherArrangementStatus.APPROVED);
        arrangement.setReviewedAt(LocalDateTime.now());
        arrangement.setReviewedByUserId(accessGuard.currentUserIdOrNull());
        arrangement.setReviewNote(null);
        return toResponse(arrangementRepository.save(arrangement));
    }

    @Override
    @Transactional
    public TeacherArrangementResponse reject(Long arrangementId, String note) {
        accessGuard.requireApprove(RESOURCE);
        TeacherArrangement arrangement = requireArrangement(arrangementId);
        if (arrangement.getStatus() != TeacherArrangementStatus.PENDING) {
            throw new ConflictException("Only pending arrangements can be rejected.");
        }
        arrangement.setStatus(TeacherArrangementStatus.REJECTED);
        arrangement.setReviewedAt(LocalDateTime.now());
        arrangement.setReviewedByUserId(accessGuard.currentUserIdOrNull());
        arrangement.setReviewNote(note == null ? null : note.trim());
        return toResponse(arrangementRepository.save(arrangement));
    }

    private List<TeacherArrangementEligibleSlotResponse> eligibleSlots(
            Long academicYearId, LocalDate absenceDate, Long absentTeacherId, Long excludeArrangementId,
            boolean rejectDuplicateAbsence) {
        AcademicYear year = requireYear(academicYearId);
        validateDate(year, absenceDate);
        TimetableVersion version = requirePublishedVersion(academicYearId);
        Map<Long, Staff> primaryTeachers = activePrimaryTeachers(academicYearId);
        if (primaryTeachers.values().stream().noneMatch(staff -> Objects.equals(staff.getStaffId(), absentTeacherId))) {
            throw new IllegalArgumentException("The selected teacher is not actively assigned in this academic year.");
        }
        if (rejectDuplicateAbsence) {
            ensureNoActiveArrangement(academicYearId, absentTeacherId, absenceDate);
        }

        List<TimetableEntry> dayEntries = allDayEntries(version, absenceDate);
        List<TimetableEntry> absentEntries = dayEntries.stream()
                .filter(entry -> entry.getEntryType() == TimetableEntryType.SUBJECT)
                .filter(entry -> entry.getTeacherAllocation() != null)
                .filter(entry -> {
                    Staff assigned = primaryTeachers.get(entry.getTeacherAllocation().getTeacherAllocationId());
                    return assigned != null && Objects.equals(assigned.getStaffId(), absentTeacherId);
                })
                .toList();
        if (absentEntries.isEmpty()) {
            throw new IllegalArgumentException("The selected teacher has no scheduled teaching periods on this date.");
        }

        Map<Long, Staff> staffById = primaryTeachers.values().stream().distinct()
                .collect(Collectors.toMap(Staff::getStaffId, Function.identity(), (first, ignored) -> first));
        List<TeacherArrangementEligibleSlotResponse> result = new ArrayList<>();
        for (TimetableEntry absentEntry : absentEntries) {
            Long periodId = absentEntry.getTimetablePeriod().getTimetablePeriodId();
            Set<Long> busyStaff = dayEntries.stream()
                    .filter(entry -> Objects.equals(entry.getTimetablePeriod().getTimetablePeriodId(), periodId))
                    .filter(entry -> entry.getTeacherAllocation() != null)
                    .map(entry -> primaryTeachers.get(entry.getTeacherAllocation().getTeacherAllocationId()))
                    .filter(Objects::nonNull)
                    .map(Staff::getStaffId)
                    .collect(Collectors.toSet());
            busyStaff.addAll(arrangementSlotRepository.findReservedSubstituteIds(
                    absenceDate, periodId, RESERVED_STATUSES, excludeArrangementId));

            List<TeacherArrangementEligibleSlotResponse.StaffOption> candidates = staffById.values().stream()
                    .filter(staff -> !Objects.equals(staff.getStaffId(), absentTeacherId))
                    .filter(staff -> !busyStaff.contains(staff.getStaffId()))
                    .map(staff -> new TeacherArrangementEligibleSlotResponse.StaffOption(
                            staff.getStaffId(), TimetablePlanningData.staffName(staff)))
                    .sorted(Comparator.comparing(TeacherArrangementEligibleSlotResponse.StaffOption::name,
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
            AcademicSection section = absentEntry.getSection();
            result.add(new TeacherArrangementEligibleSlotResponse(
                    absentEntry.getTimetableEntryId(),
                    section == null || section.getAcademicGroup() == null ? "" : section.getAcademicGroup().getName(),
                    section == null ? "" : section.getName(),
                    absentEntry.getSubjectNameSnapshot(),
                    absentEntry.getTimetablePeriod().getPeriodNumber(),
                    absentEntry.getTimetablePeriod().getStartTime(),
                    absentEntry.getTimetablePeriod().getEndTime(),
                    candidates));
        }
        return result;
    }

    private Map<Long, Staff> activePrimaryTeachers(Long academicYearId) {
        Map<Long, TeacherAllocationTeacher> latest = new HashMap<>();
        for (TeacherAllocationTeacher assignment : allocationTeacherRepository.findActiveByYear(academicYearId)) {
            Staff staff = assignment.getStaff();
            if (assignment.getRole() != TeacherAllocationTeacherRole.PRIMARY || staff == null
                    || !Boolean.TRUE.equals(staff.getActive())
                    || staff.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                continue;
            }
            latest.merge(assignment.getTeacherAllocation().getTeacherAllocationId(), assignment,
                    (first, second) -> first.getEffectiveFrom().isAfter(second.getEffectiveFrom()) ? first : second);
        }
        Map<Long, Staff> result = new HashMap<>();
        latest.forEach((allocationId, assignment) -> result.put(allocationId, assignment.getStaff()));
        return result;
    }

    private List<TimetableEntry> allDayEntries(TimetableVersion version, LocalDate date) {
        DayOfWeek day = DayOfWeek.valueOf(date.getDayOfWeek().name());
        return entryRepository.findGridEntries(version.getTimetableVersionId()).stream()
                .filter(entry -> entry.getDayOfWeek() == day)
                .toList();
    }

    private void validateDate(AcademicYear year, LocalDate date) {
        if (date == null || date.isBefore(year.getStartDate()) || date.isAfter(year.getEndDate())) {
            throw new IllegalArgumentException("The absence date must fall within the selected academic year.");
        }
    }

    private void validateRequest(TeacherArrangementRequest request) {
        if (request == null || request.academicYearId() == null || request.absenceDate() == null
                || request.absentTeacherId() == null || request.slots() == null || request.slots().isEmpty()
                || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Year, date, teacher, reason, and period substitutes are required.");
        }
    }

    private void ensureNoActiveArrangement(Long yearId, Long teacherId, LocalDate date) {
        ensureNoActiveArrangement(yearId, teacherId, date, null);
    }

    private void ensureNoActiveArrangement(Long yearId, Long teacherId, LocalDate date, Long excludeId) {
        boolean duplicate = arrangementRepository
                .existsByAcademicYear_AcademicYearIdAndAbsentTeacher_StaffIdAndAbsenceDateAndStatusIn(
                        yearId, teacherId, date, RESERVED_STATUSES);
        if (duplicate && excludeId == null) {
            throw new IllegalArgumentException("An active absence arrangement already exists for this teacher and date.");
        }
        if (duplicate && excludeId != null) {
            boolean other = arrangementRepository.findByAcademicYear_AcademicYearIdOrderByAbsenceDateDesc(yearId)
                    .stream().anyMatch(existing -> !Objects.equals(existing.getTeacherArrangementId(), excludeId)
                            && Objects.equals(existing.getAbsentTeacher().getStaffId(), teacherId)
                            && Objects.equals(existing.getAbsenceDate(), date)
                            && RESERVED_STATUSES.contains(existing.getStatus()));
            if (other) {
                throw new ConflictException("Another active absence arrangement already exists for this teacher and date.");
            }
        }
    }

    private AcademicYear requireYear(Long yearId) {
        AcademicYear year = yearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
        if (!year.isActive()) {
            throw new IllegalArgumentException("Inactive academic years cannot have teacher arrangements.");
        }
        return year;
    }

    private TimetableVersion requirePublishedVersion(Long yearId) {
        return versionRepository.findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED)
            .orElseThrow(() -> new ConflictException("A published timetable is required for teacher arrangements."));
    }

    private Staff requireStaff(Long staffId) {
        return activePrimaryTeachersForStaff(staffId);
    }

    private Staff activePrimaryTeachersForStaff(Long staffId) {
        return allocationTeacherRepository.findByStaff_StaffIdAndEffectiveToIsNull(staffId).stream()
                .map(TeacherAllocationTeacher::getStaff)
                .filter(Objects::nonNull)
                .filter(staff -> Objects.equals(staff.getStaffId(), staffId))
                .filter(staff -> Boolean.TRUE.equals(staff.getActive())
                        && staff.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Active teacher not found: " + staffId));
    }

    private TeacherArrangement requireArrangement(Long arrangementId) {
        return arrangementRepository.findById(arrangementId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher arrangement not found: " + arrangementId));
    }

    private TeacherArrangementResponse toResponse(TeacherArrangement arrangement) {
        List<TeacherArrangementResponse.Slot> slots = arrangement.getSlots().stream()
                .sorted(Comparator.comparing(slot -> slot.getTimetableEntry().getTimetablePeriod().getPeriodNumber()))
                .map(slot -> {
                    TimetableEntry entry = slot.getTimetableEntry();
                    AcademicSection section = entry.getSection();
                    return new TeacherArrangementResponse.Slot(
                            entry.getTimetableEntryId(),
                            section == null || section.getAcademicGroup() == null ? "" : section.getAcademicGroup().getName(),
                            section == null ? "" : section.getName(),
                            entry.getSubjectNameSnapshot(),
                            entry.getTimetablePeriod().getPeriodNumber(),
                            entry.getTimetablePeriod().getStartTime(),
                            entry.getTimetablePeriod().getEndTime(),
                            slot.getSubstituteTeacher().getStaffId(),
                            TimetablePlanningData.staffName(slot.getSubstituteTeacher()));
                }).toList();
        return new TeacherArrangementResponse(
                arrangement.getTeacherArrangementId(),
                arrangement.getAcademicYear().getAcademicYearId(),
                arrangement.getAcademicYear().getName(),
                arrangement.getAbsenceDate(),
                arrangement.getAbsentTeacher().getStaffId(),
                TimetablePlanningData.staffName(arrangement.getAbsentTeacher()),
                arrangement.getReason(),
                arrangement.getStatus().name(),
                arrangement.getReviewedByUserId(),
                arrangement.getReviewedAt(),
                arrangement.getReviewNote(),
                slots);
    }
}