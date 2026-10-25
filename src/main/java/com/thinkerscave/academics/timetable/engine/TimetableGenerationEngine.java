package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.dto.response.TimetableGenerateResultResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.MissingTeacher;
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
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TimetableGenerationEngine {

    private static final Logger log = LoggerFactory.getLogger(TimetableGenerationEngine.class);
    public static final String ALGORITHM_VERSION = "v2";
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
                            List<SolutionValidator.ValidationConflict> unscheduled,
                            int sectionCount) {}

    public TimetableGenerateResultResponse execute(Long versionId, long seed,
                                                   GenerationProgress progress) {
        log.info("Engine start: versionId={}, seed={}, algorithm={}", versionId, seed, ALGORITHM_VERSION);
        long startedAt = System.nanoTime();

        try {
            progress.advanceTo(GenerationPhase.PREPARING_WORKSPACE, 5);
            TimetableVersion version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));

            if (progress.isCancelRequested()) return cancelled(version, progress);

            progress.advanceTo(GenerationPhase.LOADING_DATA, 10);
            Prepared prepared = transactionTemplate.execute(status -> prepare(versionId, progress));
            if (prepared == null) {
                throw new IllegalStateException("Failed to prepare scheduling data for version " + versionId);
            }
            SchedulingSourceData source = prepared.source();
            List<SchedulingRequirement> requirements = prepared.requirements();
            log.info("Expanded {} scheduling requirements, {} unscheduled lesson(s) without teacher",
                    requirements.size(), prepared.unscheduled().size());

            if (progress.isCancelRequested()) return cancelled(version, progress);

            if (source.getWorkingDays().isEmpty() || source.getTeachingPeriods().isEmpty()) {
                throw new IllegalStateException("The configuration has no working days or teaching periods");
            }

            SolutionValidator.ValidationResult validation;
            SchedulingSolution solution;
            if (requirements.isEmpty()) {
                solution = new SchedulingSolution();
                validation = new SolutionValidator.ValidationResult();
            } else {
                progress.advanceTo(GenerationPhase.CALCULATING_CANDIDATES, 30);
                candidateCalculator.calculateCandidates(requirements, source);

                if (progress.isCancelRequested()) return cancelled(version, progress);

                progress.advanceTo(GenerationPhase.SCHEDULING, 40);
                solution = scheduler.solve(requirements, source, seed, progress);

                if (progress.isCancelRequested()) return cancelled(version, progress);

                Map<String, SchedulingRequirement> byId = new HashMap<>();
                for (SchedulingRequirement r : requirements) byId.put(r.getRequirementId(), r);
                progress.advanceTo(GenerationPhase.OPTIMIZING, 72);
                solution = optimizer.optimize(solution, source, progress, byId);

                if (progress.isCancelRequested()) return cancelled(version, progress);

                progress.advanceTo(GenerationPhase.VALIDATING, 82);
                validation = validator.validate(solution, requirements, source);
            }
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

    private Prepared prepare(Long versionId, GenerationProgress progress) {
        TimetableVersion version = versionRepository.findByIdWithConfigAndYear(versionId)
                .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));
        TimetableConfiguration config = version.getTimetableConfiguration();
        TimetablePlanningData data = planningDataLoader.load(
                version.getAcademicYear().getAcademicYearId(), config);

        SchedulingSourceData source = SchedulingSourceData.builder()
                .configuration(config)
                .workingDays(data.getWorkingDays())
                .teachingPeriods(data.getTeachingPeriods())
                .allPeriods(data.getAllPeriods())
                .maxTeacherWeeklyPeriods(config.getMaxTeacherWeeklyPeriods() == null
                        ? Integer.MAX_VALUE : config.getMaxTeacherWeeklyPeriods())
                .halfIndex(firstHalfTeachingCount(data.getAllPeriods(), data.getTeachingPeriods().size()))
                .build();

        progress.advanceTo(GenerationPhase.BUILDING_REQUIREMENTS, 20);
        List<SchedulingRequirement> requirements = expander.expand(data.getLessons());

        List<SolutionValidator.ValidationConflict> unscheduled = new ArrayList<>();
        for (MissingTeacher m : data.getMissingTeachers()) {
            unscheduled.add(new SolutionValidator.ValidationConflict(
                    TimetableConflictType.SUBJECT_ALLOCATION_CONFLICT,
                    m.mapping().getSubject().getName() + " for " + TimetablePlanningData.sectionLabel(m.section())
                            + " was not scheduled: " + m.reason().getLabel()
                            + (m.teacherName() != null ? " (" + m.teacherName() + ")" : "")
                            + ". Assign a teacher in Teacher Allocation and regenerate to fill its "
                            + m.weeklyPeriods() + " period(s).",
                    m.section().getSectionId(), null, null, null, null));
        }
        return new Prepared(source, requirements, unscheduled, data.getSections().size());
    }

    /**
     * Teaching periods before the longest break (usually lunch); falls back to half the day
     * when there is no break splitting the teaching periods.
     */
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
                    .append(prepared.sectionCount()).append(" section(s) with no clashes");
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
                .totalConflicts(conflicts.size())
                .openBlockingConflicts(openBlocking)
                .message(msg.toString())
                .algorithmVersion(ALGORITHM_VERSION)
                .resultKind(resultKind)
                .build();
    }

    /**
     * Entries use IDENTITY keys, which disables Hibernate insert batching; a school timetable has
     * hundreds of rows, so they are written as one JDBC batch on the session's tenant connection.
     */
    private void insertEntries(Long versionId, List<Placement> placements) {
        if (placements.isEmpty()) return;
        String sql = "INSERT INTO timetable_entry (created_by, created_on, updated_by, updated_on, version, "
                + "day_of_week, entry_type, resource_id, section_id, subject_name_snapshot, "
                + "teacher_allocation_id, timetable_period_id, timetable_version_id) "
                + "VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                    ps.setString(6, TimetableEntryType.SUBJECT.name());
                    if (p.getResourceId() == null) {
                        ps.setNull(7, Types.BIGINT);
                    } else {
                        ps.setLong(7, p.getResourceId());
                    }
                    ps.setLong(8, p.getSectionId());
                    ps.setString(9, p.getSubjectNameSnapshot());
                    ps.setLong(10, p.getTeacherAllocationId());
                    ps.setLong(11, p.getPeriodId());
                    ps.setLong(12, versionId);
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
