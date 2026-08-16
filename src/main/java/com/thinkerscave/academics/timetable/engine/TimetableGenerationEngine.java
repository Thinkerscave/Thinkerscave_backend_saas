package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.dto.response.TimetableGenerateResultResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TimetableGenerationEngine {

    private static final Logger log = LoggerFactory.getLogger(TimetableGenerationEngine.class);
    public static final String ALGORITHM_VERSION = "v1";

    private final RequirementExpander expander;
    private final CandidateCalculator candidateCalculator;
    private final ConstraintScheduler scheduler;
    private final SoftConstraintOptimizer optimizer;
    private final SolutionValidator validator;
    private final TimetableVersionRepository versionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetableConflictRepository conflictRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableWorkingDayRepository workingDayRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final AcademicResourceRepository resourceRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final TransactionTemplate transactionTemplate;

    public TimetableGenerateResultResponse execute(Long versionId, long seed,
                                                   GenerationProgress progress) {
        log.info("Engine start: versionId={}, seed={}, algorithm={}", versionId, seed, ALGORITHM_VERSION);

        try {
            // Phase 2: Load version
            progress.advanceTo(GenerationPhase.PREPARING_WORKSPACE, 5);
            TimetableVersion version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));

            if (progress.isCancelRequested()) return cancelled(version, progress);

            // Phase 3–4: Load source data + expand requirements inside one read transaction
            // (async worker otherwise hits LazyInitializationException on associations).
            progress.advanceTo(GenerationPhase.LOADING_DATA, 10);
            record Prepared(SchedulingSourceData source, List<SchedulingRequirement> requirements) {}
            Prepared prepared = transactionTemplate.execute(status -> {
                SchedulingSourceData loaded = loadSourceData(versionId);
                progress.advanceTo(GenerationPhase.BUILDING_REQUIREMENTS, 20);
                List<SchedulingRequirement> reqs = expander.expand(loaded);
                return new Prepared(loaded, reqs);
            });
            if (prepared == null) {
                throw new IllegalStateException("Failed to prepare scheduling data for version " + versionId);
            }
            SchedulingSourceData source = prepared.source();
            List<SchedulingRequirement> requirements = prepared.requirements();
            log.info("Expanded {} scheduling requirements", requirements.size());

            if (progress.isCancelRequested()) return cancelled(version, progress);

            if (requirements.isEmpty()) {
                return completeEmpty(version, progress);
            }

            if (progress.isCancelRequested()) return cancelled(version, progress);

            // Phase 5: Calculate candidates
            progress.advanceTo(GenerationPhase.CALCULATING_CANDIDATES, 30);
            candidateCalculator.calculateCandidates(requirements, source);

            List<SchedulingRequirement> zeroCandidates = requirements.stream()
                    .filter(r -> r.getCandidates().isEmpty())
                    .toList();
            if (!zeroCandidates.isEmpty()) {
                return completeBlocked(version, zeroCandidates, progress);
            }

            if (progress.isCancelRequested()) return cancelled(version, progress);

            // Phase 6: Schedule (CPU-bound, outside long DB transaction)
            progress.advanceTo(GenerationPhase.SCHEDULING, 40);
            SchedulingSolution solution = scheduler.solve(requirements, source, seed, progress);

            if (progress.isCancelRequested()) return cancelled(version, progress);

            // Phase 7: Optimize
            if (solution.isComplete()) {
                progress.advanceTo(GenerationPhase.OPTIMIZING, 72);
                solution = optimizer.optimize(solution, source, progress);
            }

            if (progress.isCancelRequested()) return cancelled(version, progress);

            // Phase 8: Validate
            progress.advanceTo(GenerationPhase.VALIDATING, 82);
            SolutionValidator.ValidationResult validation =
                    validator.validate(solution, requirements, source);

            // Phase 9: Generate conflicts
            progress.advanceTo(GenerationPhase.GENERATING_CONFLICTS, 90);

            // Phase 10: Persist in a short transaction
            progress.advanceTo(GenerationPhase.FINALIZING, 95);
            SchedulingSolution finalSolution = solution;
            return transactionTemplate.execute(status ->
                    persist(version, finalSolution, validation, progress));

        } catch (Exception e) {
            log.error("Engine failed for versionId={}", versionId, e);
            return handleFailure(versionId, e, progress);
        }
    }

    private SchedulingSourceData loadSourceData(Long versionId) {
        TimetableVersion version = versionRepository.findByIdWithConfigAndYear(versionId)
                .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));
        TimetableConfiguration config = version.getTimetableConfiguration();
        Long configId = config.getTimetableConfigurationId();
        Long yearId = version.getAcademicYear().getAcademicYearId();

        List<TimetableWorkingDay> workingDays = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(configId)
                .stream()
                .filter(w -> Boolean.TRUE.equals(w.getWorking()))
                .toList();
        List<DayOfWeek> days = workingDays.stream()
                .map(TimetableWorkingDay::getDayOfWeek).toList();

        List<TimetablePeriod> allPeriods = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId);
        List<TimetablePeriod> teachingPeriods = allPeriods.stream()
                .filter(p -> p.getSlotKind() == TimetableSlotKind.TEACHING).toList();

        int halfIndex = teachingPeriods.size() / 2;

        List<AcademicClass> classes = classRepository
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(yearId);
        List<AcademicSection> sections = new ArrayList<>();
        for (AcademicClass c : classes) {
            sections.addAll(sectionRepository
                    .findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(c.getClassId()));
        }

        List<TeacherAllocationTeacher> primaries = allocationTeacherRepository.findActiveByYear(yearId)
                .stream()
                .filter(t -> t.getRole() == TeacherAllocationTeacherRole.PRIMARY
                        && t.getTeacherAllocation().getStatus() == TeacherAllocationStatus.ASSIGNED)
                .toList();

        return SchedulingSourceData.builder()
                .configuration(config)
                .workingDays(days)
                .teachingPeriods(teachingPeriods)
                .allPeriods(allPeriods)
                .activeSections(sections)
                .activePrimaries(primaries)
                .maxTeacherWeeklyPeriods(config.getMaxTeacherWeeklyPeriods())
                .halfIndex(halfIndex)
                .build();
    }

    private TimetableGenerateResultResponse persist(TimetableVersion version,
                                                      SchedulingSolution solution,
                                                      SolutionValidator.ValidationResult validation,
                                                      GenerationProgress progress) {
        List<TimetableEntry> entries = new ArrayList<>();
        for (Placement p : solution.getPlacements()) {
            TimetableEntry entry = new TimetableEntry();
            entry.setTimetableVersion(version);
            entry.setDayOfWeek(p.getDayOfWeek());
            entry.setTimetablePeriod(periodRepository.getReferenceById(p.getPeriodId()));
            entry.setSection(sectionRepository.getReferenceById(p.getSectionId()));
            entry.setTeacherAllocation(allocationRepository.getReferenceById(p.getTeacherAllocationId()));
            entry.setEntryType(TimetableEntryType.SUBJECT);
            entry.setSubjectNameSnapshot(p.getSubjectNameSnapshot());
            if (p.getResourceId() != null) {
                entry.setResource(resourceRepository.getReferenceById(p.getResourceId()));
            }
            entries.add(entry);
        }

        List<TimetableConflict> conflicts = new ArrayList<>();
        for (SolutionValidator.ValidationConflict vc : validation.getBlockingConflicts()) {
            conflicts.add(toConflictEntity(version, vc, true));
        }
        for (SolutionValidator.ValidationConflict vc : validation.getWarnings()) {
            conflicts.add(toConflictEntity(version, vc, false));
        }

        entryRepository.saveAll(entries);
        conflictRepository.saveAll(conflicts);

        long openBlocking = conflicts.stream()
                .filter(c -> Boolean.TRUE.equals(c.getBlocking())
                        && c.getStatus() == TimetableConflictStatus.OPEN)
                .count();

        boolean hasBlockingConflicts = openBlocking > 0;
        boolean hasWarnings = !validation.getWarnings().isEmpty();

        version.setGenerationStatus(hasBlockingConflicts
                ? TimetableGenerationStatus.GENERATED_WITH_CONFLICTS
                : TimetableGenerationStatus.GENERATED);
        version.setStatus(TimetableStatus.READY_FOR_REVIEW);
        version.setGeneratedAt(LocalDateTime.now());
        versionRepository.save(version);

        TimetableGenerateResultResponse.ResultKind resultKind;
        if (hasBlockingConflicts) {
            resultKind = TimetableGenerateResultResponse.ResultKind.BLOCKED;
        } else if (hasWarnings) {
            resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS_WITH_WARNINGS;
        } else {
            resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS;
        }

        String msg = hasBlockingConflicts
                ? "Timetable generated with " + openBlocking + " blocking conflict(s)"
                : "Timetable generated successfully";

        progress.complete(version.getGenerationStatus(), msg);

        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(version.getTimetableVersionId())
                .versionNumber(version.getVersionNumber())
                .generationStatus(version.getGenerationStatus())
                .totalEntries(entries.size())
                .totalConflicts(conflicts.size())
                .openBlockingConflicts(openBlocking)
                .message(msg)
                .algorithmVersion(ALGORITHM_VERSION)
                .resultKind(resultKind)
                .build();
    }

    private TimetableConflict toConflictEntity(TimetableVersion version,
                                               SolutionValidator.ValidationConflict vc,
                                               boolean blocking) {
        TimetableConflict c = new TimetableConflict();
        c.setTimetableVersion(version);
        c.setConflictType(vc.getConflictType());
        c.setBlocking(blocking);
        c.setStatus(TimetableConflictStatus.OPEN);
        c.setMessage(vc.getMessage());
        if (vc.getSectionId() != null) {
            c.setSection(sectionRepository.getReferenceById(vc.getSectionId()));
        }
        c.setDayOfWeek(vc.getDayOfWeek());
        if (vc.getPeriodId() != null) {
            c.setTimetablePeriod(periodRepository.getReferenceById(vc.getPeriodId()));
        }
        return c;
    }

    private TimetableGenerateResultResponse completeEmpty(TimetableVersion version,
                                                          GenerationProgress progress) {
        version.setGenerationStatus(TimetableGenerationStatus.GENERATED);
        version.setStatus(TimetableStatus.READY_FOR_REVIEW);
        version.setGeneratedAt(LocalDateTime.now());
        versionRepository.save(version);

        progress.complete(TimetableGenerationStatus.GENERATED, "No requirements to schedule");
        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(version.getTimetableVersionId())
                .versionNumber(version.getVersionNumber())
                .generationStatus(TimetableGenerationStatus.GENERATED)
                .totalEntries(0).totalConflicts(0).openBlockingConflicts(0)
                .message("No requirements to schedule")
                .algorithmVersion(ALGORITHM_VERSION)
                .resultKind(TimetableGenerateResultResponse.ResultKind.SUCCESS)
                .build();
    }

    private TimetableGenerateResultResponse completeBlocked(TimetableVersion version,
                                                            List<SchedulingRequirement> zeroReqs,
                                                            GenerationProgress progress) {
        List<TimetableConflict> conflicts = new ArrayList<>();
        for (SchedulingRequirement r : zeroReqs) {
            TimetableConflict c = new TimetableConflict();
            c.setTimetableVersion(version);
            c.setConflictType(TimetableConflictType.SUBJECT_ALLOCATION_CONFLICT);
            c.setBlocking(true);
            c.setStatus(TimetableConflictStatus.OPEN);
            c.setMessage("No candidate slots for " + r.getSubjectName()
                    + " in " + r.getSectionName());
            if (r.getSectionId() != null) {
                c.setSection(sectionRepository.getReferenceById(r.getSectionId()));
            }
            conflicts.add(c);
        }
        conflictRepository.saveAll(conflicts);

        version.setGenerationStatus(TimetableGenerationStatus.GENERATED_WITH_CONFLICTS);
        version.setStatus(TimetableStatus.READY_FOR_REVIEW);
        version.setGeneratedAt(LocalDateTime.now());
        versionRepository.save(version);

        progress.complete(TimetableGenerationStatus.GENERATED_WITH_CONFLICTS,
                zeroReqs.size() + " requirement(s) have no viable candidates");

        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(version.getTimetableVersionId())
                .versionNumber(version.getVersionNumber())
                .generationStatus(TimetableGenerationStatus.GENERATED_WITH_CONFLICTS)
                .totalEntries(0).totalConflicts(conflicts.size())
                .openBlockingConflicts(conflicts.size())
                .message(zeroReqs.size() + " requirement(s) have no viable candidates")
                .algorithmVersion(ALGORITHM_VERSION)
                .resultKind(TimetableGenerateResultResponse.ResultKind.BLOCKED)
                .build();
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
