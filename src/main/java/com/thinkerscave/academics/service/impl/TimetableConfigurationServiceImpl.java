package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.FixedActivityRequest;
import com.thinkerscave.academics.dto.request.GroupScheduleAssignmentRequest;
import com.thinkerscave.academics.dto.request.SchedulePatternRequest;
import com.thinkerscave.academics.dto.response.FixedActivityResponse;
import com.thinkerscave.academics.dto.response.GroupAssignmentMatrixResponse;
import com.thinkerscave.academics.dto.response.GroupScheduleAssignmentResponse;
import com.thinkerscave.academics.dto.response.SchedulePatternResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TimetableConfigurationService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableConfigurationServiceImpl implements TimetableConfigurationService {

    private static final EnumSet<TimetableStatus> IN_WORKFLOW = EnumSet.of(
            TimetableStatus.PENDING_APPROVAL, TimetableStatus.APPROVED,
            TimetableStatus.PUBLISHED, TimetableStatus.SUPERSEDED);

    private final AcademicYearRepository academicYearRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SectionRepository sectionRepository;
    private final SchedulePatternRepository patternRepository;
    private final TimetablePeriodRepository periodRepository;
    private final GroupScheduleAssignmentRepository assignmentRepository;
    private final FixedActivityRepository fixedActivityRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableWorkingDayRepository workingDayRepository;
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
    public FixedActivityResponse createFixedActivity(Long yearId, FixedActivityRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        assertYearMutable(year);
        validateFixedActivity(year, null, request);
        FixedActivity activity = new FixedActivity();
        activity.setAcademicYear(year);
        applyFixedActivity(activity, request);
        activity = fixedActivityRepository.save(activity);
        return toFixedActivityResponse(activity);
    }

    @Override
    public FixedActivityResponse updateFixedActivity(Long activityId, FixedActivityRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        FixedActivity activity = fixedActivityRepository.findDetailedById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed activity not found: " + activityId));
        assertYearMutable(activity.getAcademicYear());
        validateFixedActivity(activity.getAcademicYear(), activityId, request);
        applyFixedActivity(activity, request);
        activity = fixedActivityRepository.save(activity);
        return toFixedActivityResponse(activity);
    }

    @Override
    public FixedActivityResponse setFixedActivityActive(Long activityId, boolean active) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        FixedActivity activity = fixedActivityRepository.findDetailedById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed activity not found: " + activityId));
        assertYearMutable(activity.getAcademicYear());
        activity.setActive(active);
        return toFixedActivityResponse(fixedActivityRepository.save(activity));
    }

    @Override
    public void deleteFixedActivity(Long activityId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        FixedActivity activity = fixedActivityRepository.findDetailedById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixed activity not found: " + activityId));
        assertYearMutable(activity.getAcademicYear());
        fixedActivityRepository.delete(activity);
    }

    private void validateFixedActivity(AcademicYear year, Long excludeId, FixedActivityRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessException("The activity must start before it ends");
        }
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BusinessException("Select at least one day for the fixed activity");
        }
        if (request.getEffectiveFrom() != null && request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new BusinessException("The effective end date must be on or after the start date");
        }
        if (Boolean.FALSE.equals(request.getAppliesToAllGroups())
                && (request.getGroups() == null || request.getGroups().isEmpty())) {
            throw new BusinessException("Select the academic groups this activity applies to");
        }
        List<FixedActivity> existing = fixedActivityRepository.findDetailedByYear(year.getAcademicYearId());
        for (FixedActivity other : existing) {
            if (excludeId != null && excludeId.equals(other.getFixedActivityId())) continue;
            if (!other.isActive() || !rangesOverlap(other.getEffectiveFrom(), other.getEffectiveTo(),
                    request.getEffectiveFrom(), request.getEffectiveTo())) continue;
            boolean dayOverlap = other.getDays().stream().anyMatch(request.getDays()::contains);
            boolean timeOverlap = request.getStartTime().isBefore(other.getEndTime())
                    && other.getStartTime().isBefore(request.getEndTime());
            if (dayOverlap && timeOverlap) {
                throw new BusinessException("This activity overlaps '" + other.getName()
                        + "' on the same day and time");
            }
        }
    }

    private void applyFixedActivity(FixedActivity activity, FixedActivityRequest request) {
        activity.setName(request.getName().trim());
        activity.setDescription(trimToNull(request.getDescription()));
        activity.setActivityType(StringUtils.hasText(request.getActivityType())
                ? request.getActivityType().trim().toUpperCase(Locale.ROOT) : "GENERAL");
        activity.setAppliesToAllGroups(request.getAppliesToAllGroups() == null || request.getAppliesToAllGroups());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setRecurrence(StringUtils.hasText(request.getRecurrence())
                ? request.getRecurrence().trim().toUpperCase(Locale.ROOT) : "WEEKLY");
        activity.setEffectiveFrom(request.getEffectiveFrom());
        activity.setEffectiveTo(request.getEffectiveTo());
        activity.setActive(request.getActive() == null || request.getActive());
        activity.getDays().clear();
        activity.getDays().addAll(request.getDays());
        activity.getGroups().clear();
        if (!Boolean.TRUE.equals(activity.getAppliesToAllGroups())) {
            Long yearId = activity.getAcademicYear().getAcademicYearId();
            for (FixedActivityRequest.Unit unit : request.getGroups()) {
                AcademicGroup group = requireGroupInYear(yearId, unit.getAcademicGroupId());
                FixedActivityGroup link = new FixedActivityGroup();
                link.setFixedActivity(activity);
                link.setAcademicGroup(group);
                link.setSection(unit.getSectionId() == null ? null
                        : requireSectionInYear(yearId, unit.getSectionId(), group.getAcademicGroupId()));
                activity.getGroups().add(link);
            }
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
                .days(new java.util.LinkedHashSet<>(Arrays.stream(DayOfWeek.values())
                        .filter(a.getDays()::contains).toList()))
                .groups(a.getGroups().stream().map(g -> FixedActivityResponse.Group.builder()
                        .academicGroupId(g.getAcademicGroup().getAcademicGroupId())
                        .academicGroupName(g.getAcademicGroup().getName())
                        .sectionId(g.getSection() == null ? null : g.getSection().getSectionId())
                        .sectionName(g.getSection() == null ? null : g.getSection().getName())
                        .build()).toList())
                .build();
    }
}
