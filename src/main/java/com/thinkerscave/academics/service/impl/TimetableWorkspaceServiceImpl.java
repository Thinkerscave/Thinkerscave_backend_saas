package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.TimetableEntryRequest;
import com.thinkerscave.academics.dto.response.*;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TimetableWorkspaceService;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningDataLoader;
import com.thinkerscave.academics.timetable.engine.TimetableReadinessEvaluator;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableWorkspaceServiceImpl implements TimetableWorkspaceService {

    private static final Set<TimetableStatus> EDITABLE_STATUSES = EnumSet.of(
            TimetableStatus.DRAFT, TimetableStatus.READY_FOR_REVIEW
    );
    private static final Set<TimetableStatus> PUBLISHED_OR_LATER = EnumSet.of(
            TimetableStatus.PUBLISHED, TimetableStatus.SUPERSEDED, TimetableStatus.ARCHIVED
    );

    private final AcademicYearRepository academicYearRepository;
    private final SectionRepository sectionRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final AcademicGroupSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetableConflictRepository conflictRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableVersionEventRepository eventRepository;
    private final StaffRepository staffRepository;
    private final TimetablePlanningDataLoader planningDataLoader;
    private final TimetableReadinessEvaluator readinessEvaluator;
    private final AcademicsAccessGuard accessGuard;

    // ─── Workspace ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableWorkspaceResponse getWorkspace(Long yearId, LocalDate referenceDate) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        TimetablePlanningData data = planningDataLoader.load(yearId, null, referenceDate);
        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(data);
        List<TimetableVersion> versions = data.getVersions();
        TimetableVersion published = versions.stream()
                .filter(v -> v.getStatus() == TimetableStatus.PUBLISHED)
                .findFirst().orElse(null);
        TimetableVersion latest = versions.isEmpty() ? null : versions.get(0);
        TimetableVersion working = versions.stream()
                .filter(v -> EDITABLE_STATUSES.contains(v.getStatus()))
                .findFirst().orElse(published);

        String state;
        if (working != null && EDITABLE_STATUSES.contains(working.getStatus())
                && (published == null || !Objects.equals(working.getTimetableVersionId(),
                published.getTimetableVersionId()))) {
            state = "DRAFT";
        } else if (published != null) {
            state = "PUBLISHED";
        } else {
            state = "NOT_GENERATED";
        }

        boolean readOnly = year.getStatus() == AcademicYearStatus.COMPLETED
                || year.getStatus() == AcademicYearStatus.ARCHIVED;
        String blocked = null;
        if (readOnly) blocked = "This academic year is read-only.";
        else if (!readiness.isReady()) {
            blocked = "Resolve the " + readiness.getBlockingIssues().size() + " blocking issue(s) first.";
        }

        List<TimetableWorkspaceResponse.SetupItem> setup = buildSetup(data, readiness);
        List<TimetableWorkspaceResponse.Issue> issues = buildIssues(data, readiness);
        if (working != null && EDITABLE_STATUSES.contains(working.getStatus())) {
            for (TimetableConflict conflict : conflictRepository
                    .findDetailedByVersion(working.getTimetableVersionId())) {
                if (conflict.getStatus() != TimetableConflictStatus.OPEN) continue;
                AcademicSection section = conflict.getSection();
                issues.add(TimetableWorkspaceResponse.Issue.builder()
                        .code("TIMETABLE_CONFLICT")
                        .title(conflictLabel(conflict.getConflictType()))
                        .severity(Boolean.TRUE.equals(conflict.getBlocking()) ? "BLOCKING" : "WARNING")
                        .type(conflict.getConflictType() == null ? null : conflict.getConflictType().name())
                        .academicGroupName(section == null || section.getAcademicGroup() == null ? null
                                : section.getAcademicGroup().getName())
                        .sectionName(section == null ? null : section.getName())
                        .timeSlot(conflict.getTimetablePeriod() == null ? null
                                : conflict.getTimetablePeriod().getName())
                        .message(conflict.getMessage())
                        .build());
            }
        }
        issues.sort(Comparator.comparing((TimetableWorkspaceResponse.Issue i) ->
                "BLOCKING".equals(i.getSeverity()) ? 0 : 1));
        int blocking = (int) issues.stream().filter(i -> "BLOCKING".equals(i.getSeverity())).count();
        int warnings = (int) issues.stream().filter(i -> "WARNING".equals(i.getSeverity())).count();

        long unscheduled = 0;
        if (working != null) {
            unscheduled = conflictRepository.findByTimetableVersion_TimetableVersionId(working.getTimetableVersionId())
                    .stream()
                    .filter(A -> A.getBlocking()
                            && A.getConflictType() != null
                            && (A.getConflictType() == TimetableConflictType.PERIOD_CAPACITY_CONFLICT
                            || A.getConflictType() == TimetableConflictType.UNPLACED_LESSON))
                    .count();
        }

        return TimetableWorkspaceResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(readOnly)
                .state(state)
                .overallStatus(readiness.isReady() && blocking == 0 ? "READY" : "BLOCKED")
                .canGenerate(readiness.isReady() && !readOnly)
                .generateBlockedReason(blocked)
                .readiness(readiness)
                .filters(buildFilters(data))
                .setup(setup)
                .issues(issues)
                .blockingIssueCount(blocking)
                .warningIssueCount(warnings)
                .unscheduledLessonCount((int) unscheduled)
                .currentVersion(published == null ? null : toVersionResponse(published, false))
                .latestVersion(latest == null ? null : toVersionResponse(latest, false))
                .workingVersion(working == null ? null : toVersionResponse(working, false))
                .versions(versions.stream().map(v -> toVersionResponse(v, false)).toList())
                .referenceDate(data.getReferenceDate())
                .build();
    }

    private List<TimetableWorkspaceResponse.SetupItem> buildSetup(TimetablePlanningData data,
                                                                  TimetableReadinessResponse readiness) {
        List<AcademicGroupSubjectMapping> mappings = data.getMappings();
        long validMappings = mappings.stream()
                .filter(m -> m.getWeeklyPeriods() != null && m.getWeeklyPeriods() > 0).count();
        String mappingStatus = validMappings == 0 ? "INCOMPLETE"
                : (validMappings == mappings.size() ? "COMPLETE" : "IN_PROGRESS");
        String mappingSummary = validMappings + " / " + mappings.size() + " subjects mapped";

        int schedulable = (int) validMappings;
        int allocated = schedulable - data.getMissingTeachers().size();
        allocated = Math.max(0, allocated);
        String allocationStatus = schedulable == 0 ? "INCOMPLETE"
                : allocated >= schedulable ? "COMPLETE" : "IN_PROGRESS";
        String allocationSummary = allocated + " / " + schedulable + " subjects assigned"
                + (data.getMissingTeachers().isEmpty() ? "" : " • " + data.getMissingTeachers().size() + " pending");

        long units = data.getUnits().size();
        long scheduled = data.getUnits().stream().filter(u -> data.hasSchedule(u.unitId())).count();
        String scheduleStatus = units == 0 ? "INCOMPLETE"
                : scheduled >= units ? "COMPLETE" : scheduled == 0 ? "INCOMPLETE" : "IN_PROGRESS";
        String scheduleSummary = data.getPatterns().stream().filter(SchedulePattern::isActive).count()
                + " patterns • " + scheduled + " groups assigned";

        return List.of(
                TimetableWorkspaceResponse.SetupItem.builder()
                        .key("mapping").label("Subject Mapping").status(mappingStatus)
                        .summary(mappingSummary).completed((int) validMappings).total(mappings.size())
                        .actionLabel("Open Subject Mapping")
                        .actionRoute("/app/academics/subject-mapping")
                        .build(),
                TimetableWorkspaceResponse.SetupItem.builder()
                        .key("allocation").label("Teacher Allocation").status(allocationStatus)
                        .summary(allocationSummary).completed(allocated).total(schedulable)
                        .actionLabel("Go to Teacher Allocation")
                        .actionRoute("/app/academics/teacher-allocation")
                        .build(),
                TimetableWorkspaceResponse.SetupItem.builder()
                        .key("schedule").label("Schedule Configuration").status(scheduleStatus)
                        .summary(scheduleSummary).completed((int) scheduled).total((int) units)
                        .actionLabel("Open Schedule Configuration")
                        .actionRoute("/app/academics/timetable/configure")
                        .build()
        );
    }

    private List<TimetableWorkspaceResponse.Issue> buildIssues(TimetablePlanningData data,
                                                               TimetableReadinessResponse readiness) {
        List<TimetableWorkspaceResponse.Issue> issues = new ArrayList<>();
        for (TimetablePlanningData.MissingTeacher m : data.getMissingTeachers()) {
            issues.add(TimetableWorkspaceResponse.Issue.builder()
                    .code("TEACHER_NOT_ASSIGNED")
                    .title("Teacher not assigned")
                    .severity("BLOCKING")
                    .type("ALLOCATION")
                    .academicGroupName(m.unit().academicGroupName())
                    .sectionName(m.unit().sectionName())
                    .subjectName(m.mapping().getSubject().getName())
                    .message(m.reason().getLabel() + " • " + m.weeklyPeriods() + " weekly periods")
                    .actionLabel("Go to Teacher Allocation")
                    .actionRoute("/app/academics/teacher-allocation")
                    .build());
        }
        Set<String> missingUnits = new HashSet<>();
        for (TimetablePlanningData.Unit unit : data.getUnits()) {
            if (unit.section() != null && !data.hasSchedule(unit.unitId())
                    && missingUnits.add(unit.unitId() + ":" + unit.label())) {
                issues.add(TimetableWorkspaceResponse.Issue.builder()
                        .code("SCHEDULE_NOT_CONFIGURED")
                        .title("Schedule not configured")
                        .severity("BLOCKING")
                        .type("SCHEDULE")
                        .academicGroupName(unit.academicGroupName())
                        .sectionName(unit.sectionName())
                        .message("No schedule pattern assigned")
                        .actionLabel("Go to Schedule Configuration")
                        .actionRoute("/app/academics/timetable/configure")
                        .build());
            }
        }
        for (TimetableReadinessResponse.ReadinessCheck check : readiness.getChecks()) {
            if (check.getStatus() == TimetableReadinessResponse.ReadinessStatus.PASSED) continue;
            if ("TEACHER_ALLOCATION_COMPLETE".equals(check.getCode())
                    || "GROUP_SCHEDULES_ASSIGNED".equals(check.getCode())) continue;
            boolean alreadyCovered = issues.stream().anyMatch(i -> i.getCode().equals(check.getCode()));
            if (alreadyCovered) continue;
            issues.add(TimetableWorkspaceResponse.Issue.builder()
                    .code(check.getCode())
                    .title(check.getTitle() == null ? check.getCode() : check.getTitle())
                    .severity(check.getStatus() == TimetableReadinessResponse.ReadinessStatus.FAILED
                            ? "BLOCKING" : "WARNING")
                    .type(check.getCode())
                    .message(check.getMessage())
                    .actionLabel(check.getActionLabel())
                    .actionRoute(check.getActionRoute())
                    .build());
        }
        issues.sort(Comparator.comparing((TimetableWorkspaceResponse.Issue i) ->
                "BLOCKING".equals(i.getSeverity()) ? 0 : 1));
        return issues;
    }

    private TimetableFiltersResponse buildFilters(TimetablePlanningData data) {
        Comparator<Integer> order = Comparator.nullsLast(Comparator.naturalOrder());
        List<AcademicSection> sections = data.getSections().stream()
                .sorted(Comparator
                        .comparing((AcademicSection s) -> s.getAcademicGroup().getDisplayOrder(), order)
                        .thenComparing(s -> Objects.toString(s.getAcademicGroup().getName(), ""))
                        .thenComparing(AcademicSection::getDisplayOrder, order)
                        .thenComparing(s -> Objects.toString(s.getName(), "")))
                .toList();
        Map<Long, TimetableFiltersResponse.AcademicGroupOption> groups = new LinkedHashMap<>();
        for (AcademicSection s : sections) {
            AcademicGroup group = s.getAcademicGroup();
            groups.computeIfAbsent(group.getAcademicGroupId(),
                            id -> new TimetableFiltersResponse.AcademicGroupOption(id, group.getName(), new ArrayList<>()))
                    .getSections().add(new TimetableFiltersResponse.Option(s.getSectionId(), s.getName()));
        }
        Map<Long, String> teachers = new HashMap<>();
        for (TimetablePlanningData.Lesson lesson : data.getLessons()) {
            teachers.putIfAbsent(lesson.teacher().getStaffId(),
                    TimetablePlanningData.staffName(lesson.teacher()));
        }
        List<TimetableFiltersResponse.Option> teacherOptions = teachers.entrySet().stream()
                .map(e -> new TimetableFiltersResponse.Option(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(o -> o.getName().toLowerCase()))
                .toList();
        return new TimetableFiltersResponse(new ArrayList<>(groups.values()), teacherOptions, List.of());
    }

    // ─── Grid ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableGridResponse getView(Long versionId, String view, Long academicGroupId, Long sectionId,
                                         Long staffId, Long resourceId, LocalDate weekStartDate) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = versionRepository.findByIdWithConfigAndYear(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable version not found: " + versionId));
        Long yearId = version.getAcademicYear().getAcademicYearId();
        LocalDate weekStart = weekStartDate == null
                ? LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                : weekStartDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        TimetablePlanningData data = planningDataLoader.load(yearId, version.getTimetableConfiguration(), weekStart);
        List<TimetableEntry> entries = entryRepository.findGridEntries(versionId);
        Map<Long, Staff> activeStaff = activePrimaryTeachers(yearId);
        Map<Long, Staff> staffById = activeStaff.values().stream()
                .collect(Collectors.toMap(Staff::getStaffId, s -> s, (a, b) -> a));

        boolean teacherView = "TEACHER".equalsIgnoreCase(view);
        TimetablePlanningData.Unit targetUnit = null;
        if (!teacherView) {
            targetUnit = resolveUnit(data, academicGroupId, sectionId);
            if (targetUnit != null) {
                Long unitId = targetUnit.unitId();
                Long targetSectionId = targetUnit.sectionId();
                Long targetGroupId = targetUnit.academicGroupId();
                entries = entries.stream()
                        .filter(e -> targetSectionId != null
                                ? e.getSection() != null && targetSectionId.equals(e.getSection().getSectionId())
                                : (e.getAcademicGroup() != null && targetGroupId.equals(e.getAcademicGroup().getAcademicGroupId()))
                                || (e.getSection() != null && e.getSection().getAcademicGroup() != null
                                && targetGroupId.equals(e.getSection().getAcademicGroup().getAcademicGroupId())))
                        .toList();
                if (entries.isEmpty() && data.getScheduleByUnit().containsKey(unitId)) {
                    // keep empty grid rows from the schedule
                }
            }
        } else {
            Long teacherId = staffId;
            entries = entries.stream()
                    .filter(e -> {
                        Staff teacher = e.getStaff() != null ? e.getStaff()
                                : e.getTeacherAllocation() == null ? null
                                : activeStaff.get(e.getTeacherAllocation().getTeacherAllocationId());
                        return teacher != null && Objects.equals(teacher.getStaffId(), teacherId);
                    })
                    .toList();
        }

        List<TimetablePeriod> periods = new ArrayList<>();
        Set<Long> seenPeriods = new HashSet<>();
        if (targetUnit != null) {
            Map<DayOfWeek, List<TimetablePeriod>> byDay = data.getScheduleByUnit().getOrDefault(
                    targetUnit.unitId(), Map.of());
            byDay.values().forEach(list -> list.forEach(p -> {
                if (seenPeriods.add(p.getTimetablePeriodId())) periods.add(p);
            }));
        } else {
            entries.stream().map(TimetableEntry::getTimetablePeriod).filter(Objects::nonNull)
                    .forEach(p -> {
                        if (seenPeriods.add(p.getTimetablePeriodId())) periods.add(p);
                    });
        }
        if (periods.isEmpty()) {
            entries.stream().map(TimetableEntry::getTimetablePeriod).filter(Objects::nonNull)
                    .forEach(p -> {
                        if (seenPeriods.add(p.getTimetablePeriodId())) periods.add(p);
                    });
        }
        periods.sort(Comparator.comparing(TimetablePeriod::getStartTime, Comparator.nullsLast(LocalTime::compareTo))
                .thenComparing(TimetablePeriod::getPeriodNumber));

        Set<DayOfWeek> days = new TreeSet<>();
        if (targetUnit != null) {
            days.addAll(data.getScheduleByUnit().getOrDefault(targetUnit.unitId(), Map.of()).keySet());
        } else {
            entries.forEach(e -> days.add(e.getDayOfWeek()));
            if (days.isEmpty()) days.addAll(data.getWorkingDays());
        }
        List<DayOfWeek> workingDays = new ArrayList<>(days);

        Map<Long, Staff> teacherByAllocation = activeStaff;
        List<TimetableCellResponse> cells = entries.stream()
                .map(e -> toCellResponse(e, teacherByAllocation, staffById))
                .toList();

        boolean editable = EDITABLE_STATUSES.contains(version.getStatus());

        return TimetableGridResponse.builder()
                .timetableVersionId(versionId)
                .view(view)
                .periods(periods.stream().map(this::toPeriodResponse).toList())
                .workingDays(workingDays)
                .cells(cells)
                .academicGroupId(targetUnit == null ? academicGroupId : targetUnit.academicGroupId())
                .academicGroupName(targetUnit == null ? null : targetUnit.academicGroupName())
                .sectionId(targetUnit == null ? sectionId : targetUnit.sectionId())
                .sectionName(targetUnit == null ? null : targetUnit.sectionName())
                .staffId(staffId)
                .staffName(staffId == null ? null : TimetablePlanningData.staffName(staffById.get(staffId)))
                .editable(editable)
                .weekStartDate(weekStart)
                .weekDates(buildWeekDates(weekStart, workingDays))
                .build();
    }

    private static String conflictLabel(TimetableConflictType type) {
        if (type == null) return "Timetable issue";
        return switch (type) {
            case TEACHER_CONFLICT -> "Teacher clash";
            case SECTION_CONFLICT, ACADEMIC_GROUP_CONFLICT -> "Academic group clash";
            case ROOM_CONFLICT, RESOURCE_CONFLICT -> "Room clash";
            case WORKLOAD_CONFLICT -> "Teacher workload";
            case SUBJECT_ALLOCATION_CONFLICT -> "Teacher not assigned";
            case SUBJECT_PREFERENCE_CONFLICT -> "Time-of-day preference";
            case FIXED_ACTIVITY_CONFLICT -> "Fixed activity conflict";
            case CHOICE_GROUP_CONFLICT -> "Choice group issue";
            case UNPLACED_LESSON, PERIOD_CAPACITY_CONFLICT -> "Lesson could not be scheduled";
            default -> type.name().replace('_', ' ');
        };
    }

    private static List<TimetableGridResponse.DayDate> buildWeekDates(LocalDate weekStart, List<DayOfWeek> days) {
        List<TimetableGridResponse.DayDate> dates = new ArrayList<>();
        for (DayOfWeek day : days) {
            dates.add(TimetableGridResponse.DayDate.builder()
                    .dayOfWeek(day)
                    .date(weekStart.plusDays(day.ordinal()))
                    .build());
        }
        return dates;
    }

    private TimetablePlanningData.Unit resolveUnit(TimetablePlanningData data, Long groupId, Long sectionId) {
        if (sectionId != null) {
            return data.getUnits().stream()
                    .filter(u -> u.sectionId() != null && u.sectionId().equals(sectionId))
                    .findFirst().orElse(null);
        }
        if (groupId != null) {
            return data.getUnits().stream()
                    .filter(u -> u.academicGroupId().equals(groupId) && u.section() != null)
                    .findFirst().orElse(null);
        }
        return data.getUnits().stream().filter(u -> u.section() != null).findFirst().orElse(null);
    }

    // ─── Manual editing ───────────────────────────────────────────────────

    @Override
    public TimetableCellResponse addEntry(Long versionId, TimetableEntryRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = requireEditableVersion(versionId);
        if (request.getSectionId() == null || request.getAcademicGroupSubjectMappingId() == null
                || request.getPeriodId() == null || request.getDayOfWeek() == null) {
            throw new BusinessException("Section, subject, day and slot are required to place a lesson");
        }
        AcademicSection section = sectionRepository.findByIdWithAcademicGroup(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        AcademicGroupSubjectMapping mapping = mappingRepository.findById(
                        request.getAcademicGroupSubjectMappingId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject mapping not found"));
        if (!mapping.getAcademicGroup().getAcademicGroupId()
                .equals(section.getAcademicGroup().getAcademicGroupId())) {
            throw new BusinessException("The selected subject does not belong to the selected group");
        }
        TeacherAllocation allocation = allocationRepository
                .findBySection_SectionIdAndAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(
                        section.getSectionId(), mapping.getAcademicGroupSubjectMappingId())
                .orElseThrow(() -> new BusinessException("Assign a teacher to this subject before placing it"));
        Staff staff = request.getStaffId() == null ? primaryTeacher(allocation) : staffById(request.getStaffId());
        if (staff == null) throw new BusinessException("Choose an active teacher for this lesson");

        TimetablePlanningData data = planningDataLoader.load(version.getAcademicYear().getAcademicYearId(),
                version.getTimetableConfiguration(), null);
        TimetablePlanningData.Unit unit = data.getUnits().stream()
                .filter(u -> Objects.equals(u.sectionId(), section.getSectionId()))
                .findFirst().orElseThrow(() -> new BusinessException("This section is not an active scheduling unit"));
        TimetablePeriod period = requireAllowedSlot(data, unit, request.getDayOfWeek(), request.getPeriodId());

        assertNoClashes(versionId, unit, request.getDayOfWeek(), period, staff.getStaffId(), null);

        TimetableEntry entry = new TimetableEntry();
        entry.setTimetableVersion(version);
        entry.setDayOfWeek(request.getDayOfWeek());
        entry.setTimetablePeriod(period);
        entry.setSection(section);
        entry.setAcademicGroup(section.getAcademicGroup());
        entry.setSubject(mapping.getSubject());
        entry.setTeacherAllocation(allocation);
        entry.setStaff(staff);
        entry.setEntryType(TimetableEntryType.SUBJECT);
        entry.setSubjectNameSnapshot(mapping.getSubject().getName());
        entry.setLocked(Boolean.TRUE.equals(request.getLocked()));
        entry = entryRepository.save(entry);
        recordEvent(version, "MANUAL_EDIT", version.getStatus().name(), version.getStatus().name(),
                "Placed " + mapping.getSubject().getName() + " for " + unit.label());
        return toCellResponse(entry, Map.of(), Map.of(staff.getStaffId(), staff));
    }

    @Override
    public TimetableCellResponse updateEntry(Long entryId, TimetableEntryRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + entryId));
        TimetableVersion version = requireEditableVersion(entry.getTimetableVersion().getTimetableVersionId());
        if (Boolean.TRUE.equals(entry.getLocked()) && request.getLocked() == null) {
            throw new BusinessException("This lesson is locked. Unlock it before moving or changing it.");
        }
        TimetablePlanningData data = planningDataLoader.load(version.getAcademicYear().getAcademicYearId(),
                version.getTimetableConfiguration(), null);
        Long entrySectionId = entry.getSection() == null ? null : entry.getSection().getSectionId();
        Long entryGroupId = entry.getAcademicGroup() == null ? null
                : entry.getAcademicGroup().getAcademicGroupId();
        TimetablePlanningData.Unit unit = data.getUnits().stream()
                .filter(u -> entrySectionId != null
                        ? Objects.equals(u.sectionId(), entrySectionId)
                        : Objects.equals(u.academicGroupId(), entryGroupId))
                .findFirst().orElse(null);
        DayOfWeek day = request.getDayOfWeek() == null ? entry.getDayOfWeek() : request.getDayOfWeek();
        Long periodId = request.getPeriodId() == null
                ? entry.getTimetablePeriod().getTimetablePeriodId() : request.getPeriodId();
        TimetablePeriod period = entry.getTimetablePeriod();
        if (unit != null) {
            period = requireAllowedSlot(data, unit, day, periodId);
        } else if (!periodId.equals(period.getTimetablePeriodId())) {
            period = periodRepository.findById(periodId)
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        }
        Staff staff = entry.getStaff();
        if (request.getStaffId() != null) {
            staff = staffById(request.getStaffId());
            if (staff == null) throw new BusinessException("Choose an active teacher");
        }
        if (request.getTeacherAllocationId() != null) {
            TeacherAllocation allocation = allocationRepository.findById(request.getTeacherAllocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher allocation not found"));
            entry.setTeacherAllocation(allocation);
            if (request.getStaffId() == null) staff = primaryTeacher(allocation);
        }
        if (staff == null) throw new BusinessException("This lesson has no teacher");

        assertNoClashes(version.getTimetableVersionId(), unit, day, period, staff.getStaffId(), entryId);

        entry.setDayOfWeek(day);
        entry.setTimetablePeriod(period);
        entry.setStaff(staff);
        if (request.getLocked() != null) entry.setLocked(request.getLocked());
        entry = entryRepository.save(entry);
        recordEvent(version, "MANUAL_EDIT", version.getStatus().name(), version.getStatus().name(),
                "Updated a lesson slot");
        return toCellResponse(entry, Map.of(), Map.of(staff.getStaffId(), staff));
    }

    @Override
    public void deleteEntry(Long entryId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + entryId));
        TimetableVersion version = requireEditableVersion(entry.getTimetableVersion().getTimetableVersionId());
        if (Boolean.TRUE.equals(entry.getLocked())) {
            throw new BusinessException("Unlock this lesson before removing it");
        }
        entryRepository.delete(entry);
        recordEvent(version, "MANUAL_EDIT", version.getStatus().name(), version.getStatus().name(),
                "Removed a lesson from the draft");
    }

    @Override
    public TimetableCellResponse setEntryLocked(Long entryId, boolean locked) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found: " + entryId));
        requireEditableVersion(entry.getTimetableVersion().getTimetableVersionId());
        entry.setLocked(locked);
        entry = entryRepository.save(entry);
        return toCellResponse(entry, Map.of(), Map.of());
    }

    private void assertNoClashes(Long versionId, TimetablePlanningData.Unit unit, DayOfWeek day,
                                 TimetablePeriod period, Long staffId, Long excludeEntryId) {
        List<TimetableEntry> entries = entryRepository.findByTimetableVersion_TimetableVersionId(versionId);
        LocalTime start = period.getStartTime();
        LocalTime end = period.getEndTime();
        for (TimetableEntry other : entries) {
            if (excludeEntryId != null && excludeEntryId.equals(other.getTimetableEntryId())) continue;
            if (other.getDayOfWeek() != day || other.getTimetablePeriod() == null) continue;
            LocalTime otherStart = other.getTimetablePeriod().getStartTime();
            LocalTime otherEnd = other.getTimetablePeriod().getEndTime();
            if (start == null || end == null || otherStart == null || otherEnd == null) continue;
            boolean overlaps = start.isBefore(otherEnd) && otherStart.isBefore(end);
            if (!overlaps) continue;
            boolean sameUnit = unit != null && (
                    (unit.sectionId() != null && other.getSection() != null
                            && unit.sectionId().equals(other.getSection().getSectionId()))
                    || (unit.sectionId() == null && other.getAcademicGroup() != null
                            && unit.academicGroupId().equals(other.getAcademicGroup().getAcademicGroupId())));
            if (sameUnit) {
                throw new BusinessException("This group/section already has a lesson at "
                        + start + "–" + end + " (" + other.getSubjectNameSnapshot() + ")");
            }
            Staff otherStaff = other.getStaff();
            if (staffId != null && otherStaff != null && staffId.equals(otherStaff.getStaffId())) {
                throw new BusinessException("The teacher is already booked at " + start + "–" + end
                        + " for another group");
            }
        }
    }

    private TimetablePeriod requireAllowedSlot(TimetablePlanningData data, TimetablePlanningData.Unit unit,
                                               DayOfWeek day, Long periodId) {
        boolean allowed = data.slotsFor(unit.unitId(), day).stream()
                .anyMatch(p -> p.getTimetablePeriodId().equals(periodId));
        if (!allowed) {
            throw new BusinessException("That slot is not part of the group's assigned schedule for " + day);
        }
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + periodId));
    }

    // ─── Validation ───────────────────────────────────────────────────────

    @Override
    public List<TimetableConflictResponse> validateVersion(Long versionId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = requireEditableVersion(versionId);
        List<TimetableEntry> entries = entryRepository.findGridEntries(versionId);
        List<TimetableConflict> created = new ArrayList<>();
        Map<String, List<TimetableEntry>> unitDay = new HashMap<>();
        Map<String, List<TimetableEntry>> teacherDay = new HashMap<>();
        for (TimetableEntry e : entries) {
            Long unitKey = e.getSection() != null ? e.getSection().getSectionId()
                    : (e.getAcademicGroup() != null ? -e.getAcademicGroup().getAcademicGroupId() : null);
            if (unitKey != null) unitDay.computeIfAbsent(unitKey + "|" + e.getDayOfWeek(),
                    k -> new ArrayList<>()).add(e);
            if (e.getStaff() != null) teacherDay.computeIfAbsent(
                    e.getStaff().getStaffId() + "|" + e.getDayOfWeek(), k -> new ArrayList<>()).add(e);
        }
        List<String> messages = new ArrayList<>();
        unitDay.values().forEach(list -> messages.addAll(overlapMessages(list, false)));
        teacherDay.values().forEach(list -> messages.addAll(overlapMessages(list, true)));
        for (String message : messages) {
            TimetableConflict conflict = new TimetableConflict();
            conflict.setTimetableVersion(version);
            conflict.setConflictType(message.startsWith("Teacher") ? TimetableConflictType.TEACHER_CONFLICT
                    : TimetableConflictType.SECTION_CONFLICT);
            conflict.setBlocking(true);
            conflict.setStatus(TimetableConflictStatus.OPEN);
            conflict.setMessage(message);
            created.add(conflictRepository.save(conflict));
        }
        recordEvent(version, "VALIDATION", version.getStatus().name(), version.getStatus().name(),
                messages.isEmpty() ? "No conflicts found" : messages.size() + " conflict(s) found");
        if (created.isEmpty()) {
            return conflictRepository.findDetailedByVersion(versionId).stream()
                    .map(A -> toConflictResponse(A, versionId)).toList();
        }
        return conflictRepository.findDetailedByVersion(versionId).stream()
                .map(A -> toConflictResponse(A, versionId)).toList();
    }

    private static List<String> overlapMessages(List<TimetableEntry> list, boolean teacher) {
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                TimetableEntry a = list.get(i);
                TimetableEntry b = list.get(j);
                if (a.getTimetablePeriod() == null || b.getTimetablePeriod() == null) continue;
                LocalTime aS = a.getTimetablePeriod().getStartTime();
                LocalTime aE = a.getTimetablePeriod().getEndTime();
                LocalTime bS = b.getTimetablePeriod().getStartTime();
                LocalTime bE = b.getTimetablePeriod().getEndTime();
                if (aS == null || aE == null || bS == null || bE == null) continue;
                if (aS.isBefore(bE) && bS.isBefore(aE)) {
                    messages.add((teacher ? "Teacher clash: " + TimetablePlanningData.staffName(a.getStaff())
                            + " is booked for " : "Academic group clash: ")
                            + a.getSubjectNameSnapshot() + " and " + b.getSubjectNameSnapshot()
                            + " overlap on " + a.getDayOfWeek() + " " + aS + "–" + aE);
                }
            }
        }
        return messages;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private TimetableVersion requireEditableVersion(Long versionId) {
        TimetableVersion version = versionRepository.findByIdWithConfigAndYear(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable version not found: " + versionId));
        if (!EDITABLE_STATUSES.contains(version.getStatus())) {
            throw new BusinessException("Only a draft timetable can be edited. Current status: "
                    + version.getStatus());
        }
        AcademicYear year = version.getAcademicYear();
        if (year.getStatus() == AcademicYearStatus.COMPLETED || year.getStatus() == AcademicYearStatus.ARCHIVED) {
            throw new BusinessException("Historical academic years are read-only");
        }
        return version;
    }

    private Staff primaryTeacher(TeacherAllocation allocation) {
        return allocationTeacherRepository
                .findFirstByTeacherAllocation_TeacherAllocationIdAndActiveTrueAndEffectiveToIsNullAndRoleOrderByEffectiveFromDesc(
                        allocation.getTeacherAllocationId(), TeacherAllocationTeacherRole.PRIMARY)
                .map(TeacherAllocationTeacher::getStaff)
                .orElse(null);
    }

    private Staff staffById(Long staffId) {
        return staffRepository.findById(staffId).orElse(null);
    }

    private Map<Long, Staff> activePrimaryTeachers(Long yearId) {
        Map<Long, TeacherAllocationTeacher> latest = new HashMap<>();
        for (TeacherAllocationTeacher t : allocationTeacherRepository.findActiveByYear(yearId)) {
            if (t.getRole() != TeacherAllocationTeacherRole.PRIMARY || t.getStaff() == null) continue;
            latest.merge(t.getTeacherAllocation().getTeacherAllocationId(), t, (a, b) -> {
                if (a.getEffectiveFrom() == null) return b;
                if (b.getEffectiveFrom() == null) return a;
                return b.getEffectiveFrom().isAfter(a.getEffectiveFrom()) ? b : a;
            });
        }
        Map<Long, Staff> result = new HashMap<>();
        latest.forEach((allocationId, t) -> result.put(allocationId, t.getStaff()));
        return result;
    }

    private TimetablePeriodResponse toPeriodResponse(TimetablePeriod p) {
        return TimetablePeriodResponse.builder()
                .timetablePeriodId(p.getTimetablePeriodId())
                .periodNumber(p.getPeriodNumber())
                .name(p.getName())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .slotKind(p.getSlotKind())
                .schedulePatternId(p.getSchedulePattern() == null ? null
                        : p.getSchedulePattern().getSchedulePatternId())
                .schedulePatternName(p.getSchedulePattern() == null ? null
                        : p.getSchedulePattern().getName())
                .durationMinutes(p.getStartTime() != null && p.getEndTime() != null
                        ? (int) java.time.Duration.between(p.getStartTime(), p.getEndTime()).toMinutes() : null)
                .build();
    }

    private TimetableCellResponse toCellResponse(TimetableEntry entry, Map<Long, Staff> teacherByAllocation,
                                                 Map<Long, Staff> staffById) {
        Staff teacher = entry.getStaff();
        if (teacher == null && entry.getTeacherAllocation() != null) {
            teacher = teacherByAllocation.get(entry.getTeacherAllocation().getTeacherAllocationId());
        }
        AcademicSection section = entry.getSection();
        AcademicGroup group = entry.getAcademicGroup() != null ? entry.getAcademicGroup()
                : section == null ? null : section.getAcademicGroup();
        return TimetableCellResponse.builder()
                .dayOfWeek(entry.getDayOfWeek())
                .periodId(entry.getTimetablePeriod().getTimetablePeriodId())
                .periodNumber(entry.getTimetablePeriod().getPeriodNumber())
                .entryId(entry.getTimetableEntryId())
                .entryType(entry.getEntryType())
                .sectionId(section == null ? null : section.getSectionId())
                .sectionName(section == null ? null : section.getName())
                .academicGroupName(group == null ? null : group.getName())
                .subjectId(entry.getSubject() == null ? null : entry.getSubject().getSubjectId())
                .subjectName(entry.getSubjectNameSnapshot())
                .academicGroupSubjectMappingId(entry.getTeacherAllocation() == null ? null
                        : entry.getTeacherAllocation().getAcademicGroupSubjectMapping()
                        .getAcademicGroupSubjectMappingId())
                .staffId(teacher == null ? null : teacher.getStaffId())
                .staffName(teacher == null ? null : TimetablePlanningData.staffName(teacher))
                .teacherAllocationId(entry.getTeacherAllocation() == null ? null
                        : entry.getTeacherAllocation().getTeacherAllocationId())
                .fixedActivityId(entry.getFixedActivity() == null ? null
                        : entry.getFixedActivity().getFixedActivityId())
                .fixedActivityType(entry.getFixedActivity() == null ? null
                        : entry.getFixedActivity().getActivityType())
                .resourceId(entry.getResource() == null ? null : entry.getResource().getAcademicResourceId())
                .resourceName(entry.getResource() == null ? null : entry.getResource().getName())
                .locked(entry.getLocked())
                .startTime(entry.getTimetablePeriod().getStartTime())
                .endTime(entry.getTimetablePeriod().getEndTime())
                .build();
    }

    private TimetableVersionResponse toVersionResponse(TimetableVersion v, boolean withEvents) {
        Long versionId = v.getTimetableVersionId();
        long openBlocking = conflictRepository
                .countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                        versionId, TimetableConflictStatus.OPEN);
        List<TimetableVersionResponse.Event> events = withEvents ? eventRepository
                .findByTimetableVersion_TimetableVersionIdOrderByCreatedOnAsc(versionId).stream()
                .map(e -> TimetableVersionResponse.Event.builder()
                        .eventId(e.getTimetableVersionEventId())
                        .eventType(e.getEventType())
                        .fromStatus(e.getFromStatus())
                        .toStatus(e.getToStatus())
                        .note(e.getNote())
                        .at(e.getCreatedOn())
                        .actorUserId(e.getActorUserId())
                        .build())
                .toList() : null;
        return TimetableVersionResponse.builder()
                .timetableVersionId(versionId)
                .academicYearId(v.getAcademicYear().getAcademicYearId())
                .timetableConfigurationId(v.getTimetableConfiguration() == null ? null
                        : v.getTimetableConfiguration().getTimetableConfigurationId())
                .versionNumber(v.getVersionNumber())
                .generationStatus(v.getGenerationStatus())
                .status(v.getStatus())
                .generatedAt(v.getGeneratedAt())
                .approvedAt(v.getApprovedAt())
                .approvedByUserId(v.getApprovedByUserId())
                .publishedAt(v.getPublishedAt())
                .publishedByUserId(v.getPublishedByUserId())
                .supersededAt(v.getSupersededAt())
                .totalEntries(entryRepository.countByTimetableVersion_TimetableVersionId(versionId))
                .totalConflicts(conflictRepository.countByTimetableVersion_TimetableVersionId(versionId))
                .openBlockingConflicts(openBlocking)
                .unplacedLessonCount(conflictRepository.findByTimetableVersion_TimetableVersionId(versionId)
                        .stream()
                        .filter(A -> A.getBlocking() && A.getConflictType() != null
                                && (A.getConflictType() == TimetableConflictType.PERIOD_CAPACITY_CONFLICT
                                || A.getConflictType() == TimetableConflictType.UNPLACED_LESSON))
                        .count())
                .editable(EDITABLE_STATUSES.contains(v.getStatus()))
                .events(events)
                .build();
    }

    private TimetableConflictResponse toConflictResponse(TimetableConflict A, Long versionId) {
        AcademicSection section = A.getSection();
        return TimetableConflictResponse.builder()
                .timetableConflictId(A.getTimetableConflictId())
                .timetableVersionId(versionId)
                .conflictType(A.getConflictType())
                .blocking(A.getBlocking())
                .status(A.getStatus())
                .message(A.getMessage())
                .entryId(A.getTimetableEntry() == null ? null : A.getTimetableEntry().getTimetableEntryId())
                .relatedEntryId(A.getRelatedTimetableEntry() == null ? null
                        : A.getRelatedTimetableEntry().getTimetableEntryId())
                .sectionId(section == null ? null : section.getSectionId())
                .sectionName(section == null ? null : section.getName())
                .academicGroupName(section == null || section.getAcademicGroup() == null ? null
                        : section.getAcademicGroup().getName())
                .teacherAllocationId(A.getTeacherAllocation() == null ? null
                        : A.getTeacherAllocation().getTeacherAllocationId())
                .resourceId(A.getResource() == null ? null : A.getResource().getAcademicResourceId())
                .dayOfWeek(A.getDayOfWeek())
                .periodId(A.getTimetablePeriod() == null ? null
                        : A.getTimetablePeriod().getTimetablePeriodId())
                .periodName(A.getTimetablePeriod() == null ? null : A.getTimetablePeriod().getName())
                .resolvedAt(A.getResolvedAt())
                .resolvedByUserId(A.getResolvedByUserId())
                .build();
    }

    private void recordEvent(TimetableVersion version, String eventType, String from, String to, String note) {
        TimetableVersionEvent event = new TimetableVersionEvent();
        event.setTimetableVersion(version);
        event.setEventType(eventType);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setNote(note);
        event.setActorUserId(accessGuard.currentUserIdOrNull());
        eventRepository.save(event);
    }
}
