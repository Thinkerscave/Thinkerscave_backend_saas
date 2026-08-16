package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicResourceRequest;
import com.thinkerscave.academics.dto.request.TimetableConfigurationRequest;
import com.thinkerscave.academics.dto.request.TimetableGenerationStartRequest;
import com.thinkerscave.academics.dto.request.TimetablePeriodRequest;
import com.thinkerscave.academics.dto.request.TimetableWorkingDayRequest;
import com.thinkerscave.academics.dto.response.*;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TimetableService;
import com.thinkerscave.academics.timetable.engine.GenerationPhase;
import com.thinkerscave.academics.timetable.engine.GenerationProgress;
import com.thinkerscave.academics.timetable.engine.GenerationProgressTracker;
import com.thinkerscave.academics.timetable.engine.TimetableGenerationEngine;
import com.thinkerscave.academics.timetable.engine.TimetableGenerationWorker;
import com.thinkerscave.academics.timetable.engine.TimetableReadinessEvaluator;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableServiceImpl implements TimetableService {

    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED,
            AcademicYearStatus.ARCHIVED
    );

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final ClassSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableWorkingDayRepository workingDayRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetableConflictRepository conflictRepository;
    private final AcademicResourceRepository resourceRepository;
    private final AcademicsAccessGuard accessGuard;
    private final TimetableReadinessEvaluator readinessEvaluator;
    private final GenerationProgressTracker progressTracker;
    private final TimetableGenerationWorker generationWorker;

    // ─── Dashboard ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableDashboardResponse getDashboard(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);

        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(yearId);
        List<TimetableDashboardResponse.ReadinessItem> checks = readiness.getChecks().stream()
                .map(c -> TimetableDashboardResponse.ReadinessItem.builder()
                        .key(c.getCode())
                        .label(c.getCode())
                        .status(c.getStatus() == TimetableReadinessResponse.ReadinessStatus.PASSED ? "PASS"
                                : c.getStatus() == TimetableReadinessResponse.ReadinessStatus.WARNING ? "WARN" : "FAIL")
                        .message(c.getMessage())
                        .blocking(c.getSeverity() == TimetableReadinessResponse.ReadinessSeverity.BLOCKING)
                        .build())
                .toList();

        String overallStatus = readiness.isReady() ? "READY" : "BLOCKED";
        boolean canGenerate = readiness.isReady() && !isYearReadOnly(year);

        TimetableConfiguration config = findActiveConfig(yearId);
        TimetableConfigurationResponse configSummary = config == null ? null : toConfigResponse(config);

        List<TimetableVersion> versions = versionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId);

        TimetableVersion published = versions.stream()
                .filter(v -> v.getStatus() == TimetableStatus.PUBLISHED)
                .findFirst().orElse(null);
        TimetableVersion latest = versions.isEmpty() ? null : versions.get(0);

        long totalConflicts = 0;
        long openBlocking = 0;
        if (latest != null) {
            List<TimetableConflict> conflicts = conflictRepository
                    .findByTimetableVersion_TimetableVersionId(latest.getTimetableVersionId());
            totalConflicts = conflicts.size();
            openBlocking = conflicts.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getBlocking())
                            && c.getStatus() == TimetableConflictStatus.OPEN)
                    .count();
        }

        return TimetableDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .readinessChecks(checks)
                .overallStatus(overallStatus)
                .canGenerate(canGenerate)
                .configurationSummary(configSummary)
                .currentVersion(published == null ? null : toVersionResponse(published))
                .latestVersion(latest == null ? null : toVersionResponse(latest))
                .totalConflicts(totalConflicts)
                .openBlockingConflicts(openBlocking)
                .build();
    }


    // ─── Configuration ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableConfigurationResponse getConfiguration(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        TimetableConfiguration config = findActiveConfig(yearId);
        return config == null ? null : toConfigResponse(config);
    }

    @Override
    public TimetableConfigurationResponse upsertConfiguration(Long yearId, TimetableConfigurationRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        assertYearMutable(year);

        TimetableShiftType shift = request.getShiftType() == null
                ? TimetableShiftType.REGULAR : request.getShiftType();

        TimetableConfiguration config = configurationRepository
                .findByAcademicYear_AcademicYearIdAndShiftType(yearId, shift)
                .orElse(null);

        if (config != null && Boolean.TRUE.equals(config.getIsLocked())) {
            throw new BusinessException("Configuration is locked and cannot be modified");
        }

        if (config == null) {
            config = new TimetableConfiguration();
            config.setAcademicYear(year);
            config.setShiftType(shift);
        }

        config.setName(request.getName());
        config.setSchoolStartTime(request.getSchoolStartTime());
        config.setSchoolEndTime(request.getSchoolEndTime());
        config.setDefaultPeriodDurationMin(request.getDefaultPeriodDurationMin());
        config.setMaxTeacherWeeklyPeriods(request.getMaxTeacherWeeklyPeriods());
        config.setActive(true);

        config = configurationRepository.save(config);
        Long configId = config.getTimetableConfigurationId();

        // Replace working days
        if (request.getWorkingDays() != null) {
            List<TimetableWorkingDay> existing = workingDayRepository
                    .findByTimetableConfiguration_TimetableConfigurationId(configId);
            Set<DayOfWeek> requestedDays = request.getWorkingDays().stream()
                    .map(TimetableWorkingDayRequest::getDayOfWeek)
                    .collect(Collectors.toSet());

            existing.stream()
                    .filter(wd -> !requestedDays.contains(wd.getDayOfWeek()))
                    .forEach(workingDayRepository::delete);

            for (TimetableWorkingDayRequest wdReq : request.getWorkingDays()) {
                TimetableWorkingDay wd = workingDayRepository
                        .findByTimetableConfiguration_TimetableConfigurationIdAndDayOfWeek(
                                configId, wdReq.getDayOfWeek())
                        .orElseGet(() -> {
                            TimetableWorkingDay n = new TimetableWorkingDay();
                            n.setTimetableConfiguration(configurationRepository.getReferenceById(configId));
                            n.setDayOfWeek(wdReq.getDayOfWeek());
                            return n;
                        });
                wd.setWorking(wdReq.getWorking());
                workingDayRepository.save(wd);
            }
        }

        // Replace periods
        if (request.getPeriods() != null) {
            List<TimetablePeriod> existingPeriods = periodRepository
                    .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId);
            Set<Short> requestedNumbers = request.getPeriods().stream()
                    .map(TimetablePeriodRequest::getPeriodNumber)
                    .collect(Collectors.toSet());

            existingPeriods.stream()
                    .filter(p -> !requestedNumbers.contains(p.getPeriodNumber()))
                    .forEach(periodRepository::delete);

            for (TimetablePeriodRequest pReq : request.getPeriods()) {
                TimetablePeriod period = existingPeriods.stream()
                        .filter(p -> p.getPeriodNumber().equals(pReq.getPeriodNumber()))
                        .findFirst()
                        .orElseGet(() -> {
                            TimetablePeriod n = new TimetablePeriod();
                            n.setTimetableConfiguration(configurationRepository.getReferenceById(configId));
                            return n;
                        });
                period.setPeriodNumber(pReq.getPeriodNumber());
                period.setName(pReq.getName());
                period.setStartTime(pReq.getStartTime());
                period.setEndTime(pReq.getEndTime());
                period.setSlotKind(pReq.getSlotKind());
                periodRepository.save(period);
            }
        }

        // Recompute status
        List<TimetableWorkingDay> wds = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(configId);
        boolean hasWorkingDay = wds.stream().anyMatch(w -> Boolean.TRUE.equals(w.getWorking()));
        List<TimetablePeriod> periods = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId);
        boolean hasTeaching = periods.stream().anyMatch(p -> p.getSlotKind() == TimetableSlotKind.TEACHING);
        config.setStatus(hasWorkingDay && hasTeaching
                ? TimetableConfigurationStatus.READY
                : TimetableConfigurationStatus.INCOMPLETE);
        configurationRepository.save(config);

        return toConfigResponse(config);
    }

    // ─── Resources ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AcademicResourceResponse> listResources() {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        return resourceRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResourceResponse)
                .toList();
    }

    @Override
    public AcademicResourceResponse createResource(AcademicResourceRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        if (resourceRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BusinessException("Resource code already exists: " + request.getCode());
        }
        AcademicResource resource = new AcademicResource();
        resource.setName(request.getName());
        resource.setCode(request.getCode());
        resource.setResourceType(request.getResourceType());
        resource.setCapacity(request.getCapacity());
        resource.setActive(true);
        return toResourceResponse(resourceRepository.save(resource));
    }

    @Override
    public AcademicResourceResponse updateResource(Long id, AcademicResourceRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        if (resourceRepository.existsByCodeIgnoreCaseAndAcademicResourceIdNot(request.getCode(), id)) {
            throw new BusinessException("Resource code already exists: " + request.getCode());
        }
        resource.setName(request.getName());
        resource.setCode(request.getCode());
        resource.setResourceType(request.getResourceType());
        resource.setCapacity(request.getCapacity());
        return toResourceResponse(resourceRepository.save(resource));
    }

    @Override
    public AcademicResourceResponse deactivateResource(Long id) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        resource.setActive(false);
        return toResourceResponse(resourceRepository.save(resource));
    }

    // ─── Readiness ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableReadinessResponse evaluateReadiness(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        return readinessEvaluator.evaluate(yearId);
    }

    // ─── Generation ──────────────────────────────────────────────────────────

    @Override
    public TimetableGenerationAcceptedResponse startGeneration(Long yearId,
                                                               TimetableGenerationStartRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        assertYearMutable(year);

        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(yearId);
        if (!readiness.isReady()) {
            throw new BusinessException("Cannot generate: readiness checks have blocking issues");
        }

        boolean generating = versionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId)
                .stream()
                .anyMatch(v -> v.getGenerationStatus() == TimetableGenerationStatus.GENERATING);
        if (generating) {
            throw new BusinessException("A timetable generation is already running for this year");
        }

        TimetableConfiguration config = findActiveConfig(yearId);
        if (config == null) {
            throw new BusinessException("No active timetable configuration found");
        }

        List<TimetableVersion> existing = versionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId);
        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1;

        TimetableVersion version = new TimetableVersion();
        version.setAcademicYear(year);
        version.setTimetableConfiguration(config);
        version.setVersionNumber(nextVersion);
        version.setGenerationStatus(TimetableGenerationStatus.GENERATING);
        version.setStatus(TimetableStatus.DRAFT);
        version = versionRepository.save(version);

        Long generationId = version.getTimetableVersionId();
        progressTracker.init(generationId);

        long seed = request != null && request.getSeed() != null
                ? request.getSeed()
                : Objects.hash(yearId, config.getTimetableConfigurationId(), nextVersion);

        String tenant = TenantContext.getTenant();
        Long orgId = OrganizationContext.getOrganizationId();
        Long userId = accessGuard.currentUserIdOrNull();

        // Launch worker only after the GENERATING version row is committed.
        final Long committedGenerationId = generationId;
        final long committedSeed = seed;
        Runnable launch = () -> generationWorker.runGeneration(
                committedGenerationId, committedSeed, committedGenerationId, tenant, orgId, userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    launch.run();
                }
            });
        } else {
            launch.run();
        }

        return TimetableGenerationAcceptedResponse.builder()
                .generationId(generationId)
                .timetableVersionId(version.getTimetableVersionId())
                .versionNumber(version.getVersionNumber())
                .status(TimetableGenerationStatus.GENERATING)
                .algorithmVersion(TimetableGenerationEngine.ALGORITHM_VERSION)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TimetableGenerationProgressResponse getGenerationProgress(Long generationId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);

        GenerationProgress progress = progressTracker.get(generationId);
        TimetableVersion version = versionRepository.findById(generationId).orElse(null);

        if (progress != null) {
            TimetableGenerateResultResponse result = null;
            if (progress.isTerminal() && version != null) {
                long entries = entryRepository.findByTimetableVersion_TimetableVersionId(generationId).size();
                List<TimetableConflict> conflicts = conflictRepository
                        .findByTimetableVersion_TimetableVersionId(generationId);
                long openBlocking = conflicts.stream()
                        .filter(c -> Boolean.TRUE.equals(c.getBlocking())
                                && c.getStatus() == TimetableConflictStatus.OPEN).count();
                long warningCount = conflicts.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getBlocking())
                                && c.getStatus() == TimetableConflictStatus.OPEN).count();

                TimetableGenerateResultResponse.ResultKind resultKind;
                if (progress.getTerminalStatus() == TimetableGenerationStatus.FAILED) {
                    resultKind = TimetableGenerateResultResponse.ResultKind.FAILED;
                } else if (openBlocking > 0
                        || version.getGenerationStatus() == TimetableGenerationStatus.GENERATED_WITH_CONFLICTS) {
                    resultKind = TimetableGenerateResultResponse.ResultKind.BLOCKED;
                } else if (warningCount > 0) {
                    resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS_WITH_WARNINGS;
                } else {
                    resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS;
                }

                result = TimetableGenerateResultResponse.builder()
                        .timetableVersionId(version.getTimetableVersionId())
                        .versionNumber(version.getVersionNumber())
                        .generationStatus(version.getGenerationStatus())
                        .totalEntries(entries)
                        .totalConflicts(conflicts.size())
                        .openBlockingConflicts(openBlocking)
                        .message(progress.getMessage())
                        .algorithmVersion(TimetableGenerationEngine.ALGORITHM_VERSION)
                        .resultKind(resultKind)
                        .build();
            }

            GenerationPhase phase = progress.getPhase();
            return TimetableGenerationProgressResponse.builder()
                    .generationId(generationId)
                    .timetableVersionId(generationId)
                    .versionNumber(version != null ? version.getVersionNumber() : null)
                    .status(progress.isTerminal() ? progress.getTerminalStatus()
                            : TimetableGenerationStatus.GENERATING)
                    .phase(phase)
                    .phaseLabel(phase != null ? phase.getLabel() : null)
                    .progressPercent(progress.getProgressPercent())
                    .result(result)
                    .message(progress.getMessage())
                    .algorithmVersion(TimetableGenerationEngine.ALGORITHM_VERSION)
                    .build();
        }

        if (version != null && version.getGenerationStatus() != TimetableGenerationStatus.GENERATING) {
            return TimetableGenerationProgressResponse.builder()
                    .generationId(generationId)
                    .timetableVersionId(version.getTimetableVersionId())
                    .versionNumber(version.getVersionNumber())
                    .status(version.getGenerationStatus())
                    .phase(GenerationPhase.FINALIZING)
                    .phaseLabel(GenerationPhase.FINALIZING.getLabel())
                    .progressPercent(100)
                    .message("Generation completed")
                    .algorithmVersion(TimetableGenerationEngine.ALGORITHM_VERSION)
                    .build();
        }

        throw new ResourceNotFoundException("Generation not found: " + generationId);
    }

    @Override
    public void cancelGeneration(Long generationId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        GenerationProgress progress = progressTracker.get(generationId);
        if (progress != null && !progress.isTerminal()) {
            progress.requestCancel();
        } else {
            throw new BusinessException("No active generation to cancel for id: " + generationId);
        }
    }

    @Override
    @Deprecated
    public TimetableGenerateResultResponse generate(Long yearId) {
        TimetableGenerationAcceptedResponse accepted = startGeneration(yearId, null);
        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(accepted.getTimetableVersionId())
                .versionNumber(accepted.getVersionNumber())
                .generationStatus(TimetableGenerationStatus.GENERATING)
                .message("Generation started asynchronously. Poll progress with generationId: "
                        + accepted.getGenerationId())
                .algorithmVersion(TimetableGenerationEngine.ALGORITHM_VERSION)
                .build();
    }

    // ─── Grid ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableGridResponse getGrid(Long versionId, String view,
                                         Long sectionId, Long staffId, Long resourceId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = requireVersion(versionId);
        TimetableConfiguration config = version.getTimetableConfiguration();

        List<TimetablePeriod> periods = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(
                        config.getTimetableConfigurationId());
        List<DayOfWeek> workingDays = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(config.getTimetableConfigurationId())
                .stream()
                .filter(w -> Boolean.TRUE.equals(w.getWorking()))
                .map(TimetableWorkingDay::getDayOfWeek)
                .toList();

        List<TimetableEntry> entries = entryRepository
                .findByTimetableVersion_TimetableVersionId(versionId);

        if ("CLASS".equalsIgnoreCase(view) && sectionId != null) {
            entries = entries.stream()
                    .filter(e -> e.getSection() != null
                            && Objects.equals(e.getSection().getSectionId(), sectionId))
                    .toList();
        } else if ("TEACHER".equalsIgnoreCase(view) && staffId != null) {
            entries = entries.stream()
                    .filter(e -> {
                        if (e.getTeacherAllocation() == null) return false;
                        TeacherAllocationTeacher primary = allocationTeacherRepository
                                .findFirstByTeacherAllocation_TeacherAllocationIdAndActiveTrueAndEffectiveToIsNullAndRoleOrderByEffectiveFromDesc(
                                        e.getTeacherAllocation().getTeacherAllocationId(),
                                        TeacherAllocationTeacherRole.PRIMARY)
                                .orElse(null);
                        return primary != null && primary.getStaff() != null
                                && Objects.equals(primary.getStaff().getStaffId(), staffId);
                    })
                    .toList();
        } else if ("ROOM".equalsIgnoreCase(view) && resourceId != null) {
            entries = entries.stream()
                    .filter(e -> e.getResource() != null
                            && Objects.equals(e.getResource().getAcademicResourceId(), resourceId))
                    .toList();
        }

        List<TimetableCellResponse> cells = entries.stream()
                .map(this::toCellResponse)
                .toList();

        List<TimetablePeriodResponse> periodResponses = periods.stream()
                .map(this::toPeriodResponse)
                .toList();

        return TimetableGridResponse.builder()
                .timetableVersionId(versionId)
                .view(view)
                .periods(periodResponses)
                .workingDays(workingDays)
                .cells(cells)
                .build();
    }

    // ─── Conflicts ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TimetableConflictResponse> getConflicts(Long versionId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireVersion(versionId);
        return conflictRepository.findByTimetableVersion_TimetableVersionId(versionId).stream()
                .map(this::toConflictResponse)
                .toList();
    }

    @Override
    public TimetableConflictResponse resolveConflict(Long conflictId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("Conflict not found: " + conflictId));
        conflict.setStatus(TimetableConflictStatus.RESOLVED);
        conflict.setResolvedAt(LocalDateTime.now());
        conflict.setResolvedByUserId(accessGuard.currentUserIdOrNull());
        return toConflictResponse(conflictRepository.save(conflict));
    }

    @Override
    public TimetableConflictResponse ignoreConflict(Long conflictId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("Conflict not found: " + conflictId));
        conflict.setStatus(TimetableConflictStatus.IGNORED);
        return toConflictResponse(conflictRepository.save(conflict));
    }

    // ─── Versions ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TimetableVersionResponse> listVersions(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        return versionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────

    @Override
    public TimetableVersionResponse submitVersion(Long versionId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = requireVersion(versionId);

        if (version.getStatus() != TimetableStatus.DRAFT
                && version.getStatus() != TimetableStatus.READY_FOR_REVIEW) {
            throw new BusinessException("Cannot submit version in status: " + version.getStatus());
        }

        long openBlocking = conflictRepository
                .findByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                        versionId, TimetableConflictStatus.OPEN)
                .size();
        if (openBlocking > 0) {
            throw new BusinessException("Cannot submit: " + openBlocking + " open blocking conflict(s) remain");
        }

        version.setStatus(TimetableStatus.PENDING_APPROVAL);
        return toVersionResponse(versionRepository.save(version));
    }

    @Override
    public TimetableVersionResponse approveVersion(Long versionId) {
        accessGuard.requireApprove(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = requireVersion(versionId);

        if (version.getStatus() != TimetableStatus.PENDING_APPROVAL) {
            throw new BusinessException("Cannot approve version in status: " + version.getStatus());
        }

        long openBlocking = conflictRepository
                .findByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                        versionId, TimetableConflictStatus.OPEN)
                .size();
        if (openBlocking > 0) {
            throw new BusinessException("Cannot approve: " + openBlocking + " open blocking conflict(s) remain");
        }

        Long yearId = version.getAcademicYear().getAcademicYearId();
        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(yearId);
        if (!readiness.isReady()) {
            throw new BusinessException("Cannot approve: readiness checks no longer pass");
        }

        version.setStatus(TimetableStatus.APPROVED);
        version.setApprovedAt(LocalDateTime.now());
        version.setApprovedByUserId(accessGuard.currentUserIdOrNull());
        return toVersionResponse(versionRepository.save(version));
    }

    @Override
    public TimetableVersionResponse rejectVersion(Long versionId) {
        accessGuard.requireApprove(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = requireVersion(versionId);

        if (version.getStatus() != TimetableStatus.PENDING_APPROVAL) {
            throw new BusinessException("Cannot reject version in status: " + version.getStatus());
        }

        version.setStatus(TimetableStatus.READY_FOR_REVIEW);
        return toVersionResponse(versionRepository.save(version));
    }

    @Override
    public TimetableVersionResponse publishVersion(Long versionId) {
        accessGuard.requireApprove(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableVersion version = requireVersion(versionId);

        if (version.getStatus() != TimetableStatus.APPROVED) {
            throw new BusinessException("Cannot publish version in status: " + version.getStatus());
        }

        long openBlocking = conflictRepository
                .findByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                        versionId, TimetableConflictStatus.OPEN)
                .size();
        if (openBlocking > 0) {
            throw new BusinessException("Cannot publish: " + openBlocking + " open blocking conflict(s) remain");
        }

        Long yearId = version.getAcademicYear().getAcademicYearId();
        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(yearId);
        if (!readiness.isReady()) {
            throw new BusinessException("Cannot publish: readiness checks no longer pass");
        }

        // Supersede any existing PUBLISHED version for same year
        versionRepository.findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED)
                .ifPresent(published -> {
                    published.setStatus(TimetableStatus.SUPERSEDED);
                    published.setSupersededAt(LocalDateTime.now());
                    versionRepository.save(published);
                });

        version.setStatus(TimetableStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedByUserId(accessGuard.currentUserIdOrNull());
        versionRepository.save(version);

        // Lock configuration
        TimetableConfiguration config = version.getTimetableConfiguration();
        config.setIsLocked(true);
        configurationRepository.save(config);

        return toVersionResponse(version);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private TimetableConfiguration findActiveConfig(Long yearId) {
        return configurationRepository.findByAcademicYear_AcademicYearId(yearId).stream()
                .filter(TimetableConfiguration::isActive)
                .findFirst()
                .orElse(null);
    }

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private TimetableVersion requireVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable version not found: " + versionId));
    }

    private boolean isYearReadOnly(AcademicYear year) {
        return READ_ONLY_YEAR_STATUSES.contains(year.getStatus());
    }

    private void assertYearMutable(AcademicYear year) {
        if (isYearReadOnly(year)) {
            throw new BusinessException("Historical academic years are read-only");
        }
        if (!year.isActive()) {
            throw new BusinessException("Cannot modify timetable for an inactive academic year");
        }
    }

    private String staffDisplayName(Staff staff) {
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return (staff.getFirstName() + middle + " " + staff.getLastName()).trim();
    }


    // ─── Mappers ──────────────────────────────────────────────────────────

    private TimetableConfigurationResponse toConfigResponse(TimetableConfiguration config) {
        Long configId = config.getTimetableConfigurationId();
        List<TimetableWorkingDayResponse> wds = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(configId).stream()
                .map(this::toWorkingDayResponse)
                .toList();
        List<TimetablePeriodResponse> periods = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId).stream()
                .map(this::toPeriodResponse)
                .toList();

        return TimetableConfigurationResponse.builder()
                .timetableConfigurationId(config.getTimetableConfigurationId())
                .academicYearId(config.getAcademicYear().getAcademicYearId())
                .name(config.getName())
                .shiftType(config.getShiftType())
                .schoolStartTime(config.getSchoolStartTime())
                .schoolEndTime(config.getSchoolEndTime())
                .defaultPeriodDurationMin(config.getDefaultPeriodDurationMin())
                .maxTeacherWeeklyPeriods(config.getMaxTeacherWeeklyPeriods())
                .status(config.getStatus())
                .isLocked(config.getIsLocked())
                .active(config.getActive())
                .workingDays(wds)
                .periods(periods)
                .build();
    }

    private TimetableWorkingDayResponse toWorkingDayResponse(TimetableWorkingDay wd) {
        return TimetableWorkingDayResponse.builder()
                .timetableWorkingDayId(wd.getTimetableWorkingDayId())
                .dayOfWeek(wd.getDayOfWeek())
                .working(wd.getWorking())
                .build();
    }

    private TimetablePeriodResponse toPeriodResponse(TimetablePeriod p) {
        return TimetablePeriodResponse.builder()
                .timetablePeriodId(p.getTimetablePeriodId())
                .periodNumber(p.getPeriodNumber())
                .name(p.getName())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .slotKind(p.getSlotKind())
                .build();
    }

    private TimetableVersionResponse toVersionResponse(TimetableVersion v) {
        long entries = entryRepository.findByTimetableVersion_TimetableVersionId(v.getTimetableVersionId()).size();
        List<TimetableConflict> conflicts = conflictRepository
                .findByTimetableVersion_TimetableVersionId(v.getTimetableVersionId());
        long openBlocking = conflicts.stream()
                .filter(c -> Boolean.TRUE.equals(c.getBlocking())
                        && c.getStatus() == TimetableConflictStatus.OPEN)
                .count();

        return TimetableVersionResponse.builder()
                .timetableVersionId(v.getTimetableVersionId())
                .academicYearId(v.getAcademicYear().getAcademicYearId())
                .timetableConfigurationId(v.getTimetableConfiguration().getTimetableConfigurationId())
                .versionNumber(v.getVersionNumber())
                .generationStatus(v.getGenerationStatus())
                .status(v.getStatus())
                .generatedAt(v.getGeneratedAt())
                .approvedAt(v.getApprovedAt())
                .approvedByUserId(v.getApprovedByUserId())
                .publishedAt(v.getPublishedAt())
                .publishedByUserId(v.getPublishedByUserId())
                .supersededAt(v.getSupersededAt())
                .totalEntries(entries)
                .totalConflicts(conflicts.size())
                .openBlockingConflicts(openBlocking)
                .build();
    }

    private TimetableCellResponse toCellResponse(TimetableEntry entry) {
        Long staffId = null;
        String staffName = null;
        if (entry.getTeacherAllocation() != null) {
            TeacherAllocationTeacher primary = allocationTeacherRepository
                    .findFirstByTeacherAllocation_TeacherAllocationIdAndActiveTrueAndEffectiveToIsNullAndRoleOrderByEffectiveFromDesc(
                            entry.getTeacherAllocation().getTeacherAllocationId(),
                            TeacherAllocationTeacherRole.PRIMARY)
                    .orElse(null);
            if (primary != null && primary.getStaff() != null) {
                staffId = primary.getStaff().getStaffId();
                staffName = staffDisplayName(primary.getStaff());
            }
        }

        return TimetableCellResponse.builder()
                .dayOfWeek(entry.getDayOfWeek())
                .periodId(entry.getTimetablePeriod().getTimetablePeriodId())
                .periodNumber(entry.getTimetablePeriod().getPeriodNumber())
                .entryId(entry.getTimetableEntryId())
                .entryType(entry.getEntryType())
                .sectionId(entry.getSection() == null ? null : entry.getSection().getSectionId())
                .sectionName(entry.getSection() == null ? null : entry.getSection().getName())
                .className(entry.getSection() == null || entry.getSection().getAcademicClass() == null
                        ? null : entry.getSection().getAcademicClass().getName())
                .subjectName(entry.getSubjectNameSnapshot())
                .staffId(staffId)
                .staffName(staffName)
                .resourceId(entry.getResource() == null ? null : entry.getResource().getAcademicResourceId())
                .resourceName(entry.getResource() == null ? null : entry.getResource().getName())
                .build();
    }

    private TimetableConflictResponse toConflictResponse(TimetableConflict c) {
        return TimetableConflictResponse.builder()
                .timetableConflictId(c.getTimetableConflictId())
                .timetableVersionId(c.getTimetableVersion().getTimetableVersionId())
                .conflictType(c.getConflictType())
                .blocking(c.getBlocking())
                .status(c.getStatus())
                .message(c.getMessage())
                .entryId(c.getTimetableEntry() == null ? null : c.getTimetableEntry().getTimetableEntryId())
                .relatedEntryId(c.getRelatedTimetableEntry() == null
                        ? null : c.getRelatedTimetableEntry().getTimetableEntryId())
                .sectionId(c.getSection() == null ? null : c.getSection().getSectionId())
                .sectionName(c.getSection() == null ? null : c.getSection().getName())
                .teacherAllocationId(c.getTeacherAllocation() == null
                        ? null : c.getTeacherAllocation().getTeacherAllocationId())
                .resourceId(c.getResource() == null ? null : c.getResource().getAcademicResourceId())
                .dayOfWeek(c.getDayOfWeek())
                .periodId(c.getTimetablePeriod() == null ? null : c.getTimetablePeriod().getTimetablePeriodId())
                .resolvedAt(c.getResolvedAt())
                .resolvedByUserId(c.getResolvedByUserId())
                .build();
    }

    private AcademicResourceResponse toResourceResponse(AcademicResource r) {
        return AcademicResourceResponse.builder()
                .academicResourceId(r.getAcademicResourceId())
                .name(r.getName())
                .code(r.getCode())
                .resourceType(r.getResourceType())
                .capacity(r.getCapacity())
                .active(r.getActive())
                .build();
    }
}
