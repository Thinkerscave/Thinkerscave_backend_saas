package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.FixedActivityRequest;
import com.thinkerscave.academics.dto.request.FixedActivityResolveRequest;
import com.thinkerscave.academics.dto.request.GroupScheduleAssignmentRequest;
import com.thinkerscave.academics.dto.request.SchedulePatternRequest;
import com.thinkerscave.academics.dto.response.FixedActivityOptionsResponse;
import com.thinkerscave.academics.dto.response.FixedActivityResponse;
import com.thinkerscave.academics.dto.response.FixedActivityScheduleResponse;
import com.thinkerscave.academics.dto.response.GroupAssignmentMatrixResponse;
import com.thinkerscave.academics.dto.response.GroupScheduleAssignmentResponse;
import com.thinkerscave.academics.dto.response.SchedulePatternResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TimetableConfigurationService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableConfigurationServiceImpl implements TimetableConfigurationService {

    private static final EnumSet<TimetableStatus> IN_WORKFLOW = EnumSet.of(
            TimetableStatus.PENDING_APPROVAL, TimetableStatus.APPROVED,
            TimetableStatus.PUBLISHED, TimetableStatus.SUPERSEDED);

    /** A timetable that still drives the live school schedule. */
    private static final EnumSet<TimetableStatus> ACTIVE_TIMETABLE_STATUSES = EnumSet.of(
            TimetableStatus.PENDING_APPROVAL, TimetableStatus.APPROVED, TimetableStatus.PUBLISHED);

    private static final EnumSet<EmploymentStatus> INACTIVE_EMPLOYMENT = EnumSet.of(
            EmploymentStatus.RESIGNED,
            EmploymentStatus.RETIRED,
            EmploymentStatus.CONTRACT_COMPLETED);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AcademicYearRepository academicYearRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SectionRepository sectionRepository;
    private final SchedulePatternRepository patternRepository;
    private final TimetablePeriodRepository periodRepository;
    private final GroupScheduleAssignmentRepository assignmentRepository;
    private final FixedActivityRepository fixedActivityRepository;
    private final FixedActivityPeriodRepository fixedActivityPeriodRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicGroupSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository teacherAllocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TeacherGroupAllocationRepository groupAllocationRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableWorkingDayRepository workingDayRepository;
    private final StaffRepository staffRepository;
    private final AcademicsAccessGuard accessGuard;

    // ─── Patterns ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SchedulePatternResponse> listPatterns(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        Map<Long, List<GroupScheduleAssignment>> byPattern = assignmentRepository
                .findDetailedByYear(yearId).stream()
                .collect(Collectors.groupingBy(a -> a.getSchedulePattern().getSchedulePatternId()));
        return patternRepository.findByAcademicYear_AcademicYearIdOrderByNameAsc(yearId).stream()
                .map(p -> toPatternResponse(p, byPattern.getOrDefault(p.getSchedulePatternId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulePatternResponse getPattern(Long patternId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        SchedulePattern pattern = requirePattern(patternId);
        List<GroupScheduleAssignment> assignments = assignmentRepository
                .findBySchedulePattern_SchedulePatternId(patternId);
        return toPatternResponse(pattern, assignments);
    }

    @Override
    public SchedulePatternResponse createPattern(Long yearId, SchedulePatternRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        assertYearMutable(year);
        validatePatternRequest(request);
        if (patternRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(yearId, request.getName().trim())) {
            throw new BusinessException("A schedule pattern named '" + request.getName().trim() + "' already exists");
        }
        SchedulePattern pattern = new SchedulePattern();
        pattern.setAcademicYear(year);
        pattern.setName(request.getName().trim());
        pattern.setDescription(trimToNull(request.getDescription()));
        pattern.setActive(request.getActive() == null || request.getActive());
        pattern = patternRepository.save(pattern);
        writeSlots(pattern, request.getSlots());
        syncWorkingDays(year);
        return toPatternResponse(pattern, List.of());
    }

    @Override
    public SchedulePatternResponse updatePattern(Long patternId, SchedulePatternRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        SchedulePattern pattern = requirePattern(patternId);
        assertYearMutable(pattern.getAcademicYear());
        validatePatternRequest(request);
        Long yearId = pattern.getAcademicYear().getAcademicYearId();
        if (patternRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndSchedulePatternIdNot(
                yearId, request.getName().trim(), patternId)) {
            throw new BusinessException("A schedule pattern named '" + request.getName().trim() + "' already exists");
        }
        boolean structuralChange = patternStructureChanged(pattern,
                request.getSlots().stream().map(SchedulePatternRequest.Slot::getStartTime).toList(),
                request.getSlots().stream().map(SchedulePatternRequest.Slot::getEndTime).toList());
        if (structuralChange && hasInWorkflowVersion(yearId)) {
            throw new BusinessException("This pattern's slots are used by a submitted, approved or published "
                    + "timetable. Create a new pattern for the new timings instead of editing it.");
        }
        if ((structuralChange || periodLabelsChanged(pattern, request.getSlots()))
                && fixedActivityPeriodRepository.countBySchedulePatternId(patternId) > 0) {
            throw new BusinessException("This pattern's periods are used by a fixed activity. Create a new "
                    + "pattern for the new timings instead of editing it.");
        }
        pattern.setName(request.getName().trim());
        pattern.setDescription(trimToNull(request.getDescription()));
        pattern.setActive(request.getActive() == null || request.getActive());
        patternRepository.save(pattern);
        if (structuralChange) {
            periodRepository.deleteBySchedulePattern_SchedulePatternId(patternId);
            periodRepository.flush();
        }
        writeSlots(pattern, request.getSlots());
        syncWorkingDays(pattern.getAcademicYear());
        return toPatternResponse(pattern,
                assignmentRepository.findBySchedulePattern_SchedulePatternId(patternId));
    }

    @Override
    public SchedulePatternResponse duplicatePattern(Long patternId, String name) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        SchedulePattern source = requirePattern(patternId);
        assertYearMutable(source.getAcademicYear());
        String targetName = StringUtils.hasText(name) ? name.trim() : source.getName() + " (copy)";
        int suffix = 2;
        while (patternRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(
                source.getAcademicYear().getAcademicYearId(), targetName)) {
            targetName = (StringUtils.hasText(name) ? name.trim() : source.getName()) + " (" + suffix++ + ")";
        }
        SchedulePattern copy = new SchedulePattern();
        copy.setAcademicYear(source.getAcademicYear());
        copy.setName(targetName);
        copy.setDescription(source.getDescription());
        copy.setActive(true);
        copy = patternRepository.save(copy);
        List<TimetablePeriod> slots = periodRepository
                .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(patternId);
        short order = 1;
        for (TimetablePeriod slot : slots) {
            TimetablePeriod copySlot = new TimetablePeriod();
            copySlot.setSchedulePattern(copy);
            copySlot.setPeriodNumber(order++);
            copySlot.setName(slot.getName());
            copySlot.setStartTime(slot.getStartTime());
            copySlot.setEndTime(slot.getEndTime());
            copySlot.setSlotKind(slot.getSlotKind());
            periodRepository.save(copySlot);
        }
        return toPatternResponse(copy, List.of());
    }

    @Override
    public SchedulePatternResponse setPatternActive(Long patternId, boolean active) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        SchedulePattern pattern = requirePattern(patternId);
        assertYearMutable(pattern.getAcademicYear());
        if (!active) {
            long usage = versionRepository
                    .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(
                            pattern.getAcademicYear().getAcademicYearId()).stream()
                    .filter(v -> IN_WORKFLOW.contains(v.getStatus())).count();
            if (usage > 0) {
                throw new BusinessException("This pattern cannot be deactivated while a submitted, approved or "
                        + "published timetable exists for the academic year.");
            }
        }
        pattern.setActive(active);
        return toPatternResponse(patternRepository.save(pattern),
                assignmentRepository.findBySchedulePattern_SchedulePatternId(patternId));
    }

    // ─── Group assignments ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<GroupScheduleAssignmentResponse> listAssignments(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        return assignmentRepository.findDetailedByYear(yearId).stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GroupAssignmentMatrixResponse getAssignmentMatrix(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        List<AcademicGroup> groups = academicGroupRepository
                .findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(yearId);
        List<GroupScheduleAssignment> assignments = assignmentRepository.findDetailedByYear(yearId);
        Map<String, List<GroupScheduleAssignment>> byUnit = new HashMap<>();
        for (GroupScheduleAssignment a : assignments) {
            byUnit.computeIfAbsent(unitKey(a.getAcademicGroup().getAcademicGroupId(),
                            a.getSection() == null ? null : a.getSection().getSectionId()),
                    k -> new ArrayList<>()).add(a);
        }
        List<GroupAssignmentMatrixResponse.Row> rows = new ArrayList<>();
        for (AcademicGroup group : groups) {
            List<AcademicSection> sections = sectionRepository
                    .findByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(group.getAcademicGroupId());
            List<AcademicSection> activeSections = sections.stream().filter(AcademicSection::isActive).toList();
            if (activeSections.isEmpty()) {
                rows.add(matrixRow(group, null, byUnit, null));
            } else {
                if (!byUnit.getOrDefault(unitKey(group.getAcademicGroupId(), null), List.of()).isEmpty()) {
                    rows.add(matrixRow(group, null, byUnit, "All Sections"));
                }
                activeSections.forEach(section -> rows.add(matrixRow(group, section, byUnit, null)));
            }
        }
        return GroupAssignmentMatrixResponse.builder()
                .academicYearId(yearId)
                .days(Arrays.asList(DayOfWeek.values()))
                .rows(rows)
                .build();
    }

    private GroupAssignmentMatrixResponse.Row matrixRow(AcademicGroup group, AcademicSection section,
                                                         Map<String, List<GroupScheduleAssignment>> byUnit,
                                                         String scopeLabel) {
        List<GroupAssignmentMatrixResponse.Cell> cells = new ArrayList<>();
        for (GroupScheduleAssignment a : byUnit.getOrDefault(
                unitKey(group.getAcademicGroupId(), section == null ? null : section.getSectionId()), List.of())) {
            List<TimetablePeriod> patternSlots = periodRepository
                    .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(
                            a.getSchedulePattern().getSchedulePatternId());
            LocalTime start = patternSlots.stream().map(TimetablePeriod::getStartTime)
                    .min(LocalTime::compareTo).orElse(null);
            LocalTime end = patternSlots.stream().map(TimetablePeriod::getEndTime)
                    .max(LocalTime::compareTo).orElse(null);
            cells.add(GroupAssignmentMatrixResponse.Cell.builder()
                    .dayOfWeek(a.getDayOfWeek())
                    .groupScheduleAssignmentId(a.getGroupScheduleAssignmentId())
                    .schedulePatternId(a.getSchedulePattern().getSchedulePatternId())
                    .schedulePatternName(a.getSchedulePattern().getName())
                    .startTime(start)
                    .endTime(end)
                    .active(a.isActive())
                    .effectiveFrom(a.getEffectiveFrom() == null ? null : a.getEffectiveFrom().toString())
                    .effectiveTo(a.getEffectiveTo() == null ? null : a.getEffectiveTo().toString())
                    .build());
        }
        return GroupAssignmentMatrixResponse.Row.builder()
                .academicGroupId(group.getAcademicGroupId())
                .academicGroupName(group.getName())
                .academicGroupCode(group.getCode())
                .sectionId(section == null ? null : section.getSectionId())
                .sectionName(section == null ? null : section.getName())
                .scopeLabel(scopeLabel)
                .active(group.isActive())
                .cells(cells)
                .build();
    }

    @Override
    public List<GroupScheduleAssignmentResponse> assignPattern(Long yearId, GroupScheduleAssignmentRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        assertYearMutable(year);
        SchedulePattern pattern = requirePattern(request.getSchedulePatternId());
        if (!Objects.equals(pattern.getAcademicYear().getAcademicYearId(), yearId)) {
            throw new BusinessException("The selected pattern belongs to a different academic year");
        }
        if (!pattern.isActive()) {
            throw new BusinessException("Activate the selected schedule pattern before assigning it");
        }
        List<DayOfWeek> days = request.getDays() == null || request.getDays().isEmpty()
                ? List.of() : request.getDays();
        if (days.isEmpty()) throw new BusinessException("Select at least one applicable day");
        if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new BusinessException("The effective end date must be on or after the start date");
        }
        List<GroupScheduleAssignmentRequest.Unit> units = resolveUnits(yearId, request);
        if (units.isEmpty()) throw new BusinessException("Select at least one academic group or section");

        List<GroupScheduleAssignment> saved = new ArrayList<>();
        List<TimetablePeriod> patternSlots = periodRepository
                .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(pattern.getSchedulePatternId());
        LocalTime newStart = patternSlots.stream().map(TimetablePeriod::getStartTime)
                .min(LocalTime::compareTo).orElse(null);
        LocalTime newEnd = patternSlots.stream().map(TimetablePeriod::getEndTime)
                .max(LocalTime::compareTo).orElse(null);

        for (GroupScheduleAssignmentRequest.Unit unit : units) {
            AcademicGroup group = requireGroupInYear(yearId, unit.getAcademicGroupId());
            AcademicSection section = unit.getSectionId() == null ? null
                    : requireSectionInYear(yearId, unit.getSectionId(), group.getAcademicGroupId());
            for (DayOfWeek day : days) {
                List<GroupScheduleAssignment> existing = assignmentRepository.findDetailedByYear(yearId).stream()
                        .filter(a -> a.isActive()
                                && Objects.equals(a.getAcademicGroup().getAcademicGroupId(), group.getAcademicGroupId())
                                && Objects.equals(a.getSection() == null ? null : a.getSection().getSectionId(),
                                        section == null ? null : section.getSectionId())
                                && a.getDayOfWeek() == day)
                        .toList();
                for (GroupScheduleAssignment old : existing) {
                    if (!rangesOverlap(old.getEffectiveFrom(), old.getEffectiveTo(),
                            request.getEffectiveFrom(), request.getEffectiveTo())) continue;
                    List<TimetablePeriod> oldSlots = periodRepository
                            .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(
                                    old.getSchedulePattern().getSchedulePatternId());
                    LocalTime oldStart = oldSlots.stream().map(TimetablePeriod::getStartTime)
                            .min(LocalTime::compareTo).orElse(null);
                    LocalTime oldEnd = oldSlots.stream().map(TimetablePeriod::getEndTime)
                            .max(LocalTime::compareTo).orElse(null);
                    boolean samePattern = Objects.equals(old.getSchedulePattern().getSchedulePatternId(),
                            pattern.getSchedulePatternId());
                    boolean overlappingTimes = oldStart != null && newStart != null
                            && newStart.isBefore(oldEnd) && oldStart.isBefore(newEnd);
                    if (samePattern || overlappingTimes) {
                        assignmentRepository.delete(old);
                    }
                }
                GroupScheduleAssignment assignment = new GroupScheduleAssignment();
                assignment.setAcademicYear(year);
                assignment.setAcademicGroup(group);
                assignment.setSection(section);
                assignment.setDayOfWeek(day);
                assignment.setSchedulePattern(pattern);
                assignment.setEffectiveFrom(request.getEffectiveFrom());
                assignment.setEffectiveTo(request.getEffectiveTo());
                assignment.setActive(true);
                saved.add(assignmentRepository.save(assignment));
            }
        }
        assignmentRepository.flush();
        syncWorkingDays(year);
        return saved.stream().map(this::toAssignmentResponse).toList();
    }

    @Override
    public GroupScheduleAssignmentResponse updateAssignment(Long assignmentId,
                                                             GroupScheduleAssignmentRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        GroupScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule assignment not found: " + assignmentId));
        assertYearMutable(assignment.getAcademicYear());
        if (request.getSchedulePatternId() != null) {
            SchedulePattern pattern = requirePattern(request.getSchedulePatternId());
            if (!pattern.isActive()) throw new BusinessException("The selected pattern is inactive");
            assignment.setSchedulePattern(pattern);
        }
        if (request.getEffectiveFrom() != null) assignment.setEffectiveFrom(request.getEffectiveFrom());
        if (request.getEffectiveTo() != null) assignment.setEffectiveTo(request.getEffectiveTo());
        if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new BusinessException("The effective end date must be on or after the start date");
        }
        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Override
    public void deleteAssignment(Long assignmentId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        GroupScheduleAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule assignment not found: " + assignmentId));
        assertYearMutable(assignment.getAcademicYear());
        assignmentRepository.delete(assignment);
        assignmentRepository.flush();
        syncWorkingDays(assignment.getAcademicYear());
    }

    // ─── Fixed activities ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<FixedActivityResponse> listFixedActivities(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        return fixedActivityRepository.findDetailedByYear(yearId).stream()
                .map(this::toFixedActivityResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FixedActivityOptionsResponse getFixedActivityOptions(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        List<FixedActivityOptionsResponse.SubjectOption> subjects = subjectRepository
                .findByActiveTrueOrderByNameAsc().stream()
                .filter(subject -> isFixedActivitySubject(subject.getSubjectType()))
                .sorted(Comparator
                        .comparingInt((Subject subject) -> subject.getSubjectType() == SubjectType.ACTIVITY
                                ? 1 : 0)
                        .thenComparing(Subject::getName, String.CASE_INSENSITIVE_ORDER))
                .map(subject -> FixedActivityOptionsResponse.SubjectOption.builder()
                        .subjectId(subject.getSubjectId())
                        .name(subject.getName())
                        .code(subject.getCode())
                        .subjectType(subject.getSubjectType().name())
                        .build())
                .toList();
        List<FixedActivityOptionsResponse.GroupOption> groups = academicGroupRepository
                .findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(yearId).stream()
                .map(group -> FixedActivityOptionsResponse.GroupOption.builder()
                        .academicGroupId(group.getAcademicGroupId())
                        .name(group.getName())
                        .code(group.getCode())
                        .sections(sectionRepository
                                .findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(
                                        group.getAcademicGroupId()).stream()
                                .map(section -> FixedActivityOptionsResponse.SectionOption.builder()
                                        .sectionId(section.getSectionId())
                                        .name(section.getName())
                                        .code(section.getCode())
                                        .build())
                                .toList())
                        .build())
                .toList();
        return FixedActivityOptionsResponse.builder()
                .academicYearId(yearId)
                .subjects(subjects)
                .groups(groups)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FixedActivityScheduleResponse resolveFixedActivitySchedule(Long yearId,
                                                                      FixedActivityResolveRequest request) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        List<UnitRef> units = expandUnits(yearId, resolveSelections(request.getGroups()));
        Set<DayOfWeek> requestedDays = request.getDays() == null
                ? EnumSet.noneOf(DayOfWeek.class)
                : request.getDays().stream().filter(Objects::nonNull)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));

        List<FixedActivityScheduleResponse.Combination> combinations = new ArrayList<>();
        List<FixedActivityScheduleResponse.ValidationIssue> issues = new ArrayList<>();
        List<FixedActivityScheduleResponse.PeriodOption> commonPeriods = List.of();
        List<FixedActivityScheduleResponse.TeacherOption> teachers = List.of();
        Subject subject = request.getSubjectId() == null ? null
                : subjectRepository.findById(request.getSubjectId()).orElse(null);

        if (request.getSubjectId() != null
                && (subject == null || !Boolean.TRUE.equals(subject.getActive())
                || !isFixedActivitySubject(subject.getSubjectType()))) {
            issues.add(FixedActivityScheduleResponse.ValidationIssue.builder()
                    .code("SUBJECT_UNAVAILABLE")
                    .message("The selected subject is no longer active or is not available for fixed scheduling.")
                    .build());
            subject = null;
        }

        if (!units.isEmpty()) {
            if (subject != null && isAcademicSubject(subject.getSubjectType())) {
                addAcademicFixedActivityIssues(subject, units, issues);
            }
            ScheduleLookup lookup = loadSchedules(year, units);
            Set<DayOfWeek> days = requestedDays.isEmpty() ? lookup.scheduledDays(units) : requestedDays;
            for (UnitRef unit : units) {
                for (DayOfWeek day : days) {
                    DaySchedule schedule = lookup.schedule(unit, day);
                    if (requestedDays.isEmpty() && schedule.periods().isEmpty()) continue;
                    List<TimetablePeriod> teaching = schedule.teachingPeriods();
                    combinations.add(FixedActivityScheduleResponse.Combination.builder()
                            .academicGroupId(unit.groupId())
                            .academicGroupName(unit.group().getName())
                            .sectionId(unit.sectionId())
                            .sectionName(unit.section() == null ? null : unit.section().getName())
                            .unitLabel(unit.label())
                            .dayOfWeek(day.name())
                            .schedulePatternId(schedule.singlePatternId())
                            .schedulePatternName(schedule.patternLabel())
                            .periods(teaching.stream()
                                    .map(period -> FixedActivityScheduleResponse.Period.builder()
                                            .timetablePeriodId(period.getTimetablePeriodId())
                                            .periodNumber(period.getPeriodNumber().intValue())
                                            .name(period.getName())
                                            .startTime(period.getStartTime())
                                            .endTime(period.getEndTime())
                                            .build())
                                    .toList())
                            .build());
                    if (!requestedDays.isEmpty() && teaching.isEmpty()) {
                        issues.add(FixedActivityScheduleResponse.ValidationIssue.builder()
                                .code("DAY_NOT_SCHEDULED")
                                .message("No timetable schedule is configured for " + unit.label() + " on "
                                        + dayName(day) + ". Assign a schedule pattern to that day or choose "
                                        + "another day.")
                                .academicGroupId(unit.groupId())
                                .sectionId(unit.sectionId())
                                .dayOfWeek(day.name())
                                .build());
                    }
                }
            }
            commonPeriods = commonPeriods(combinations);
            String periodName = trimToNull(request.getPeriodName());
            if (periodName != null && !requestedDays.isEmpty()) {
                for (FixedActivityScheduleResponse.Combination combination : combinations) {
                    boolean present = combination.getPeriods().stream()
                            .anyMatch(period -> period.getName() != null
                                    && period.getName().equalsIgnoreCase(periodName));
                    if (!present) {
                        issues.add(FixedActivityScheduleResponse.ValidationIssue.builder()
                                .code("PERIOD_NOT_CONFIGURED")
                                .message(periodNotConfiguredMessage(periodName,
                                        DayOfWeek.valueOf(combination.getDayOfWeek()),
                                        combination.getAcademicGroupName(), combination.getSectionName()))
                                .academicGroupId(combination.getAcademicGroupId())
                                .sectionId(combination.getSectionId())
                                .dayOfWeek(combination.getDayOfWeek())
                                .build());
                    }
                }
            }
            if (!requestedDays.isEmpty() && commonPeriods.isEmpty()) {
                issues.add(FixedActivityScheduleResponse.ValidationIssue.builder()
                        .code("NO_COMMON_PERIOD")
                        .message("The selected days and groups do not share a common teaching period. "
                                + "Create separate fixed activity schedules for the applicable days or "
                                + "choose groups that follow the same schedule pattern.")
                        .build());
            }
            teachers = subject == null ? List.of()
                    : resolveTeacherOptions(subject.getSubjectId(), units);
        }

        return FixedActivityScheduleResponse.builder()
                .academicYearId(yearId)
                .combinations(combinations)
                .commonPeriods(commonPeriods)
                .teachers(teachers)
                .issues(issues)
                .build();
    }

    @Override
    public FixedActivityResponse createFixedActivity(Long yearId, FixedActivityRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        assertYearMutable(year);
        FixedActivityPlan plan = planFixedActivity(year, null, request);
        FixedActivity activity = new FixedActivity();
        activity.setAcademicYear(year);
        applyFixedActivity(activity, request, plan, true);
        activity = fixedActivityRepository.save(activity);
        return toFixedActivityResponse(activity);
    }

    @Override
    public FixedActivityResponse updateFixedActivity(Long activityId, FixedActivityRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        FixedActivity activity = fixedActivityRepository.findDetailedById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed activity not found: " + activityId));
        assertYearMutable(activity.getAcademicYear());
        FixedActivityPlan plan = planFixedActivity(activity.getAcademicYear(), activityId, request);
        applyFixedActivity(activity, request, plan, false);
        activity = fixedActivityRepository.save(activity);
        return toFixedActivityResponse(activity);
    }

    @Override
    public FixedActivityResponse setFixedActivityActive(Long activityId, boolean active) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        FixedActivity activity = fixedActivityRepository.findDetailedById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed activity not found: " + activityId));
        assertYearMutable(activity.getAcademicYear());
        if (!active) {
            Long yearId = activity.getAcademicYear().getAcademicYearId();
            boolean liveTimetable = versionRepository
                    .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId).stream()
                    .anyMatch(v -> ACTIVE_TIMETABLE_STATUSES.contains(v.getStatus()));
            if (liveTimetable) {
                throw new BusinessException("This fixed activity cannot be deactivated while a submitted, "
                        + "approved or published timetable exists for the academic year. The published "
                        + "timetable still references it.");
            }
        }
        activity.setActive(active);
        return toFixedActivityResponse(fixedActivityRepository.save(activity));
    }

    @Override
    public void deleteFixedActivity(Long activityId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        FixedActivity activity = fixedActivityRepository.findDetailedById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed activity not found: " + activityId));
        assertYearMutable(activity.getAcademicYear());
        if (entryRepository.existsByFixedActivity_FixedActivityId(activityId)) {
            throw new BusinessException("This fixed activity is referenced by a generated timetable. "
                    + "Deactivate it instead of deleting it so historical timetable entries stay valid.");
        }
        fixedActivityRepository.delete(activity);
    }

    /**
     * Resolves the authoritative subject, teaching units, configured periods and teacher
     * before anything is written. Validation never silently drops a day, group or period:
     * any incompatible selection aborts the whole save.
     *
     * <p>The returned plan keeps the user's scope (a group-level unit for "All Sections")
     * while validation and period resolution run against every affected section.</p>
     */
    private FixedActivityPlan planFixedActivity(AcademicYear year, Long excludeId, FixedActivityRequest request) {
        Long yearId = year.getAcademicYearId();
        if (request.getSubjectId() == null) {
            throw new BusinessException("Select a subject");
        }
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new BusinessException("The selected subject was not found"));
        if (!Boolean.TRUE.equals(subject.getActive()) || !isFixedActivitySubject(subject.getSubjectType())) {
            throw new BusinessException("Select an active academic, practical or activity subject");
        }
        List<DayOfWeek> days = orderedDays(request.getDays());
        if (days.isEmpty()) {
            throw new BusinessException("Select at least one applicable day");
        }
        if (request.getGroups() == null || request.getGroups().isEmpty()) {
            throw new BusinessException("Select an academic group and section scope");
        }
        String periodName = trimToNull(request.getPeriodName());
        if (periodName == null) {
            throw new BusinessException("Select a configured period");
        }

        List<UnitSelection> selections = saveSelections(request.getGroups());
        List<UnitRef> units = expandUnits(yearId, selections);
        if (units.isEmpty()) {
            throw new BusinessException("Select an academic group and section scope");
        }
        List<UnitRef> scopeUnits = scopeUnits(yearId, selections);
        if (scopeUnits.isEmpty()) {
            throw new BusinessException("Select an academic group and section scope");
        }

        ScheduleLookup lookup = loadSchedules(year, units);
        List<ResolvedPeriod> resolved = new ArrayList<>();
        for (UnitRef unit : units) {
            for (DayOfWeek day : days) {
                TimetablePeriod period = lookup.schedule(unit, day).teachingPeriods().stream()
                        .filter(candidate -> candidate.getName() != null
                                && candidate.getName().equalsIgnoreCase(periodName))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(periodNotConfiguredMessage(
                                periodName, day, unit.group().getName(),
                                unit.section() == null ? null : unit.section().getName())));
                resolved.add(new ResolvedPeriod(unit, day, period));
            }
        }

        if (isAcademicSubject(subject.getSubjectType())) {
            validateAcademicFixedActivityWorkload(yearId, excludeId, subject, units, days.size());
        }
        Staff staff = resolveTeacherForUnits(subject, units);
        validateFixedActivityConflicts(year, excludeId, staff, resolved);
        validateDuplicateFixedActivity(yearId, excludeId, subject, scopeUnits, days, periodName, staff);
        return new FixedActivityPlan(subject, staff, scopeUnits, resolved, days, periodName);
    }

    /**
     * Keeps the requested scope for persistence: a null section id becomes a group-level
     * row, so newly added sections of that group follow the fixed activity automatically.
     */
    private List<UnitRef> scopeUnits(Long yearId, List<UnitSelection> selections) {
        Map<String, UnitRef> units = new LinkedHashMap<>();
        for (UnitSelection selection : selections) {
            if (selection.academicGroupId() == null) continue;
            AcademicGroup group = requireGroupInYear(yearId, selection.academicGroupId());
            if (!Boolean.TRUE.equals(group.getActive())) {
                throw new BusinessException("'" + group.getName() + "' is not an active academic group");
            }
            if (selection.sectionId() != null) {
                AcademicSection section = requireSectionInYear(yearId, selection.sectionId(),
                        group.getAcademicGroupId());
                if (!Boolean.TRUE.equals(section.getActive())) {
                    throw new BusinessException("'" + group.getName() + ", Section " + section.getName()
                            + "' is not active");
                }
                UnitRef unit = new UnitRef(group, section);
                units.putIfAbsent(unit.key(), unit);
            } else {
                UnitRef unit = new UnitRef(group, null);
                units.putIfAbsent(unit.key(), unit);
            }
        }
        return new ArrayList<>(units.values());
    }

    private void validateAcademicFixedActivityWorkload(Long yearId, Long excludeId, Subject subject,
                                                       List<UnitRef> units, int newSessions) {
        for (UnitRef unit : units) {
            AcademicGroupSubjectMapping mapping = activeMappingForUnit(unit, subject.getSubjectId());
            if (mapping == null) {
                throw new BusinessException("'" + subject.getName() + "' is not mapped with weekly periods for "
                        + unit.label() + ". Map the subject and set its weekly periods in Subject Mapping "
                        + "before scheduling a fixed academic lesson.");
            }
            int existing = existingFixedSessions(yearId, excludeId, subject.getSubjectId(), unit);
            int weekly = mapping.getWeeklyPeriods();
            if (existing + newSessions > weekly) {
                throw new BusinessException("'" + subject.getName() + "' for " + unit.label() + " already has "
                        + existing + " fixed period(s) this week and its weekly workload is " + weekly
                        + ". Reduce the fixed days or increase the weekly periods in Subject Mapping.");
            }
        }
    }

    private int existingFixedSessions(Long yearId, Long excludeId, Long subjectId, UnitRef unit) {
        int sessions = 0;
        for (FixedActivity other : fixedActivityRepository.findDetailedByYear(yearId)) {
            if (!other.isActive()) continue;
            if (excludeId != null && excludeId.equals(other.getFixedActivityId())) continue;
            if (other.getSubject() == null || !subjectId.equals(other.getSubject().getSubjectId())) continue;
            boolean coversUnit = Boolean.TRUE.equals(other.getAppliesToAllGroups())
                    || other.getGroups().stream().anyMatch(group ->
                    group.appliesTo(unit.groupId(), unit.sectionId()));
            if (!coversUnit) continue;
            sessions += other.getDays().size();
        }
        return sessions;
    }

    private AcademicGroupSubjectMapping activeMappingForUnit(UnitRef unit, Long subjectId) {
        return mappingRepository
                .findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(unit.groupId(), subjectId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .filter(candidate -> candidate.getWeeklyPeriods() != null
                        && candidate.getWeeklyPeriods() > 0)
                .orElse(null);
    }

    private Staff resolveTeacherForUnits(Subject subject, List<UnitRef> units) {
        List<Staff> eligible = eligibleTeachersForUnits(subject.getSubjectId(), units);
        if (!eligible.isEmpty()) return eligible.get(0);
        if (isAcademicSubject(subject.getSubjectType())) {
            String labels = units.stream().map(UnitRef::label).distinct().collect(Collectors.joining(", "));
            throw new BusinessException("No teacher is allocated to " + subject.getName() + " for " + labels
                    + ". Assign a teacher in Teacher Allocation before saving this fixed academic lesson.");
        }
        return null;
    }

    private List<Staff> eligibleTeachersForUnits(Long subjectId, List<UnitRef> units) {
        Map<Long, Staff> common = null;
        for (UnitRef unit : units) {
            List<Staff> unitTeachers = teachersForUnit(unit, subjectId);
            if (unitTeachers.isEmpty()) return List.of();
            Set<Long> ids = unitTeachers.stream().map(Staff::getStaffId).collect(Collectors.toSet());
            if (common == null) {
                Map<Long, Staff> initial = new LinkedHashMap<>();
                unitTeachers.forEach(staff -> initial.putIfAbsent(staff.getStaffId(), staff));
                common = initial;
            } else {
                common.keySet().retainAll(ids);
            }
            if (common.isEmpty()) return List.of();
        }
        return common == null ? List.of() : new ArrayList<>(common.values());
    }

    private void addAcademicFixedActivityIssues(Subject subject, List<UnitRef> units,
                                                List<FixedActivityScheduleResponse.ValidationIssue> issues) {
        for (UnitRef unit : units) {
            if (activeMappingForUnit(unit, subject.getSubjectId()) != null) continue;
            issues.add(FixedActivityScheduleResponse.ValidationIssue.builder()
                    .code("SUBJECT_NOT_MAPPED")
                    .message("'" + subject.getName() + "' is not mapped with weekly periods for "
                            + unit.label() + ". Map the subject and set its weekly periods in Subject Mapping.")
                    .academicGroupId(unit.groupId())
                    .sectionId(unit.sectionId())
                    .build());
        }
        if (eligibleTeachersForUnits(subject.getSubjectId(), units).isEmpty()) {
            String labels = units.stream().map(UnitRef::label).distinct().collect(Collectors.joining(", "));
            issues.add(FixedActivityScheduleResponse.ValidationIssue.builder()
                    .code("TEACHER_NOT_ALLOCATED")
                    .message("No teacher is allocated to " + subject.getName() + " for " + labels
                            + ". Assign a teacher in Teacher Allocation before saving.")
                    .build());
        }
    }

    private void validateDuplicateFixedActivity(Long yearId, Long excludeId, Subject subject,
                                                List<UnitRef> scopeUnits, List<DayOfWeek> days,
                                                String periodName, Staff staff) {
        String signature = fixedActivitySignature(subject.getSubjectId(), scopeUnits, days, periodName, staff);
        for (FixedActivity other : fixedActivityRepository.findDetailedByYear(yearId)) {
            if (!other.isActive()) continue;
            if (excludeId != null && excludeId.equals(other.getFixedActivityId())) continue;
            if (other.getSubject() == null) continue;
            String otherSignature = fixedActivitySignature(other.getSubject().getSubjectId(),
                    other.getGroups().stream().map(group -> new UnitRef(group.getAcademicGroup(),
                            group.getSection())).toList(),
                    orderedDays(other.getDays()), other.getPeriodName(), other.getStaff());
            if (signature.equals(otherSignature)) {
                throw new BusinessException("An identical fixed activity already exists for '"
                        + subject.getName() + "' on the selected days and period.");
            }
        }
    }

    private static String fixedActivitySignature(Long subjectId, List<UnitRef> units, List<DayOfWeek> days,
                                                 String periodName, Staff staff) {
        String unitSignature = units.stream().map(UnitRef::key).sorted().collect(Collectors.joining(","));
        String daySignature = days.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        String period = periodName == null ? "" : periodName.toLowerCase(Locale.ROOT);
        return subjectId + "|" + unitSignature + "|" + daySignature + "|" + period + "|"
                + (staff == null ? 0 : staff.getStaffId());
    }

    /**
     * Expands a group-level selection into every active section when the group has sections.
     * Groups without sections stay group-level ({@code sectionId = null}).
     */
    private List<UnitRef> expandUnits(Long yearId, List<UnitSelection> selections) {
        Map<String, UnitRef> units = new LinkedHashMap<>();
        for (UnitSelection selection : selections) {
            if (selection.academicGroupId() == null) continue;
            AcademicGroup group = requireGroupInYear(yearId, selection.academicGroupId());
            if (!Boolean.TRUE.equals(group.getActive())) {
                throw new BusinessException("'" + group.getName() + "' is not an active academic group");
            }
            List<AcademicSection> sections = sectionRepository
                    .findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(
                            group.getAcademicGroupId());
            if (selection.sectionId() != null) {
                AcademicSection section = requireSectionInYear(yearId, selection.sectionId(),
                        group.getAcademicGroupId());
                if (!Boolean.TRUE.equals(section.getActive())) {
                    throw new BusinessException("'" + group.getName() + ", Section " + section.getName()
                            + "' is not active");
                }
                UnitRef unit = new UnitRef(group, section);
                units.putIfAbsent(unit.key(), unit);
            } else if (sections.isEmpty()) {
                UnitRef unit = new UnitRef(group, null);
                units.putIfAbsent(unit.key(), unit);
            } else {
                for (AcademicSection section : sections) {
                    UnitRef unit = new UnitRef(group, section);
                    units.putIfAbsent(unit.key(), unit);
                }
            }
        }
        return new ArrayList<>(units.values());
    }

    /**
     * Mirrors {@code TimetablePlanningDataLoader}: an active, effective section-specific
     * assignment overrides group-level rows for the same group and day. Periods always come
     * from the resolved schedule pattern, so the same label may carry a different time.
     */
    private ScheduleLookup loadSchedules(AcademicYear year, List<UnitRef> units) {
        LocalDate referenceDate = referenceDate(year);
        List<GroupScheduleAssignment> assignments = assignmentRepository
                .findDetailedByYear(year.getAcademicYearId()).stream()
                .filter(GroupScheduleAssignment::isActive)
                .filter(assignment -> assignment.isEffectiveOn(referenceDate))
                .toList();
        Map<Long, List<TimetablePeriod>> slotsByPattern = periodRepository
                .findAllSlotsForYear(year.getAcademicYearId()).stream()
                .filter(period -> period.getSchedulePattern() != null)
                .collect(Collectors.groupingBy(period ->
                        period.getSchedulePattern().getSchedulePatternId()));
        slotsByPattern.values().forEach(slots -> slots.sort(Comparator
                .comparing(TimetablePeriod::getStartTime)
                .thenComparing(TimetablePeriod::getPeriodNumber)));

        Map<String, Map<DayOfWeek, DaySchedule>> byUnit = new LinkedHashMap<>();
        for (UnitRef unit : units) {
            Map<DayOfWeek, DaySchedule> byDay = new LinkedHashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                List<GroupScheduleAssignment> dayAssignments = assignments.stream()
                        .filter(assignment -> assignment.getAcademicGroup().getAcademicGroupId()
                                .equals(unit.groupId()))
                        .filter(assignment -> assignment.getDayOfWeek() == day)
                        .toList();
                List<GroupScheduleAssignment> sectionSpecific = unit.section() == null
                        ? List.of()
                        : dayAssignments.stream()
                                .filter(assignment -> assignment.getSection() != null
                                        && assignment.getSection().getSectionId().equals(unit.sectionId()))
                                .toList();
                List<GroupScheduleAssignment> selected = sectionSpecific.isEmpty()
                        ? dayAssignments.stream()
                                .filter(assignment -> assignment.getSection() == null).toList()
                        : sectionSpecific;
                Map<Long, SchedulePattern> patterns = new LinkedHashMap<>();
                for (GroupScheduleAssignment assignment : selected) {
                    patterns.putIfAbsent(assignment.getSchedulePattern().getSchedulePatternId(),
                            assignment.getSchedulePattern());
                }
                List<TimetablePeriod> ordered = new ArrayList<>();
                for (SchedulePattern pattern : patterns.values()) {
                    ordered.addAll(slotsByPattern.getOrDefault(pattern.getSchedulePatternId(), List.of()));
                }
                ordered.sort(Comparator.comparing(TimetablePeriod::getStartTime)
                        .thenComparing(TimetablePeriod::getPeriodNumber));
                byDay.put(day, new DaySchedule(ordered, new ArrayList<>(patterns.values())));
            }
            byUnit.put(unit.key(), byDay);
        }
        return new ScheduleLookup(byUnit);
    }

    private List<FixedActivityScheduleResponse.PeriodOption> commonPeriods(
            List<FixedActivityScheduleResponse.Combination> combinations) {
        if (combinations.isEmpty()) return List.of();
        Map<String, Integer> seenIn = new HashMap<>();
        Map<String, Set<String>> timesByName = new LinkedHashMap<>();
        for (FixedActivityScheduleResponse.Combination combination : combinations) {
            Set<String> namesInCombination = new HashSet<>();
            for (FixedActivityScheduleResponse.Period period : combination.getPeriods()) {
                String name = period.getName();
                if (name == null) continue;
                timesByName.computeIfAbsent(name, key -> new LinkedHashSet<>())
                        .add(periodTime(period.getStartTime(), period.getEndTime()));
                if (namesInCombination.add(name)) {
                    seenIn.merge(name, 1, Integer::sum);
                }
            }
        }
        List<FixedActivityScheduleResponse.PeriodOption> options = new ArrayList<>();
        timesByName.forEach((name, times) -> {
            if (seenIn.getOrDefault(name, 0) != combinations.size()) return;
            boolean varies = times.size() > 1;
            options.add(FixedActivityScheduleResponse.PeriodOption.builder()
                    .name(name)
                    .displayTime(varies ? "Varies by day" : times.iterator().next())
                    .timesVary(varies)
                    .times(new ArrayList<>(times))
                    .build());
        });
        return options;
    }

    private List<FixedActivityScheduleResponse.TeacherOption> resolveTeacherOptions(Long subjectId,
                                                                                   List<UnitRef> units) {
        if (subjectId == null || units.isEmpty()) return List.of();
        Map<Long, Staff> staffById = new LinkedHashMap<>();
        Map<Long, Set<String>> labelsByStaff = new LinkedHashMap<>();
        for (UnitRef unit : units) {
            for (Staff staff : teachersForUnit(unit, subjectId)) {
                staffById.putIfAbsent(staff.getStaffId(), staff);
                labelsByStaff.computeIfAbsent(staff.getStaffId(), key -> new LinkedHashSet<>())
                        .add(unit.label());
            }
        }
        return staffById.values().stream()
                .map(staff -> {
                    Set<String> labels = labelsByStaff.getOrDefault(staff.getStaffId(), Set.of());
                    return FixedActivityScheduleResponse.TeacherOption.builder()
                            .staffId(staff.getStaffId())
                            .staffName(staffName(staff))
                            .eligibleForAllUnits(labels.size() == units.size())
                            .unitLabels(new ArrayList<>(labels))
                            .build();
                })
                .sorted(Comparator.comparing(FixedActivityScheduleResponse.TeacherOption::getStaffName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Staff> teachersForUnit(UnitRef unit, Long subjectId) {
        AcademicGroupSubjectMapping mapping = mappingRepository
                .findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(unit.groupId(), subjectId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElse(null);
        if (mapping == null) return List.of();
        Map<Long, Staff> staff = new LinkedHashMap<>();
        if (unit.section() == null) {
            groupAllocationRepository
                    .findByMapping_AcademicGroupSubjectMappingId(
                            mapping.getAcademicGroupSubjectMappingId())
                    .filter(allocation -> Boolean.TRUE.equals(allocation.getActive()))
                    .map(TeacherGroupAllocation::getStaff)
                    .filter(candidate -> candidate != null && isStaffAvailable(candidate))
                    .ifPresent(candidate -> staff.putIfAbsent(candidate.getStaffId(), candidate));
            return new ArrayList<>(staff.values());
        }
        for (TeacherAllocation allocation : teacherAllocationRepository
                .findByAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(
                        mapping.getAcademicGroupSubjectMappingId())) {
            if (!Boolean.TRUE.equals(allocation.getActive()) || allocation.getSection() == null) continue;
            if (!unit.sectionId().equals(allocation.getSection().getSectionId())) continue;
            List<TeacherAllocationTeacher> allocationTeachers = allocationTeacherRepository
                    .findByTeacherAllocation_TeacherAllocationIdOrderByEffectiveFromDesc(
                            allocation.getTeacherAllocationId());
            // Prefer the active primary allocation when a section has more than one teacher.
            for (TeacherAllocationTeacher teacher : allocationTeachers) {
                if (teacher.getRole() != TeacherAllocationTeacherRole.PRIMARY) continue;
                addAvailableTeacher(staff, teacher);
            }
            for (TeacherAllocationTeacher teacher : allocationTeachers) {
                if (teacher.getRole() == TeacherAllocationTeacherRole.PRIMARY) continue;
                addAvailableTeacher(staff, teacher);
            }
        }
        return new ArrayList<>(staff.values());
    }

    private static void addAvailableTeacher(Map<Long, Staff> staff, TeacherAllocationTeacher teacher) {
        if (!Boolean.TRUE.equals(teacher.getActive()) || teacher.getEffectiveTo() != null) return;
        if (teacher.getStaff() != null && isStaffAvailable(teacher.getStaff())) {
            staff.putIfAbsent(teacher.getStaff().getStaffId(), teacher.getStaff());
        }
    }

    private void validateFixedActivityConflicts(AcademicYear year, Long excludeId, Staff staff,
                                                List<ResolvedPeriod> resolved) {
        List<FixedActivity> existing = fixedActivityRepository.findDetailedByYear(year.getAcademicYearId());
        for (FixedActivity other : existing) {
            if (!other.isActive()) continue;
            if (excludeId != null && excludeId.equals(other.getFixedActivityId())) continue;
            String otherName = other.getSubject() != null ? other.getSubject().getName() : other.getName();
            for (ResolvedPeriod candidate : resolved) {
                if (!other.getDays().contains(candidate.day())) continue;
                boolean coversUnit = Boolean.TRUE.equals(other.getAppliesToAllGroups())
                        || other.getGroups().stream().anyMatch(group -> group.appliesTo(
                                candidate.unit().groupId(), candidate.unit().sectionId()));
                if (!coversUnit) continue;
                if (fixedActivityOverlaps(other, candidate)) {
                    throw new BusinessException(candidate.unit().label() + " already has '" + otherName
                            + "' in " + candidate.period().getName() + " on " + dayName(candidate.day())
                            + ". Choose a different period or day, or deactivate the other fixed activity.");
                }
            }
            if (staff == null || other.getStaff() == null
                    || !staff.getStaffId().equals(other.getStaff().getStaffId())) {
                continue;
            }
            for (ResolvedPeriod candidate : resolved) {
                if (!other.getDays().contains(candidate.day())) continue;
                if (fixedActivityTimeOverlaps(other, candidate)) {
                    throw new BusinessException(staffName(staff) + " is already assigned to '" + otherName
                            + "' on " + dayName(candidate.day()) + " at " + candidate.period().getName()
                            + ". Choose another teacher, period or day.");
                }
            }
        }
    }

    private static boolean fixedActivityOverlaps(FixedActivity other, ResolvedPeriod candidate) {
        FixedActivityPeriod period = other.periodFor(candidate.unit().groupId(), candidate.unit().sectionId(),
                candidate.day());
        if (period != null && period.getTimetablePeriod() != null) {
            return Objects.equals(period.getTimetablePeriod().getTimetablePeriodId(),
                    candidate.period().getTimetablePeriodId());
        }
        return fixedActivityTimeOverlaps(other, candidate);
    }

    private static boolean fixedActivityTimeOverlaps(FixedActivity other, ResolvedPeriod candidate) {
        LocalTime candidateStart = candidate.period().getStartTime();
        LocalTime candidateEnd = candidate.period().getEndTime();
        if (candidateStart == null || candidateEnd == null) return false;

        boolean hasResolvedRows = false;
        if (other.getPeriods() != null) {
            for (FixedActivityPeriod period : other.getPeriods()) {
                if (period.getDayOfWeek() != candidate.day()) continue;
                TimetablePeriod timetablePeriod = period.getTimetablePeriod();
                if (timetablePeriod == null
                        || timetablePeriod.getStartTime() == null
                        || timetablePeriod.getEndTime() == null) {
                    continue;
                }
                hasResolvedRows = true;
                if (timetablePeriod.getStartTime().isBefore(candidateEnd)
                        && candidateStart.isBefore(timetablePeriod.getEndTime())) {
                    return true;
                }
            }
        }
        if (hasResolvedRows) return false;

        LocalTime otherStart = other.getStartTime();
        LocalTime otherEnd = other.getEndTime();
        if (otherStart == null || otherEnd == null) return false;
        return otherStart.isBefore(candidateEnd) && candidateStart.isBefore(otherEnd);
    }

    private void applyFixedActivity(FixedActivity activity, FixedActivityRequest request,
                                    FixedActivityPlan plan, boolean creating) {
        activity.setSubject(plan.subject());
        activity.setStaff(plan.staff());
        activity.setName(plan.subject().getName());
        activity.setPeriodName(plan.periodName());
        activity.setDescription(trimToNull(request.getDescription()));
        activity.setActivityType(plan.subject().getSubjectType().name());
        activity.setAppliesToAllGroups(false);
        activity.setRecurrence("WEEKLY");
        activity.setEffectiveFrom(null);
        activity.setEffectiveTo(null);
        // Status is controlled only by the activate/deactivate actions; edits preserve it.
        if (creating) activity.setActive(true);
        activity.setStartTime(plan.resolved().stream().map(row -> row.period().getStartTime())
                .min(LocalTime::compareTo).orElse(null));
        activity.setEndTime(plan.resolved().stream().map(row -> row.period().getEndTime())
                .max(LocalTime::compareTo).orElse(null));
        activity.getDays().clear();
        activity.getDays().addAll(plan.days());
        syncFixedActivityGroups(activity, plan.units());
        syncFixedActivityPeriods(activity, plan.resolved());
    }

    private void syncFixedActivityGroups(FixedActivity activity, List<UnitRef> units) {
        Map<String, FixedActivityGroup> existing = new LinkedHashMap<>();
        for (FixedActivityGroup link : activity.getGroups()) {
            existing.put(unitKey(link.getAcademicGroup().getAcademicGroupId(),
                    link.getSection() == null ? null : link.getSection().getSectionId()), link);
        }
        Set<String> keep = new HashSet<>();
        for (UnitRef unit : units) {
            keep.add(unit.key());
            if (!existing.containsKey(unit.key())) {
                FixedActivityGroup link = new FixedActivityGroup();
                link.setFixedActivity(activity);
                link.setAcademicGroup(unit.group());
                link.setSection(unit.section());
                activity.getGroups().add(link);
            }
        }
        activity.getGroups().removeIf(link -> !keep.contains(unitKey(
                link.getAcademicGroup().getAcademicGroupId(),
                link.getSection() == null ? null : link.getSection().getSectionId())));
    }

    private void syncFixedActivityPeriods(FixedActivity activity, List<ResolvedPeriod> resolved) {
        Map<String, FixedActivityPeriod> existing = new LinkedHashMap<>();
        for (FixedActivityPeriod row : activity.getPeriods()) {
            existing.put(periodKey(row.getAcademicGroup().getAcademicGroupId(),
                    row.getSection() == null ? null : row.getSection().getSectionId(),
                    row.getDayOfWeek()), row);
        }
        Set<String> keep = new HashSet<>();
        for (ResolvedPeriod row : resolved) {
            String key = periodKey(row.unit().groupId(), row.unit().sectionId(), row.day());
            keep.add(key);
            FixedActivityPeriod link = existing.get(key);
            if (link == null) {
                link = new FixedActivityPeriod();
                link.setFixedActivity(activity);
                link.setAcademicGroup(row.unit().group());
                link.setSection(row.unit().section());
                link.setDayOfWeek(row.day());
                activity.getPeriods().add(link);
            }
            link.setTimetablePeriod(row.period());
        }
        activity.getPeriods().removeIf(row -> !keep.contains(periodKey(
                row.getAcademicGroup().getAcademicGroupId(),
                row.getSection() == null ? null : row.getSection().getSectionId(),
                row.getDayOfWeek())));
    }

    private static String periodKey(Long groupId, Long sectionId, DayOfWeek day) {
        return unitKey(groupId, sectionId) + ":" + day;
    }

    private static List<UnitSelection> saveSelections(List<FixedActivityRequest.Unit> units) {
        if (units == null) return List.of();
        return units.stream()
                .filter(Objects::nonNull)
                .map(unit -> new UnitSelection(unit.getAcademicGroupId(), unit.getSectionId()))
                .toList();
    }

    private static List<UnitSelection> resolveSelections(List<FixedActivityResolveRequest.Unit> units) {
        if (units == null) return List.of();
        return units.stream()
                .filter(Objects::nonNull)
                .map(unit -> new UnitSelection(unit.getAcademicGroupId(), unit.getSectionId()))
                .toList();
    }

    private static List<DayOfWeek> orderedDays(Set<DayOfWeek> days) {
        if (days == null) return List.of();
        return days.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private static LocalDate referenceDate(AcademicYear year) {
        LocalDate today = LocalDate.now();
        if (year.getStartDate() != null && today.isBefore(year.getStartDate())) return year.getStartDate();
        if (year.getEndDate() != null && today.isAfter(year.getEndDate())) return year.getEndDate();
        return today;
    }

    private static boolean isStaffAvailable(Staff staff) {
        if (staff == null || Boolean.FALSE.equals(staff.getActive())) return false;
        return staff.getEmploymentStatus() == null
                || !INACTIVE_EMPLOYMENT.contains(staff.getEmploymentStatus());
    }

    private static boolean isFixedActivitySubject(SubjectType type) {
        return type == SubjectType.ACADEMIC_SUBJECT
                || type == SubjectType.PRACTICAL_LAB
                || type == SubjectType.ACTIVITY;
    }

    private static boolean isAcademicSubject(SubjectType type) {
        return type == SubjectType.ACADEMIC_SUBJECT || type == SubjectType.PRACTICAL_LAB;
    }

    private static String staffName(Staff staff) {
        if (staff == null) return "";
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return ((staff.getFirstName() == null ? "" : staff.getFirstName()) + middle + " "
                + (staff.getLastName() == null ? "" : staff.getLastName())).trim();
    }

    private static String dayName(DayOfWeek day) {
        if (day == null) return "";
        String lower = day.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String periodNotConfiguredMessage(String periodName, DayOfWeek day,
                                                     String groupName, String sectionName) {
        String unit = sectionName == null ? groupName : groupName + ", Section " + sectionName;
        return periodName + " is not configured for " + dayName(day) + " for " + unit
                + ". Create separate fixed activity schedules for the applicable days "
                + "or choose a period available on every selected day.";
    }

    private static String periodTime(LocalTime start, LocalTime end) {
        if (start == null || end == null) return "";
        return start.format(TIME_FORMAT) + " – " + end.format(TIME_FORMAT);
    }

    private record UnitSelection(Long academicGroupId, Long sectionId) {}

    private record UnitRef(AcademicGroup group, AcademicSection section) {
        Long groupId() {
            return group.getAcademicGroupId();
        }

        Long sectionId() {
            return section == null ? null : section.getSectionId();
        }

        String key() {
            return unitKey(groupId(), sectionId());
        }

        String label() {
            return section == null ? group.getName() : group.getName() + ", Section " + section.getName();
        }
    }

    private record ResolvedPeriod(UnitRef unit, DayOfWeek day, TimetablePeriod period) {}

    private record FixedActivityPlan(Subject subject, Staff staff, List<UnitRef> units,
                                     List<ResolvedPeriod> resolved, List<DayOfWeek> days,
                                     String periodName) {}

    private record DaySchedule(List<TimetablePeriod> periods, List<SchedulePattern> patterns) {

        List<TimetablePeriod> teachingPeriods() {
            return periods.stream()
                    .filter(period -> period.getSlotKind() == TimetableSlotKind.TEACHING)
                    .toList();
        }

        Long singlePatternId() {
            return patterns.size() == 1 ? patterns.get(0).getSchedulePatternId() : null;
        }

        String patternLabel() {
            return patterns.isEmpty() ? null : patterns.stream()
                    .map(SchedulePattern::getName)
                    .distinct()
                    .collect(Collectors.joining(", "));
        }
    }

    private record ScheduleLookup(Map<String, Map<DayOfWeek, DaySchedule>> byUnit) {

        DaySchedule schedule(UnitRef unit, DayOfWeek day) {
            DaySchedule schedule = byUnit.getOrDefault(unit.key(), Map.of()).get(day);
            return schedule == null ? new DaySchedule(List.of(), List.of()) : schedule;
        }

        Set<DayOfWeek> scheduledDays(List<UnitRef> units) {
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            for (UnitRef unit : units) {
                byUnit.getOrDefault(unit.key(), Map.of()).forEach((day, schedule) -> {
                    if (!schedule.periods().isEmpty()) days.add(day);
                });
            }
            return days;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private void writeSlots(SchedulePattern pattern, List<SchedulePatternRequest.Slot> slots) {
        List<SchedulePatternRequest.Slot> ordered = new ArrayList<>(slots);
        ordered.sort(Comparator.comparing(SchedulePatternRequest.Slot::getStartTime));
        short order = 1;
        List<TimetablePeriod> existing = periodRepository
                .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(pattern.getSchedulePatternId());
        Map<Short, TimetablePeriod> byOrder = existing.stream()
                .collect(Collectors.toMap(TimetablePeriod::getPeriodNumber, p -> p, (a, b) -> a));
        Set<Short> kept = new HashSet<>();
        for (SchedulePatternRequest.Slot slot : ordered) {
            TimetablePeriod period = byOrder.get(order);
            if (period == null) period = new TimetablePeriod();
            period.setSchedulePattern(pattern);
            period.setPeriodNumber(order);
            period.setName(slot.getName().trim());
            period.setStartTime(slot.getStartTime());
            period.setEndTime(slot.getEndTime());
            period.setSlotKind(slot.getSlotType());
            periodRepository.save(period);
            kept.add(order);
            order++;
        }
        existing.stream().filter(p -> !kept.contains(p.getPeriodNumber()))
                .forEach(periodRepository::delete);
    }

    private boolean patternStructureChanged(SchedulePattern pattern, List<LocalTime> starts, List<LocalTime> ends) {
        List<TimetablePeriod> existing = periodRepository
                .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(pattern.getSchedulePatternId());
        if (existing.size() != starts.size()) return true;
        List<LocalTime> oldStarts = existing.stream().map(TimetablePeriod::getStartTime)
                .sorted().toList();
        List<LocalTime> oldEnds = existing.stream().map(TimetablePeriod::getEndTime)
                .sorted().toList();
        return !oldStarts.equals(starts.stream().sorted().toList())
                || !oldEnds.equals(ends.stream().sorted().toList());
    }

    /**
     * A fixed activity stores the period label and points at a specific period row, so a
     * rename or a slot-kind change is also a structural change for fixed activities.
     */
    private boolean periodLabelsChanged(SchedulePattern pattern, List<SchedulePatternRequest.Slot> slots) {
        List<TimetablePeriod> existing = periodRepository
                .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(pattern.getSchedulePatternId());
        List<SchedulePatternRequest.Slot> ordered = new ArrayList<>(slots);
        ordered.sort(Comparator.comparing(SchedulePatternRequest.Slot::getStartTime));
        if (existing.size() != ordered.size()) return true;
        for (int index = 0; index < existing.size(); index++) {
            TimetablePeriod period = existing.get(index);
            SchedulePatternRequest.Slot slot = ordered.get(index);
            if (period.getSlotKind() != slot.getSlotType()
                    || !Objects.equals(period.getName(), slot.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    private void validatePatternRequest(SchedulePatternRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException("Give the pattern a name");
        }
        if (request.getSlots() == null || request.getSlots().isEmpty()) {
            throw new BusinessException("Add at least one time slot to the pattern");
        }
        List<SchedulePatternRequest.Slot> sorted = new ArrayList<>(request.getSlots());
        sorted.sort(Comparator.comparing(SchedulePatternRequest.Slot::getStartTime));
        int teaching = 0;
        for (SchedulePatternRequest.Slot slot : sorted) {
            if (!StringUtils.hasText(slot.getName())) throw new BusinessException("Every slot needs a name");
            if (slot.getStartTime() == null || slot.getEndTime() == null
                    || !slot.getStartTime().isBefore(slot.getEndTime())) {
                throw new BusinessException("'" + slot.getName() + "' must start before it ends");
            }
            if (slot.getSlotType() == TimetableSlotKind.TEACHING) teaching++;
        }
        if (teaching == 0) throw new BusinessException("A pattern needs at least one teaching slot");
        for (int i = 1; i < sorted.size(); i++) {
            SchedulePatternRequest.Slot prev = sorted.get(i - 1);
            SchedulePatternRequest.Slot cur = sorted.get(i);
            if (cur.getStartTime().isBefore(prev.getEndTime())) {
                throw new BusinessException("'" + prev.getName() + "' overlaps '" + cur.getName() + "'");
            }
        }
    }

    private List<GroupScheduleAssignmentRequest.Unit> resolveUnits(Long yearId, GroupScheduleAssignmentRequest request) {
        List<GroupScheduleAssignmentRequest.Unit> units = new ArrayList<>();
        if (request.getUnits() != null && !request.getUnits().isEmpty()) {
            units.addAll(request.getUnits());
            return units;
        }
        List<Long> groupIds = request.getAcademicGroupIds() == null ? List.of() : request.getAcademicGroupIds();
        for (Long groupId : groupIds) {
            List<AcademicSection> sections = sectionRepository
                    .findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(groupId);
            if (sections.isEmpty()) {
                GroupScheduleAssignmentRequest.Unit unit = new GroupScheduleAssignmentRequest.Unit();
                unit.setAcademicGroupId(groupId);
                units.add(unit);
            } else {
                for (AcademicSection section : sections) {
                    GroupScheduleAssignmentRequest.Unit unit = new GroupScheduleAssignmentRequest.Unit();
                    unit.setAcademicGroupId(groupId);
                    unit.setSectionId(section.getSectionId());
                    units.add(unit);
                }
            }
        }
        return units;
    }

    private void syncWorkingDays(AcademicYear year) {
        List<GroupScheduleAssignment> assignments = assignmentRepository
                .findByAcademicYear_AcademicYearIdAndActiveTrue(year.getAcademicYearId());
        Set<DayOfWeek> working = assignments.stream().map(GroupScheduleAssignment::getDayOfWeek)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
        List<TimetableConfiguration> configs = configurationRepository
                .findByAcademicYear_AcademicYearId(year.getAcademicYearId());
        if (configs.isEmpty()) return;
        TimetableConfiguration config = configs.get(0);
        Map<DayOfWeek, TimetableWorkingDay> existing = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(config.getTimetableConfigurationId())
                .stream().collect(Collectors.toMap(TimetableWorkingDay::getDayOfWeek, w -> w, (a, b) -> a));
        if (working.isEmpty()) return;
        for (DayOfWeek day : working) {
            TimetableWorkingDay wd = existing.get(day);
            if (wd == null) {
                wd = new TimetableWorkingDay();
                wd.setTimetableConfiguration(config);
                wd.setDayOfWeek(day);
            }
            wd.setWorking(true);
            workingDayRepository.save(wd);
        }
    }

    private boolean hasInWorkflowVersion(Long yearId) {
        return versionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId).stream()
                .anyMatch(v -> IN_WORKFLOW.contains(v.getStatus()));
    }

    private static boolean rangesOverlap(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        LocalDate aS = aStart == null ? LocalDate.MIN : aStart;
        LocalDate aE = aEnd == null ? LocalDate.MAX : aEnd;
        LocalDate bS = bStart == null ? LocalDate.MIN : bStart;
        LocalDate bE = bEnd == null ? LocalDate.MAX : bEnd;
        return !aE.isBefore(bS) && !bE.isBefore(aS);
    }

    private static String unitKey(Long groupId, Long sectionId) {
        return groupId + ":" + (sectionId == null ? 0 : sectionId);
    }

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private SchedulePattern requirePattern(Long patternId) {
        return patternRepository.findByIdWithYear(patternId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule pattern not found: " + patternId));
    }

    private AcademicGroup requireGroupInYear(Long yearId, Long groupId) {
        AcademicGroup group = academicGroupRepository.findByIdWithYear(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic group not found: " + groupId));
        if (!Objects.equals(group.getAcademicYear().getAcademicYearId(), yearId)) {
            throw new BusinessException("The selected academic group belongs to a different academic year");
        }
        return group;
    }

    private AcademicSection requireSectionInYear(Long yearId, Long sectionId, Long groupId) {
        AcademicSection section = sectionRepository.findByIdWithAcademicGroup(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        if (!Objects.equals(section.getAcademicGroup().getAcademicGroupId(), groupId)
                || !Objects.equals(section.getAcademicGroup().getAcademicYear().getAcademicYearId(), yearId)) {
            throw new BusinessException("The selected section does not belong to the selected academic group");
        }
        return section;
    }

    private void assertYearMutable(AcademicYear year) {
        if (year.getStatus() == com.thinkerscave.academics.enums.AcademicYearStatus.COMPLETED
                || year.getStatus() == com.thinkerscave.academics.enums.AcademicYearStatus.ARCHIVED) {
            throw new BusinessException("Historical academic years are read-only");
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SchedulePatternResponse toPatternResponse(SchedulePattern pattern,
                                                      List<GroupScheduleAssignment> assignments) {
        List<TimetablePeriod> slots = periodRepository
                .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(pattern.getSchedulePatternId());
        List<SchedulePatternResponse.Slot> slotResponses = slots.stream()
                .map(p -> SchedulePatternResponse.Slot.builder()
                        .periodId(p.getTimetablePeriodId())
                        .slotOrder(p.getPeriodNumber().intValue())
                        .name(p.getName())
                        .slotType(p.getSlotKind())
                        .startTime(p.getStartTime())
                        .endTime(p.getEndTime())
                        .durationMinutes((int) Duration.between(p.getStartTime(), p.getEndTime()).toMinutes())
                        .build())
                .toList();
        Map<String, SchedulePatternResponse.AssignedGroup> uniqueGroups = new LinkedHashMap<>();
        for (GroupScheduleAssignment a : assignments) {
            if (!a.isActive()) continue;
            String key = a.getAcademicGroup().getAcademicGroupId() + ":"
                    + (a.getSection() == null ? 0 : a.getSection().getSectionId());
            uniqueGroups.putIfAbsent(key, SchedulePatternResponse.AssignedGroup.builder()
                    .academicGroupId(a.getAcademicGroup().getAcademicGroupId())
                    .academicGroupName(a.getAcademicGroup().getName())
                    .sectionId(a.getSection() == null ? null : a.getSection().getSectionId())
                    .sectionName(a.getSection() == null ? null : a.getSection().getName())
                    .build());
        }
        List<SchedulePatternResponse.AssignedGroup> groups = new ArrayList<>(uniqueGroups.values());
        int teaching = (int) slots.stream().filter(s -> s.getSlotKind() == TimetableSlotKind.TEACHING).count();
        LocalTime start = slots.stream().map(TimetablePeriod::getStartTime).min(LocalTime::compareTo).orElse(null);
        LocalTime end = slots.stream().map(TimetablePeriod::getEndTime).max(LocalTime::compareTo).orElse(null);
        return SchedulePatternResponse.builder()
                .schedulePatternId(pattern.getSchedulePatternId())
                .academicYearId(pattern.getAcademicYear().getAcademicYearId())
                .name(pattern.getName())
                .description(pattern.getDescription())
                .active(pattern.getActive())
                .teachingSlotCount(teaching)
                .breakSlotCount(slots.size() - teaching)
                .dayStartTime(start)
                .dayEndTime(end)
                .totalDurationMinutes(start != null && end != null
                        ? (int) Duration.between(start, end).toMinutes() : 0)
                .assignedGroupCount(groups.size())
                .assignedGroups(groups)
                .slots(slotResponses)
                .build();
    }

    private GroupScheduleAssignmentResponse toAssignmentResponse(GroupScheduleAssignment a) {
        List<TimetablePeriod> slots = periodRepository
                .findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(
                        a.getSchedulePattern().getSchedulePatternId());
        LocalTime start = slots.stream().map(TimetablePeriod::getStartTime).min(LocalTime::compareTo).orElse(null);
        LocalTime end = slots.stream().map(TimetablePeriod::getEndTime).max(LocalTime::compareTo).orElse(null);
        return GroupScheduleAssignmentResponse.builder()
                .groupScheduleAssignmentId(a.getGroupScheduleAssignmentId())
                .academicGroupId(a.getAcademicGroup().getAcademicGroupId())
                .academicGroupName(a.getAcademicGroup().getName())
                .academicGroupCode(a.getAcademicGroup().getCode())
                .sectionId(a.getSection() == null ? null : a.getSection().getSectionId())
                .sectionName(a.getSection() == null ? null : a.getSection().getName())
                .schedulePatternId(a.getSchedulePattern().getSchedulePatternId())
                .schedulePatternName(a.getSchedulePattern().getName())
                .dayOfWeek(a.getDayOfWeek())
                .startTime(start)
                .endTime(end)
                .effectiveFrom(a.getEffectiveFrom())
                .effectiveTo(a.getEffectiveTo())
                .active(a.getActive())
                .build();
    }

    private FixedActivityResponse toFixedActivityResponse(FixedActivity a) {
        return FixedActivityResponse.builder()
                .fixedActivityId(a.getFixedActivityId())
                .academicYearId(a.getAcademicYear().getAcademicYearId())
                .name(a.getName())
                .description(a.getDescription())
                .activityType(a.getActivityType())
                .appliesToAllGroups(a.getAppliesToAllGroups())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .recurrence(a.getRecurrence())
                .effectiveFrom(a.getEffectiveFrom())
                .effectiveTo(a.getEffectiveTo())
                .active(a.getActive())
                .subjectId(a.getSubject() == null ? null : a.getSubject().getSubjectId())
                .subjectName(a.getSubject() == null ? null : a.getSubject().getName())
                .subjectCode(a.getSubject() == null ? null : a.getSubject().getCode())
                .subjectType(a.getSubject() == null || a.getSubject().getSubjectType() == null
                        ? null : a.getSubject().getSubjectType().name())
                .periodName(a.getPeriodName())
                .staffId(a.getStaff() == null ? null : a.getStaff().getStaffId())
                .staffName(a.getStaff() == null ? null : staffName(a.getStaff()))
                .days(new LinkedHashSet<>(Arrays.stream(DayOfWeek.values())
                        .filter(a.getDays()::contains).toList()))
                .groups(a.getGroups().stream().map(g -> FixedActivityResponse.Group.builder()
                        .academicGroupId(g.getAcademicGroup().getAcademicGroupId())
                        .academicGroupName(g.getAcademicGroup().getName())
                        .sectionId(g.getSection() == null ? null : g.getSection().getSectionId())
                        .sectionName(g.getSection() == null ? null : g.getSection().getName())
                        .build()).toList())
                .periods(a.getPeriods().stream()
                        .sorted(Comparator
                                .comparing((FixedActivityPeriod p) ->
                                                p.getAcademicGroup().getDisplayOrder(),
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(p -> p.getSection() == null ? null
                                                : p.getSection().getDisplayOrder(),
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(FixedActivityPeriod::getDayOfWeek)
                                .thenComparing(p -> p.getTimetablePeriod().getPeriodNumber()))
                        .map(p -> FixedActivityResponse.Period.builder()
                                .timetablePeriodId(p.getTimetablePeriod().getTimetablePeriodId())
                                .academicGroupId(p.getAcademicGroup().getAcademicGroupId())
                                .sectionId(p.getSection() == null ? null : p.getSection().getSectionId())
                                .dayOfWeek(p.getDayOfWeek())
                                .periodName(p.getTimetablePeriod().getName())
                                .periodNumber(p.getTimetablePeriod().getPeriodNumber().intValue())
                                .startTime(p.getTimetablePeriod().getStartTime())
                                .endTime(p.getTimetablePeriod().getEndTime())
                                .schedulePatternId(p.getTimetablePeriod().getSchedulePattern() == null
                                        ? null : p.getTimetablePeriod().getSchedulePattern()
                                                .getSchedulePatternId())
                                .schedulePatternName(p.getTimetablePeriod().getSchedulePattern() == null
                                        ? null : p.getTimetablePeriod().getSchedulePattern().getName())
                                .build())
                        .toList())
                .build();
    }
}
