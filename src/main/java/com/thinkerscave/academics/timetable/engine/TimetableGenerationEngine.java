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

        return new Prepared(source, requirements, fixedIssues, fixedReservations, unscheduled,
                data.getUnits().size(), fixedCount);
    }

    private int reserveFixedActivities(TimetablePlanningData data, List<Placement> reservations,
                                       List<SolutionValidator.ValidationConflict> issues) {
        int count = 0;
        for (Unit unit : data.getUnits()) {
            if (unit.section() == null) continue;
            List<FixedActivity> activities = data.fixedActivitiesFor(unit);
            for (FixedActivity activity : activities) {
                for (DayOfWeek day : activity.getDays()) {
                    if (!data.getScheduleByUnit().getOrDefault(unit.unitId(), Map.of()).containsKey(day)) continue;
                    List<TimetablePeriod> slots = data.slotsFor(unit.unitId(), day);
                    TimetablePeriod match = slots.stream()
                            .filter(slot -> overlaps(slot, activity))
                            .findFirst().orElse(null);
                    if (match == null) {
                        issues.add(new SolutionValidator.ValidationConflict(
                                TimetableConflictType.FIXED_ACTIVITY_CONFLICT,
                                "'" + activity.getName() + "' for " + unit.label() + " on " + day
                                        + " (" + activity.getStartTime() + "–" + activity.getEndTime()
                                        + ") does not fit any teaching slot in the assigned schedule. "
                                        + "Adjust the activity time or the group's schedule pattern.",
                                unit.sectionId(), null, null, day, null));
                        continue;
                    }
                    boolean already = reservations.stream().anyMatch(p ->
                            Objects.equals(p.getUnitId(), unit.unitId())
                                    && p.getDayOfWeek() == day
                                    && Objects.equals(p.getPeriodId(), match.getTimetablePeriodId()));
                    if (already) continue;
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
                    p.setSubjectNameSnapshot(activity.getName());
                    p.setStaffId(null);
                    p.setTeacherAllocationId(null);
                    reservations.add(p);
                    count++;
                }
            }
        }
        return count;
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
                + "timetable_version_id, is_locked) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                    ps.setBoolean(17, p.isLocked());
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
