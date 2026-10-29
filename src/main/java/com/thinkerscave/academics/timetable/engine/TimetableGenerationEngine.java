package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.dto.response.TimetableGenerateResultResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.MissingTeacher;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Unit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TimetableGenerationEngine {

    private static final Logger log = LoggerFactory.getLogger(TimetableGenerationEngine.class);
    public static final String ALGORITHM_VERSION = "v3";
    private static final String SYSTEM_USER = "system";
    private static final int INSERT_BATCH_SIZE = 250;

    private final TimetablePlanningDataLoader planningDataLoader;
    private final RequirementExpander expander;
    private final CandidateCalculator candidateCalculator;
    private final ConstraintScheduler scheduler;
    private final SoftConstraintOptimizer optimizer;
    private final SolutionValidator validator;
    private final TimetableVersionRepository versionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetableConflictRepository conflictRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final AcademicResourceRepository resourceRepository;
    private final SectionRepository sectionRepository;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private record Prepared(SchedulingSourceData source,
                            List<SchedulingRequirement> requirements,
                            List<SolutionValidator.ValidationConflict> fixedIssues,
                            List<Placement> fixedReservations,
                            List<SolutionValidator.ValidationConflict> unscheduled,
                            int unitCount,
                            int fixedActivityCount) {}

    public TimetableGenerateResultResponse execute(Long versionId, long seed,
                                                   GenerationProgress progress) {
        return execute(versionId, seed, progress, null);
    }

    public TimetableGenerateResultResponse execute(Long versionId, long seed,
                                                   GenerationProgress progress,
                                                   LocalDate referenceDate) {
        log.info("Engine start: versionId={}, seed={}, algorithm={}", versionId, seed, ALGORITHM_VERSION);
        long startedAt = System.nanoTime();

        try {
            progress.advanceTo(GenerationPhase.PREPARING_WORKSPACE, 5);
            TimetableVersion version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));

            if (progress.isCancelRequested()) return cancelled(version, progress);

            progress.advanceTo(GenerationPhase.LOADING_DATA, 10);
            Prepared prepared = transactionTemplate.execute(status -> prepare(versionId, referenceDate, progress));
            if (prepared == null) {
                throw new IllegalStateException("Failed to prepare scheduling data for version " + versionId);
            }
            SchedulingSourceData source = prepared.source();
            List<SchedulingRequirement> requirements = prepared.requirements();
            log.info("Expanded {} scheduling requirements, {} fixed reservations, {} unscheduled lesson(s)",
                    requirements.size(), prepared.fixedReservations().size(), prepared.unscheduled().size());

            if (progress.isCancelRequested()) return cancelled(version, progress);

            if (prepared.unitCount() == 0) {
                throw new IllegalStateException("No active academic sections are available for scheduling");
            }
            if (source.getWorkingDays().isEmpty() || source.getTeachingPeriods().isEmpty()) {
                throw new IllegalStateException("No group has an active schedule assignment with teaching slots");
            }

            SolutionValidator.ValidationResult validation;
            SchedulingSolution solution;
            if (requirements.isEmpty() && prepared.fixedReservations().isEmpty()) {
                solution = new SchedulingSolution();
                validation = new SolutionValidator.ValidationResult();
            } else {
                progress.advanceTo(GenerationPhase.CALCULATING_CANDIDATES, 30);
                candidateCalculator.calculateCandidates(requirements, source);

                if (progress.isCancelRequested()) return cancelled(version, progress);

                progress.advanceTo(GenerationPhase.SCHEDULING, 40);
                solution = scheduler.solve(requirements, source, seed, progress,
                        prepared.fixedReservations());

                if (progress.isCancelRequested()) return cancelled(version, progress);

                Map<String, SchedulingRequirement> byId = new HashMap<>();
                for (SchedulingRequirement r : requirements) byId.put(r.getRequirementId(), r);
                progress.advanceTo(GenerationPhase.OPTIMIZING, 72);
                solution = optimizer.optimize(solution, source, progress, byId);

                if (progress.isCancelRequested()) return cancelled(version, progress);

                progress.advanceTo(GenerationPhase.VALIDATING, 82);
                validation = validator.validate(solution, requirements, source);
            }
            prepared.fixedIssues().forEach(validation::addWarning);
            prepared.unscheduled().forEach(validation::addWarning);

            progress.advanceTo(GenerationPhase.GENERATING_CONFLICTS, 90);
            progress.advanceTo(GenerationPhase.FINALIZING, 95);
            SchedulingSolution finalSolution = solution;
            SolutionValidator.ValidationResult finalValidation = validation;
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            TimetableGenerateResultResponse result = transactionTemplate.execute(status ->
                    persist(versionId, finalSolution, finalValidation, prepared, elapsedMs));
            if (result == null) {
                throw new IllegalStateException("Failed to save the generated timetable");
            }
            progress.complete(result.getGenerationStatus(), result.getMessage());
            return result;

        } catch (Exception e) {
            log.error("Engine failed for versionId={}", versionId, e);
            return handleFailure(versionId, e, progress);
        }
    }

    private Prepared prepare(Long versionId, LocalDate referenceDate, GenerationProgress progress) {
        TimetableVersion version = versionRepository.findByIdWithConfigAndYear(versionId)
                .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));
        TimetableConfiguration config = version.getTimetableConfiguration();
        TimetablePlanningData data = planningDataLoader.load(
                version.getAcademicYear().getAcademicYearId(), config, referenceDate);

        SchedulingSourceData source = SchedulingSourceData.builder()
                .configuration(config)
                .workingDays(data.getWorkingDays())
                .teachingPeriods(data.getTeachingPeriods())
                .allPeriods(data.getAllPeriods())
                .schedulesByUnit(data.getScheduleByUnit())
                .maxTeacherWeeklyPeriods(config == null || config.getMaxTeacherWeeklyPeriods() == null
                        ? 30 : config.getMaxTeacherWeeklyPeriods())
                .halfIndex(firstHalfTeachingCount(data.getAllPeriods(), data.getTeachingPeriods().size()))
                .build();

        progress.advanceTo(GenerationPhase.BUILDING_REQUIREMENTS, 20);
        List<SchedulingRequirement> requirements = expander.expand(data.getLessons());

        List<SolutionValidator.ValidationConflict> unscheduled = new ArrayList<>();
        for (MissingTeacher m : data.getMissingTeachers()) {
            unscheduled.add(new SolutionValidator.ValidationConflict(
                    TimetableConflictType.SUBJECT_ALLOCATION_CONFLICT,
                    m.mapping().getSubject().getName() + " for " + m.unit().label()
                            + " was not scheduled: " + m.reason().getLabel()
                            + (m.teacherName() != null ? " (" + m.teacherName() + ")" : "")
                            + ". Assign a teacher in Teacher Allocation and regenerate to fill its "
                            + m.weeklyPeriods() + " period(s).",
                    m.unit().sectionId(), null, null, null, null));
        }

        List<SolutionValidator.ValidationConflict> fixedIssues = new ArrayList<>();
        List<Placement> fixedReservations = new ArrayList<>();
        int fixedCount = reserveFixedActivities(data, fixedReservations, fixedIssues);
        fixedCount += carryLockedLessons(version, data, requirements, fixedReservations, fixedIssues);
        fixedCount += reserveConsecutiveLessons(source, data, requirements, fixedReservations, fixedIssues);
        // A fixed academic lesson occupies one of the subject's weekly periods, so its
        // requirement must not be scheduled a second time.
        applyFixedAcademicWorkload(data, fixedReservations, requirements);

        return new Prepared(source, requirements, fixedIssues, fixedReservations, unscheduled,
                data.getUnits().size(), fixedCount);
    }

    private int reserveFixedActivities(TimetablePlanningData data, List<Placement> reservations,
                                       List<SolutionValidator.ValidationConflict> issues) {
        int count = 0;
        for (Unit unit : data.getUnits()) {
            List<FixedActivity> activities = data.fixedActivitiesFor(unit);
            for (FixedActivity activity : activities) {
                String subjectLabel = activity.getSubject() != null
                        ? activity.getSubject().getName() : activity.getName();
                Long subjectId = activity.getSubject() == null
                        ? null : activity.getSubject().getSubjectId();
                Long staffId = activity.getStaff() == null ? null : activity.getStaff().getStaffId();
                for (DayOfWeek day : activity.getDays()) {
                    if (!data.getScheduleByUnit().getOrDefault(unit.unitId(), Map.of()).containsKey(day)) continue;
                    List<TimetablePeriod> slots = data.slotsFor(unit.unitId(), day);
                    TimetablePeriod match = resolveFixedActivityPeriod(activity, unit, day, slots);
                    if (match == null) {
                        issues.add(new SolutionValidator.ValidationConflict(
                                TimetableConflictType.FIXED_ACTIVITY_CONFLICT,
                                "'" + subjectLabel + "' for " + unit.label() + " on " + day
                                        + " is not configured in the assigned schedule. "
                                        + "Choose a period available on that day or update the fixed activity.",
                                unit.sectionId(), subjectId, null, day, null));
                        continue;
                    }
                    boolean already = reservations.stream().anyMatch(p ->
                            Objects.equals(p.getUnitId(), unit.unitId())
                                    && p.getDayOfWeek() == day
                                    && Objects.equals(p.getPeriodId(), match.getTimetablePeriodId()));
                    if (already) continue;
                    if (staffId != null) {
                        boolean teacherBusy = reservations.stream().anyMatch(p ->
                                p.getDayOfWeek() == day
                                        && Objects.equals(p.getStaffId(), staffId)
                                        && Objects.equals(p.getPeriodId(), match.getTimetablePeriodId()));
                        if (teacherBusy) {
                            issues.add(new SolutionValidator.ValidationConflict(
                                    TimetableConflictType.TEACHER_CONFLICT,
                                    "'" + subjectLabel + "' for " + unit.label() + " on " + day + " ("
                                            + match.getName() + ") needs the assigned teacher, who is already "
                                            + "reserved at that time. Choose another period, day or teacher.",
                                    unit.sectionId(), subjectId, staffId, day, null));
                            continue;
                        }
                    }
                    Placement p = new Placement();
                    p.setUnitId(unit.unitId());
                    p.setSectionId(unit.sectionId());
                    p.setAcademicGroupId(unit.academicGroupId());
                    p.setFixedActivityId(activity.getFixedActivityId());
                    p.setDayOfWeek(day);
                    p.setPeriodId(match.getTimetablePeriodId());
                    p.setPeriodNumber(match.getPeriodNumber());
                    p.setStartTime(match.getStartTime());
                    p.setEndTime(match.getEndTime());
                    p.setTeachingIndex(slots.indexOf(match));
                    p.setEntryType(TimetableEntryType.ACTIVITY);
                    p.setSubjectId(subjectId);
                    p.setSubjectNameSnapshot(subjectLabel);
                    p.setStaffId(staffId);
                    p.setTeacherAllocationId(null);
                    reservations.add(p);
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Fixed academic lessons are real teaching periods for the mapped subject. The requirement
     * list still contains the full weekly frequency, so drop one requirement per reserved fixed
     * lesson instead of scheduling the same subject twice.
     */
    private void applyFixedAcademicWorkload(TimetablePlanningData data, List<Placement> reservations,
                                            List<SchedulingRequirement> requirements) {
        if (reservations.isEmpty() || requirements.isEmpty()) return;
        Map<Long, FixedActivity> activitiesById = new HashMap<>();
        for (FixedActivity activity : data.getFixedActivities()) {
            if (activity.getFixedActivityId() != null) {
                activitiesById.put(activity.getFixedActivityId(), activity);
            }
        }
        Map<String, Integer> reserved = new HashMap<>();
        for (Placement placement : reservations) {
            if (placement.getFixedActivityId() == null || placement.getUnitId() == null) continue;
            // Locked lessons carried from the source version already removed their requirement.
            if (placement.isLocked()) continue;
            FixedActivity activity = activitiesById.get(placement.getFixedActivityId());
            if (activity == null || activity.getSubject() == null) continue;
            SubjectType type = activity.getSubject().getSubjectType();
            if (type != SubjectType.ACADEMIC_SUBJECT && type != SubjectType.PRACTICAL_LAB) continue;
            reserved.merge(requirementKey(placement.getUnitId(),
                    activity.getSubject().getSubjectId()), 1, Integer::sum);
        }
        if (reserved.isEmpty()) return;
        for (int index = requirements.size() - 1; index >= 0; index--) {
            SchedulingRequirement requirement = requirements.get(index);
            String key = requirementKey(requirement.getUnitId(), requirement.getSubjectId());
            Integer remaining = reserved.get(key);
            if (remaining == null || remaining <= 0) continue;
            requirements.remove(index);
            reserved.put(key, remaining - 1);
        }
    }

    private static String requirementKey(Long unitId, Long subjectId) {
        return unitId + "|" + subjectId;
    }

    /**
     * Prefers the resolved {@code fixed_activity_period} row for this exact group/section/day,
     * then the configured period label, then the legacy time window. The chosen period must be
     * part of the unit's current schedule, so a changed pattern produces a visible conflict
     * instead of silently moving the activity.
     */
    private static TimetablePeriod resolveFixedActivityPeriod(FixedActivity activity, Unit unit,
                                                              DayOfWeek day, List<TimetablePeriod> slots) {
        FixedActivityPeriod resolved = activity.periodFor(
                unit.group().getAcademicGroupId(), unit.sectionId(), day);
        if (resolved != null && resolved.getTimetablePeriod() != null) {
            Long periodId = resolved.getTimetablePeriod().getTimetablePeriodId();
            return slots.stream()
                    .filter(slot -> slot.getSlotKind() == TimetableSlotKind.TEACHING)
                    .filter(slot -> Objects.equals(slot.getTimetablePeriodId(), periodId))
                    .findFirst().orElse(null);
        }
        if (activity.getPeriodName() != null) {
            TimetablePeriod byName = slots.stream()
                    .filter(slot -> slot.getSlotKind() == TimetableSlotKind.TEACHING)
                    .filter(slot -> slot.getName() != null
                            && slot.getName().equalsIgnoreCase(activity.getPeriodName()))
                    .findFirst().orElse(null);
            if (byName != null) return byName;
        }
        return slots.stream()
                .filter(slot -> slot.getSlotKind() == TimetableSlotKind.TEACHING)
                .filter(slot -> overlaps(slot, activity))
                .findFirst().orElse(null);
    }

    /**
     * Carries locked lessons from the source draft into the new version and removes the
     * matching scheduled requirement so the generator does not create a duplicate.
     */
    private int carryLockedLessons(TimetableVersion version, TimetablePlanningData data,
                                   List<SchedulingRequirement> requirements,
                                   List<Placement> reservations,
                                   List<SolutionValidator.ValidationConflict> issues) {
        if (version.getSourceVersionId() == null) return 0;
        List<TimetableEntry> locked = entryRepository.findGridEntries(version.getSourceVersionId())
                .stream().filter(e -> Boolean.TRUE.equals(e.getLocked())).toList();
        int carried = 0;
        for (TimetableEntry entry : locked) {
            Long unitId = entry.getSection() != null ? entry.getSection().getSectionId()
                    : entry.getAcademicGroup() != null ? -entry.getAcademicGroup().getAcademicGroupId() : null;
            if (unitId == null) continue;
            Unit unit = data.getUnits().stream().filter(u -> Objects.equals(u.unitId(), unitId))
                    .findFirst().orElse(null);
            if (unit == null) continue;
            TimetablePeriod period = entry.getTimetablePeriod();
            List<TimetablePeriod> slots = data.slotsFor(unitId, entry.getDayOfWeek());
            if (period == null || slots.stream().noneMatch(s -> Objects.equals(
                    s.getTimetablePeriodId(), period.getTimetablePeriodId()))) {
                issues.add(new SolutionValidator.ValidationConflict(
                        TimetableConflictType.FIXED_ACTIVITY_CONFLICT,
                        "Locked lesson for " + unit.label() + " on " + entry.getDayOfWeek()
                                + " no longer fits the assigned schedule and was not carried forward.",
                        unit.sectionId(), entry.getSubject() == null ? null : entry.getSubject().getSubjectId(),
                        entry.getStaff() == null ? null : entry.getStaff().getStaffId(), entry.getDayOfWeek(), null));
                continue;
            }
            Long mappingId = entry.getTeacherAllocation() == null
                    || entry.getTeacherAllocation().getAcademicGroupSubjectMapping() == null ? null
                    : entry.getTeacherAllocation().getAcademicGroupSubjectMapping()
                    .getAcademicGroupSubjectMappingId();
            Long subjectId = entry.getSubject() == null ? null : entry.getSubject().getSubjectId();
            SchedulingRequirement matched = requirements.stream()
                    .filter(r -> Objects.equals(r.getUnitId(), unitId))
                    .filter(r -> mappingId != null
                            ? Objects.equals(r.getAcademicGroupSubjectMappingId(), mappingId)
                            : subjectId == null || Objects.equals(r.getSubjectId(), subjectId))
                    .findFirst().orElse(null);
            if (matched == null) continue;
            boolean slotTaken = reservations.stream().anyMatch(p ->
                    Objects.equals(p.getUnitId(), unitId)
                            && p.getDayOfWeek() == entry.getDayOfWeek()
                            && Objects.equals(p.getPeriodId(), period.getTimetablePeriodId())
                            && !(matched.getChoiceMappingId() != null
                            && Objects.equals(p.getChoiceMappingId(), matched.getChoiceMappingId())
                            && p.getPeriodIndex() == matched.getPeriodIndex()
                            && !Objects.equals(p.getAcademicGroupSubjectMappingId(),
                            matched.getAcademicGroupSubjectMappingId())));
            if (slotTaken) continue;
            requirements.remove(matched);
            Placement p = new Placement();
            p.setRequirementId(matched.getRequirementId());
            p.setUnitId(unitId);
            p.setSectionId(entry.getSection() == null ? null : entry.getSection().getSectionId());
            p.setAcademicGroupId(unit.academicGroupId());
            p.setAcademicGroupSubjectMappingId(mappingId);
            p.setChoiceMappingId(matched.getChoiceMappingId());
            p.setPeriodIndex(matched.getPeriodIndex());
            p.setSubjectId(subjectId);
            p.setTeacherAllocationId(entry.getTeacherAllocation() == null ? null
                    : entry.getTeacherAllocation().getTeacherAllocationId());
            p.setStaffId(entry.getStaff() == null ? null : entry.getStaff().getStaffId());
            p.setResourceId(entry.getResource() == null ? null : entry.getResource().getAcademicResourceId());
            p.setFixedActivityId(entry.getFixedActivity() == null ? null
                    : entry.getFixedActivity().getFixedActivityId());
            p.setDayOfWeek(entry.getDayOfWeek());
            p.setPeriodId(period.getTimetablePeriodId());
            p.setPeriodNumber(period.getPeriodNumber());
            p.setStartTime(period.getStartTime());
            p.setEndTime(period.getEndTime());
            p.setTeachingIndex(slots.indexOf(period));
            p.setEntryType(entry.getEntryType());
            p.setSubjectNameSnapshot(entry.getSubjectNameSnapshot());
            p.setLocked(true);
            reservations.add(p);
            carried++;
        }
        return carried;
    }

    /**
     * Reserves contiguous blocks for curriculum mappings that require consecutive periods.
     * If no contiguous block exists, the lessons remain for the normal scheduler and a
     * warning is recorded instead of silently breaking the requirement.
     */
    private int reserveConsecutiveLessons(SchedulingSourceData source, TimetablePlanningData data,
                                          List<SchedulingRequirement> requirements,
                                          List<Placement> reservations,
                                          List<SolutionValidator.ValidationConflict> issues) {
        Map<String, List<SchedulingRequirement>> grouped = requirements.stream()
                .filter(r -> r.getConsecutivePeriods() > 1)
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getUnitId() + "|" + r.getAcademicGroupSubjectMappingId(),
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        int reserved = 0;
        for (List<SchedulingRequirement> group : grouped.values()) {
            group.sort(Comparator.comparingInt(SchedulingRequirement::getPeriodIndex));
            SchedulingRequirement first = group.get(0);
            int block = Math.max(1, first.getConsecutivePeriods());
            List<SchedulingRequirement> pending = new ArrayList<>(group);
            while (pending.size() >= block) {
                ScheduledBlock found = findBlock(source, data, previousBlockRequirements(pending, block),
                        reservations, first);
                if (found == null) break;
                for (int i = 0; i < block; i++) {
                    SchedulingRequirement req = pending.get(i);
                    TimetablePeriod period = found.slots().get(i);
                    CandidateSlot slot = new CandidateSlot(found.day(), req.getUnitId(), req.getSectionId(),
                            req.getAcademicGroupId(), period.getTimetablePeriodId(), period.getPeriodNumber(),
                            period.getStartTime(), period.getEndTime(), found.startIndex() + i,
                            req.getPrimaryStaffId(), req.getDefaultResourceId());
                    reservations.add(new Placement(req, slot));
                }
                pending.subList(0, block).clear();
                reserved += block;
            }
            if (pending.size() >= block) {
                issues.add(new SolutionValidator.ValidationConflict(
                        TimetableConflictType.PERIOD_CAPACITY_CONFLICT,
                        first.getSubjectName() + " for " + first.getSectionName() + " requires "
                                + block + " consecutive period(s), but no free block was available. "
                                + "Add capacity or reduce the consecutive-period requirement.",
                        first.getSectionId(), first.getSubjectId(), first.getPrimaryStaffId(), null, null));
            }
            requirements.removeIf(r -> group.stream().anyMatch(g -> g.getRequirementId()
                    .equals(r.getRequirementId()) && !pending.contains(g)));
        }
        return reserved;
    }

    private List<SchedulingRequirement> previousBlockRequirements(List<SchedulingRequirement> pending, int block) {
        return pending.subList(0, block);
    }

    private record ScheduledBlock(DayOfWeek day, int startIndex, List<TimetablePeriod> slots) {}

    private ScheduledBlock findBlock(SchedulingSourceData source, TimetablePlanningData data, List<SchedulingRequirement> reqs,
                                     List<Placement> reservations, SchedulingRequirement sample) {
        int block = reqs.size();
        for (DayOfWeek day : data.getWorkingDays()) {
            List<TimetablePeriod> slots = data.slotsFor(sample.getUnitId(), day);
            for (int start = 0; start + block <= slots.size(); start++) {
                boolean free = true;
                for (int i = 0; i < block && free; i++) {
                    TimetablePeriod period = slots.get(start + i);
                    SchedulingRequirement req = reqs.get(i);
                    free = reservations.stream()
                            .filter(p -> Objects.equals(p.getStaffId(), req.getPrimaryStaffId())).count()
                            + block <= source.getMaxTeacherWeeklyPeriods()
                            && reservations.stream().noneMatch(p ->
                            p.getDayOfWeek() == day
                                    && Objects.equals(p.getPeriodId(), period.getTimetablePeriodId())
                                    && (Objects.equals(p.getUnitId(), req.getUnitId())
                                    || (req.getPrimaryStaffId() != null
                                    && Objects.equals(p.getStaffId(), req.getPrimaryStaffId()))
                                    || (req.getDefaultResourceId() != null
                                    && Objects.equals(p.getResourceId(), req.getDefaultResourceId()))));
                }
                if (free) return new ScheduledBlock(day, start, slots.subList(start, start + block));
            }
        }
        return null;
    }

    private static boolean overlaps(TimetablePeriod slot, FixedActivity activity) {
        if (slot.getStartTime() == null || slot.getEndTime() == null) return false;
        return slot.getStartTime().isBefore(activity.getEndTime())
                && activity.getStartTime().isBefore(slot.getEndTime());
    }

    static int firstHalfTeachingCount(List<TimetablePeriod> allPeriods, int teachingCount) {
        TimetablePeriod longestBreak = null;
        long longest = -1;
        for (TimetablePeriod p : allPeriods) {
            if (p.getSlotKind() != TimetableSlotKind.BREAK || p.getStartTime() == null || p.getEndTime() == null) continue;
            long minutes = Duration.between(p.getStartTime(), p.getEndTime()).toMinutes();
            if (minutes > longest) {
                longest = minutes;
                longestBreak = p;
            }
        }
        if (longestBreak != null) {
            int before = 0;
            for (TimetablePeriod p : allPeriods) {
                if (p == longestBreak) break;
                if (p.getSlotKind() == TimetableSlotKind.TEACHING) before++;
            }
            if (before > 0 && before < teachingCount) return before;
        }
        return Math.max(1, teachingCount / 2);
    }

    private TimetableGenerateResultResponse persist(Long versionId,
                                                    SchedulingSolution solution,
                                                    SolutionValidator.ValidationResult validation,
                                                    Prepared prepared,
                                                    long elapsedMs) {
        TimetableVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));

        List<Placement> entries = new ArrayList<>(solution.getPlacements());
        insertEntries(versionId, entries);

        List<TimetableConflict> conflicts = new ArrayList<>();
        for (SolutionValidator.ValidationConflict vc : validation.getBlockingConflicts()) {
            conflicts.add(toConflictEntity(version, vc, true));
        }
        for (SolutionValidator.ValidationConflict vc : validation.getWarnings()) {
            conflicts.add(toConflictEntity(version, vc, false));
        }
        conflictRepository.saveAll(conflicts);

        long openBlocking = validation.getBlockingConflicts().size();
        long warnings = validation.getWarnings().size();

        version.setGenerationStatus(openBlocking > 0
                ? TimetableGenerationStatus.GENERATED_WITH_CONFLICTS
                : TimetableGenerationStatus.GENERATED);
        version.setStatus(TimetableStatus.READY_FOR_REVIEW);
        version.setGeneratedAt(LocalDateTime.now());
        versionRepository.save(version);

        TimetableGenerateResultResponse.ResultKind resultKind;
        if (openBlocking > 0) {
            resultKind = TimetableGenerateResultResponse.ResultKind.BLOCKED;
        } else if (warnings > 0) {
            resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS_WITH_WARNINGS;
        } else {
            resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS;
        }

        StringBuilder msg = new StringBuilder();
        if (openBlocking > 0) {
            msg.append("Timetable generated with ").append(openBlocking)
                    .append(" unresolved issue(s): ").append(entries.size()).append(" periods placed");
        } else {
            msg.append("Timetable generated: ").append(entries.size()).append(" periods placed across ")
                    .append(prepared.unitCount()).append(" group/section unit(s) with no clashes");
        }
        if (prepared.fixedActivityCount() > 0) {
            msg.append("; ").append(prepared.fixedActivityCount()).append(" fixed activity reservation(s)");
        }
        if (!prepared.unscheduled().isEmpty()) {
            msg.append("; ").append(prepared.unscheduled().size())
                    .append(" subject(s) skipped because no teacher is assigned");
        }
        msg.append(" (").append(Math.max(1, elapsedMs / 1000)).append("s)");

        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(version.getTimetableVersionId())
                .versionNumber(version.getVersionNumber())
                .generationStatus(version.getGenerationStatus())
                .totalEntries(entries.size())
                .totalPlacements(entries.size())
                .totalConflicts(conflicts.size())
                .openBlockingConflicts(openBlocking)
                .message(msg.toString())
                .algorithmVersion(ALGORITHM_VERSION)
                .resultKind(resultKind)
                .unplacedLessons(solution.getUnplacedRequirements().stream()
                        .map(r -> r.getSubjectName() + " · " + r.getSectionName())
                        .distinct().toList())
                .warnings(validation.getWarnings().stream()
                        .map(SolutionValidator.ValidationConflict::getMessage).toList())
                .build();
    }

    private void insertEntries(Long versionId, List<Placement> placements) {
        if (placements.isEmpty()) return;
        String sql = "INSERT INTO timetable_entry (created_by, created_on, updated_by, updated_on, version, "
                + "day_of_week, entry_type, resource_id, section_id, academic_group_id, subject_id, staff_id, "
                + "fixed_activity_id, subject_name_snapshot, teacher_allocation_id, timetable_period_id, "
                + "timetable_version_id, academic_group_choice_mapping_id, is_locked) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int pending = 0;
                for (Placement p : placements) {
                    ps.setString(1, SYSTEM_USER);
                    ps.setObject(2, now);
                    ps.setString(3, SYSTEM_USER);
                    ps.setObject(4, now);
                    ps.setString(5, p.getDayOfWeek().name());
                    ps.setString(6, p.getEntryType().name());
                    setNullableLong(ps, 7, p.getResourceId());
                    setNullableLong(ps, 8, p.getSectionId());
                    setNullableLong(ps, 9, p.getAcademicGroupId());
                    setNullableLong(ps, 10, p.getSubjectId());
                    setNullableLong(ps, 11, p.getStaffId());
                    setNullableLong(ps, 12, p.getFixedActivityId());
                    ps.setString(13, p.getSubjectNameSnapshot());
                    setNullableLong(ps, 14, p.getTeacherAllocationId());
                    ps.setLong(15, p.getPeriodId());
                    ps.setLong(16, versionId);
                    setNullableLong(ps, 17, p.getChoiceMappingId());
                    ps.setBoolean(18, p.isLocked());
                    ps.addBatch();
                    if (++pending == INSERT_BATCH_SIZE) {
                        ps.executeBatch();
                        pending = 0;
                    }
                }
                if (pending > 0) ps.executeBatch();
            }
        });
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws java.sql.SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private TimetableConflict toConflictEntity(TimetableVersion version,
                                               SolutionValidator.ValidationConflict vc,
                                               boolean blocking) {
        TimetableConflict A = new TimetableConflict();
        A.setTimetableVersion(version);
        A.setConflictType(vc.getConflictType());
        A.setBlocking(blocking);
        A.setStatus(TimetableConflictStatus.OPEN);
        String message = vc.getMessage();
        A.setMessage(message.length() > 1000 ? message.substring(0, 997) + "..." : message);
        if (vc.getSectionId() != null) {
            A.setSection(sectionRepository.getReferenceById(vc.getSectionId()));
        }
        if (vc.getTeacherAllocationId() != null) {
            A.setTeacherAllocation(allocationRepository.getReferenceById(vc.getTeacherAllocationId()));
        }
        if (vc.getResourceId() != null) {
            A.setResource(resourceRepository.getReferenceById(vc.getResourceId()));
        }
        A.setDayOfWeek(vc.getDayOfWeek());
        if (vc.getPeriodId() != null) {
            A.setTimetablePeriod(periodRepository.getReferenceById(vc.getPeriodId()));
        }
        return A;
    }

    private TimetableGenerateResultResponse cancelled(TimetableVersion version,
                                                      GenerationProgress progress) {
        version.setGenerationStatus(TimetableGenerationStatus.FAILED);
        version.setStatus(TimetableStatus.DRAFT);
        versionRepository.save(version);

        progress.complete(TimetableGenerationStatus.FAILED, "Generation cancelled by user");
        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(version.getTimetableVersionId())
                .versionNumber(version.getVersionNumber())
                .generationStatus(TimetableGenerationStatus.FAILED)
                .totalEntries(0).totalConflicts(0).openBlockingConflicts(0)
                .message("Generation cancelled by user")
                .algorithmVersion(ALGORITHM_VERSION)
                .resultKind(TimetableGenerateResultResponse.ResultKind.FAILED)
                .build();
    }

    private TimetableGenerateResultResponse handleFailure(Long versionId, Exception e,
                                                          GenerationProgress progress) {
        try {
            TimetableVersion version = versionRepository.findById(versionId).orElse(null);
            if (version != null) {
                version.setGenerationStatus(TimetableGenerationStatus.FAILED);
                version.setStatus(TimetableStatus.DRAFT);
                versionRepository.save(version);
            }
        } catch (Exception ex) {
            log.error("Failed to update version status on failure", ex);
        }

        String msg = "Generation failed: " + e.getMessage();
        progress.complete(TimetableGenerationStatus.FAILED, msg);

        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(versionId)
                .generationStatus(TimetableGenerationStatus.FAILED)
                .totalEntries(0).totalConflicts(0).openBlockingConflicts(0)
                .message(msg)
                .algorithmVersion(ALGORITHM_VERSION)
                .resultKind(TimetableGenerateResultResponse.ResultKind.FAILED)
                .build();
    }
}
