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
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningDataLoader;
import com.thinkerscave.academics.timetable.engine.TimetableReadinessEvaluator;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableServiceImpl implements TimetableService {

    private static final Logger log = LoggerFactory.getLogger(TimetableServiceImpl.class);

    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED,
            AcademicYearStatus.ARCHIVED
    );

    private final AcademicYearRepository academicYearRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableWorkingDayRepository workingDayRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableVersionRepository versionRepository;
    private final TimetableEntryRepository entryRepository;
    private final TimetableConflictRepository conflictRepository;
    private final AcademicResourceRepository resourceRepository;
    private final StaffRepository staffRepository;
    private final AcademicsAccessGuard accessGuard;
    private final TimetableReadinessEvaluator readinessEvaluator;
    private final TimetablePlanningDataLoader planningDataLoader;
    private final GenerationProgressTracker progressTracker;
    private final TimetableGenerationWorker generationWorker;

    // ─── Dashboard ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableDashboardResponse getDashboard(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);

        TimetablePlanningData data = planningDataLoader.load(yearId);
        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(data);
        List<TimetableDashboardResponse.ReadinessItem> checks = readiness.getChecks().stream()
                .map(A -> TimetableDashboardResponse.ReadinessItem.builder()
                        .key(A.getCode())
                        .label(A.getTitle() != null ? A.getTitle() : A.getCode())
                        .status(A.getStatus() == TimetableReadinessResponse.ReadinessStatus.PASSED ? "PASS"
                                : A.getStatus() == TimetableReadinessResponse.ReadinessStatus.WARNING ? "WARN" : "FAIL")
                        .message(A.getMessage())
                        .blocking(A.getSeverity() == TimetableReadinessResponse.ReadinessSeverity.BLOCKING)
                        .build())
                .toList();

        boolean yearReadOnly = isYearReadOnly(year);
        TimetableConfiguration config = data.getConfiguration();
        List<TimetableVersion> versions = data.getVersions();
        TimetableVersion published = versions.stream()
                .filter(v -> v.getStatus() == TimetableStatus.PUBLISHED)
                .findFirst().orElse(null);
        TimetableVersion latest = versions.isEmpty() ? null : versions.get(0);

        long totalConflicts = 0;
        long openBlocking = 0;
        if (latest != null) {
            Long latestId = latest.getTimetableVersionId();
            totalConflicts = conflictRepository.countByTimetableVersion_TimetableVersionId(latestId);
            openBlocking = conflictRepository.countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                    latestId, TimetableConflictStatus.OPEN);
        }

        return TimetableDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(yearReadOnly)
                .readinessChecks(checks)
                .readiness(readiness)
                .filters(buildFilters(data))
                .overallStatus(readiness.isReady() ? "READY" : "BLOCKED")
                .canGenerate(readiness.isReady() && !yearReadOnly)
                .configurationSummary(config == null ? null : toConfigResponse(config))
                .currentVersion(published == null ? null : toVersionResponse(published))
                .latestVersion(latest == null ? null : toVersionResponse(latest))
                .totalConflicts(totalConflicts)
                .openBlockingConflicts(openBlocking)
                .build();
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

        Map<Long, TimetableFiltersResponse.AcademicGroupOption> academicGroups = new LinkedHashMap<>();
        for (AcademicSection s : sections) {
            AcademicGroup cls = s.getAcademicGroup();
            academicGroups.computeIfAbsent(cls.getAcademicGroupId(),
                            id -> new TimetableFiltersResponse.AcademicGroupOption(id, cls.getName(), new ArrayList<>()))
                    .getSections()
                    .add(new TimetableFiltersResponse.Option(s.getSectionId(), s.getName()));
        }

        Map<Long, String> teachers = new HashMap<>();
        for (TimetablePlanningData.Lesson lesson : data.getLessons()) {
            teachers.putIfAbsent(lesson.teacher().getStaffId(), TimetablePlanningData.staffName(lesson.teacher()));
        }
        List<TimetableFiltersResponse.Option> teacherOptions = teachers.entrySet().stream()
                .map(e -> new TimetableFiltersResponse.Option(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(o -> o.getName().toLowerCase()))
                .toList();

        List<TimetableFiltersResponse.Option> roomOptions = data.getResources().stream()
                .map(r -> new TimetableFiltersResponse.Option(r.getAcademicResourceId(), r.getName()))
                .toList();

        return new TimetableFiltersResponse(new ArrayList<>(academicGroups.values()), teacherOptions, roomOptions);
    }

    // ─── Configuration ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TimetableConfigurationResponse getConfiguration(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        requireYear(yearId);
        TimetableConfiguration config = planningDataLoader.resolveActiveConfiguration(yearId);
        return config == null ? null : toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public TimetableConfigurationResponse getConfigurationTemplate(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);

        List<AcademicYear> otherYears = academicYearRepository.findAll().stream()
                .filter(y -> !Objects.equals(y.getAcademicYearId(), yearId))
                .sorted(Comparator.comparing(AcademicYear::getStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        for (AcademicYear other : otherYears) {
            TimetableConfiguration source = planningDataLoader.resolveActiveConfiguration(other.getAcademicYearId());
            if (source == null) continue;
            List<TimetablePeriod> periods = periodRepository
                    .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(
                            source.getTimetableConfigurationId());
            if (periods.stream().noneMatch(p -> p.getSlotKind() == TimetableSlotKind.TEACHING)) continue;

            TimetableConfigurationResponse copy = toConfigResponse(source);
            copy.setTimetableConfigurationId(null);
            copy.setAcademicYearId(year.getAcademicYearId());
            copy.setStatus(null);
            copy.setIsLocked(false);
            copy.setActive(true);
            copy.setLockedReason(null);
            copy.getWorkingDays().forEach(w -> w.setTimetableWorkingDayId(null));
            copy.getPeriods().forEach(p -> p.setTimetablePeriodId(null));
            copy.setTemplateSource("Copied from " + other.getName() + " (" + source.getName() + ")");
            return copy;
        }
        return defaultTemplate(year);
    }

    @Override
    public TimetableConfigurationResponse upsertConfiguration(Long yearId, TimetableConfigurationRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        AcademicYear year = requireYear(yearId);
        assertYearMutable(year);
        validateConfigurationRequest(request);

        TimetableShiftType shift = request.getShiftType() == null
                ? TimetableShiftType.REGULAR : request.getShiftType();

        List<TimetableConfiguration> all = configurationRepository.findByAcademicYear_AcademicYearId(yearId);
        TimetableConfiguration active = TimetablePlanningDataLoader.selectActive(all);
        TimetableConfiguration config = all.stream()
                .filter(A -> A.getShiftType() == shift)
                .findFirst()
                .orElse(null);
        if (config == null && active != null && !Boolean.TRUE.equals(active.getIsLocked())) {
            // Changing the shift of the editable configuration updates it rather than adding a second one.
            config = active;
        }

        if (config != null && Boolean.TRUE.equals(config.getIsLocked())) {
            if (isStructuralChange(config, request, shift)) {
                throw new BusinessException("'" + config.getName() + "' is used by a published timetable, so its "
                        + "working days, periods and timings are locked. You can still rename it or change the "
                        + "teacher weekly limit. To change the structure for a new term, set up the next "
                        + "academic year's timetable.");
            }
            config.setName(request.getName().trim());
            config.setDefaultPeriodDurationMin(request.getDefaultPeriodDurationMin());
            config.setMaxTeacherWeeklyPeriods(request.getMaxTeacherWeeklyPeriods());
            config.setActive(true);
            config = configurationRepository.save(config);
            renamePeriods(config, request.getPeriods());
            deactivateOtherConfigurations(all, config);
            return toConfigResponse(config);
        }

        if (config == null) {
            config = new TimetableConfiguration();
            config.setAcademicYear(year);
        }
        config.setShiftType(shift);
        config.setName(request.getName().trim());
        config.setSchoolStartTime(request.getSchoolStartTime());
        config.setSchoolEndTime(request.getSchoolEndTime());
        config.setDefaultPeriodDurationMin(request.getDefaultPeriodDurationMin());
        config.setMaxTeacherWeeklyPeriods(request.getMaxTeacherWeeklyPeriods());
        config.setActive(true);

        config = configurationRepository.save(config);
        Long configId = config.getTimetableConfigurationId();
        TimetableConfiguration configRef = config;

        if (request.getWorkingDays() != null) {
            Map<DayOfWeek, TimetableWorkingDay> existing = workingDayRepository
                    .findByTimetableConfiguration_TimetableConfigurationId(configId).stream()
                    .collect(Collectors.toMap(TimetableWorkingDay::getDayOfWeek, w -> w, (a, b) -> a));
            Set<DayOfWeek> requestedDays = request.getWorkingDays().stream()
                    .map(TimetableWorkingDayRequest::getDayOfWeek)
                    .collect(Collectors.toSet());
            existing.values().stream()
                    .filter(wd -> !requestedDays.contains(wd.getDayOfWeek()))
                    .forEach(workingDayRepository::delete);
            for (TimetableWorkingDayRequest wdReq : request.getWorkingDays()) {
                TimetableWorkingDay wd = existing.get(wdReq.getDayOfWeek());
                if (wd == null) {
                    wd = new TimetableWorkingDay();
                    wd.setTimetableConfiguration(configRef);
                    wd.setDayOfWeek(wdReq.getDayOfWeek());
                }
                wd.setWorking(Boolean.TRUE.equals(wdReq.getWorking()));
                workingDayRepository.save(wd);
            }
        }

        if (request.getPeriods() != null) {
            List<TimetablePeriod> existingPeriods = periodRepository
                    .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId);
            Set<Short> requestedNumbers = request.getPeriods().stream()
                    .map(TimetablePeriodRequest::getPeriodNumber)
                    .collect(Collectors.toSet());
            List<TimetablePeriod> removed = existingPeriods.stream()
                    .filter(p -> !requestedNumbers.contains(p.getPeriodNumber()))
                    .toList();
            if (!removed.isEmpty()) {
                discardDraftsBuiltOn(configId, removed);
                periodRepository.deleteAll(removed);
                periodRepository.flush();
            }
            for (TimetablePeriodRequest pReq : request.getPeriods()) {
                TimetablePeriod period = existingPeriods.stream()
                        .filter(p -> p.getPeriodNumber().equals(pReq.getPeriodNumber()))
                        .findFirst()
                        .orElseGet(() -> {
                            TimetablePeriod n = new TimetablePeriod();
                            n.setTimetableConfiguration(configRef);
                            return n;
                        });
                period.setPeriodNumber(pReq.getPeriodNumber());
                period.setName(pReq.getName().trim());
                period.setStartTime(pReq.getStartTime());
                period.setEndTime(pReq.getEndTime());
                period.setSlotKind(pReq.getSlotKind());
                periodRepository.save(period);
            }
        }

        boolean hasWorkingDay = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(configId).stream()
                .anyMatch(w -> Boolean.TRUE.equals(w.getWorking()));
        boolean hasTeaching = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId).stream()
                .anyMatch(p -> p.getSlotKind() == TimetableSlotKind.TEACHING);
        config.setStatus(hasWorkingDay && hasTeaching
                ? TimetableConfigurationStatus.READY
                : TimetableConfigurationStatus.INCOMPLETE);
        config = configurationRepository.save(config);

        deactivateOtherConfigurations(all, config);
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

        List<TimetableVersion> existing = versionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId);
        for (TimetableVersion v : existing) {
            if (v.getGenerationStatus() != TimetableGenerationStatus.GENERATING) continue;
            if (progressTracker.hasActive(v.getTimetableVersionId())) {
                throw new BusinessException("Version " + v.getVersionNumber()
                        + " is still being generated. Wait for it to finish or cancel it first.");
            }
            // No live worker owns it (e.g. the server restarted mid-run), so it can never finish.
            log.warn("Marking interrupted timetable generation as failed: versionId={}", v.getTimetableVersionId());
            v.setGenerationStatus(TimetableGenerationStatus.FAILED);
            v.setStatus(TimetableStatus.DRAFT);
            versionRepository.save(v);
        }

        TimetablePlanningData data = planningDataLoader.load(yearId);
        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(data);
        if (!readiness.isReady()) {
            throw new BusinessException("Cannot generate yet. Fix these first: " + describeBlocking(readiness));
        }

        TimetableConfiguration config = data.getConfiguration();
        if (config == null) {
            throw new BusinessException("Set up the timetable configuration (working days and periods) first");
        }

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
                result = buildResult(version, progress.getTerminalStatus(), progress.getMessage());
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

        if (version == null) {
            throw new ResourceNotFoundException("Generation not found: " + generationId);
        }

        boolean interrupted = version.getGenerationStatus() == TimetableGenerationStatus.GENERATING;
        TimetableGenerationStatus status = interrupted ? TimetableGenerationStatus.FAILED : version.getGenerationStatus();
        String message = interrupted
                ? "This generation was interrupted (the server restarted). Start a new generation."
                : "Generation completed";
        return TimetableGenerationProgressResponse.builder()
                .generationId(generationId)
                .timetableVersionId(version.getTimetableVersionId())
                .versionNumber(version.getVersionNumber())
                .status(status)
                .phase(GenerationPhase.FINALIZING)
                .phaseLabel(GenerationPhase.FINALIZING.getLabel())
                .progressPercent(100)
                .result(buildResult(version, status, message))
                .message(message)
                .algorithmVersion(TimetableGenerationEngine.ALGORITHM_VERSION)
                .build();
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
    public TimetableGridResponse getGrid(Long versionId, String view, Long academicGroupId,
                                         Long sectionId, Long staffId, Long resourceId) {
        if ("TEACHER".equalsIgnoreCase(view) && !accessGuard.hasElevatedRole()) {
            accessGuard.requireView(AcademicsAccessGuard.RESOURCE_MY_TIMETABLE);
            Long currentUserId = accessGuard.currentUserIdOrNull();
            Staff currentStaff = currentUserId == null ? null
                    : staffRepository.findByUser_Id(currentUserId).orElse(null);
            if (currentStaff == null || staffId == null
                    || !Objects.equals(currentStaff.getStaffId(), staffId)) {
                throw new AccessDeniedException("Teachers may only view their own timetable.");
            }
        } else {
            accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        }
        TimetableVersion version = versionRepository.findByIdWithConfigAndYear(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable version not found: " + versionId));
        Long configId = version.getTimetableConfiguration().getTimetableConfigurationId();

        List<TimetablePeriod> periods = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId);
        List<DayOfWeek> workingDays = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(configId).stream()
                .filter(w -> Boolean.TRUE.equals(w.getWorking()))
                .map(TimetableWorkingDay::getDayOfWeek)
                .sorted()
                .toList();

        Map<Long, Staff> teacherByAllocation = currentPrimaryTeachers(version.getAcademicYear().getAcademicYearId());
        List<TimetableEntry> entries = entryRepository.findGridEntries(versionId);

        if ("TEACHER".equalsIgnoreCase(view)) {
            entries = staffId == null ? List.of() : entries.stream()
                    .filter(e -> {
                        Staff teacher = e.getTeacherAllocation() == null ? null
                                : teacherByAllocation.get(e.getTeacherAllocation().getTeacherAllocationId());
                        return teacher != null && Objects.equals(teacher.getStaffId(), staffId);
                    })
                    .toList();
        } else if ("ROOM".equalsIgnoreCase(view)) {
            entries = resourceId == null ? List.of() : entries.stream()
                    .filter(e -> e.getResource() != null
                            && Objects.equals(e.getResource().getAcademicResourceId(), resourceId))
                    .toList();
        } else if (sectionId != null) {
            entries = entries.stream()
                    .filter(e -> e.getSection() != null && Objects.equals(e.getSection().getSectionId(), sectionId))
                    .toList();
        } else if (academicGroupId != null) {
            entries = entries.stream()
                    .filter(e -> e.getSection() != null && e.getSection().getAcademicGroup() != null
                            && Objects.equals(e.getSection().getAcademicGroup().getAcademicGroupId(), academicGroupId))
                    .toList();
        }

        List<TimetableCellResponse> cells = entries.stream()
                .map(e -> toCellResponse(e, teacherByAllocation))
                .toList();

        return TimetableGridResponse.builder()
                .timetableVersionId(versionId)
                .view(view)
                .periods(periods.stream().map(this::toPeriodResponse).toList())
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
        return conflictRepository.findDetailedByVersion(versionId).stream()
                .map(A -> toConflictResponse(A, versionId))
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
        conflict = conflictRepository.save(conflict);
        return toConflictResponse(conflict, conflict.getTimetableVersion().getTimetableVersionId());
    }

    @Override
    public TimetableConflictResponse ignoreConflict(Long conflictId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TIMETABLE);
        TimetableConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("Conflict not found: " + conflictId));
        conflict.setStatus(TimetableConflictStatus.IGNORED);
        conflict = conflictRepository.save(conflict);
        return toConflictResponse(conflict, conflict.getTimetableVersion().getTimetableVersionId());
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
        assertGenerated(version, "submit");
        assertNoOpenBlocking(versionId, "submit");

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
        assertNoOpenBlocking(versionId, "approve");
        assertStillReady(version, "approve");

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
        assertNoOpenBlocking(versionId, "publish");
        assertStillReady(version, "publish");

        Long yearId = version.getAcademicYear().getAcademicYearId();
        versionRepository.findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED)
                .ifPresent(published -> {
                    published.setStatus(TimetableStatus.SUPERSEDED);
                    published.setSupersededAt(LocalDateTime.now());
                    versionRepository.saveAndFlush(published);
                });

        version.setStatus(TimetableStatus.PUBLISHED);
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedByUserId(accessGuard.currentUserIdOrNull());
        versionRepository.save(version);

        TimetableConfiguration config = version.getTimetableConfiguration();
        config.setIsLocked(true);
        configurationRepository.save(config);

        return toVersionResponse(version);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

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

    private void assertGenerated(TimetableVersion version, String action) {
        TimetableGenerationStatus status = version.getGenerationStatus();
        if (status != TimetableGenerationStatus.GENERATED
                && status != TimetableGenerationStatus.GENERATED_WITH_CONFLICTS) {
            throw new BusinessException("Cannot " + action + " version " + version.getVersionNumber()
                    + ": it has not been generated successfully (" + status + ")");
        }
    }

    private void assertNoOpenBlocking(Long versionId, String action) {
        long openBlocking = conflictRepository.countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                versionId, TimetableConflictStatus.OPEN);
        if (openBlocking > 0) {
            throw new BusinessException("Cannot " + action + ": " + openBlocking
                    + " open blocking conflict(s) remain. Review them on the Conflicts tab, fix the data and regenerate.");
        }
    }

    private void assertStillReady(TimetableVersion version, String action) {
        TimetableReadinessResponse readiness = readinessEvaluator.evaluate(
                planningDataLoader.load(version.getAcademicYear().getAcademicYearId(), version.getTimetableConfiguration()));
        if (!readiness.isReady()) {
            throw new BusinessException("Cannot " + action + ": the academic setup changed since generation. "
                    + describeBlocking(readiness));
        }
    }

    private static String describeBlocking(TimetableReadinessResponse readiness) {
        return readiness.getBlockingIssues().stream()
                .map(A -> (A.getTitle() != null ? A.getTitle() : A.getCode()) + " — " + A.getMessage())
                .collect(Collectors.joining("; "));
    }

    /**
     * Timetables generated on the old period structure reference the periods being removed and are
     * invalid once they change, so unpublished drafts are discarded. Submitted or approved versions
     * must be rejected first.
     */
    private void discardDraftsBuiltOn(Long configId, List<TimetablePeriod> removed) {
        List<TimetableVersion> versions = versionRepository.findByTimetableConfiguration_TimetableConfigurationId(configId);
        if (versions.isEmpty()) return;
        String removedNames = removed.stream().map(TimetablePeriod::getName).collect(Collectors.joining(", "));
        for (TimetableVersion v : versions) {
            boolean inWorkflow = v.getStatus() == TimetableStatus.PENDING_APPROVAL
                    || v.getStatus() == TimetableStatus.APPROVED
                    || v.getStatus() == TimetableStatus.PUBLISHED
                    || v.getStatus() == TimetableStatus.SUPERSEDED;
            if (inWorkflow || progressTracker.hasActive(v.getTimetableVersionId())) {
                throw new BusinessException("Cannot remove " + removedNames + ": timetable version "
                        + v.getVersionNumber() + " (" + v.getStatus() + ") was built on these periods. "
                        + "Reject it first, or keep the periods and change their timings instead.");
            }
        }
        List<Long> ids = versions.stream().map(TimetableVersion::getTimetableVersionId).toList();
        conflictRepository.deleteByVersionIds(ids);
        entryRepository.deleteByVersionIds(ids);
        versionRepository.deleteAll(versions);
        versionRepository.flush();
        log.info("Discarded {} draft timetable version(s) of configuration {} after removing periods {}",
                ids.size(), configId, removedNames);
    }

    private void deactivateOtherConfigurations(List<TimetableConfiguration> all, TimetableConfiguration keep) {
        for (TimetableConfiguration other : all) {
            if (Objects.equals(other.getTimetableConfigurationId(), keep.getTimetableConfigurationId())) continue;
            if (!other.isActive()) continue;
            other.setActive(false);
            configurationRepository.save(other);
        }
    }

    private void validateConfigurationRequest(TimetableConfigurationRequest request) {
        if (!request.getSchoolStartTime().isBefore(request.getSchoolEndTime())) {
            throw new BusinessException("School start time must be before school end time");
        }
        if (request.getWorkingDays() != null) {
            Set<DayOfWeek> seen = EnumSet.noneOf(DayOfWeek.class);
            for (TimetableWorkingDayRequest wd : request.getWorkingDays()) {
                if (wd.getDayOfWeek() == null) {
                    throw new BusinessException("Every working day entry needs a day of the week");
                }
                if (!seen.add(wd.getDayOfWeek())) {
                    throw new BusinessException(wd.getDayOfWeek() + " is listed more than once in working days");
                }
            }
        }
        if (request.getPeriods() == null) return;

        Set<Short> numbers = new HashSet<>();
        List<TimetablePeriodRequest> sorted = new ArrayList<>(request.getPeriods());
        for (TimetablePeriodRequest p : sorted) {
            if (!numbers.add(p.getPeriodNumber())) {
                throw new BusinessException("Period number " + p.getPeriodNumber() + " is used more than once");
            }
            if (!p.getStartTime().isBefore(p.getEndTime())) {
                throw new BusinessException("'" + p.getName() + "' must start before it ends ("
                        + p.getStartTime() + " – " + p.getEndTime() + ")");
            }
            if (p.getStartTime().isBefore(request.getSchoolStartTime())
                    || p.getEndTime().isAfter(request.getSchoolEndTime())) {
                throw new BusinessException("'" + p.getName() + "' (" + p.getStartTime() + " – " + p.getEndTime()
                        + ") falls outside school hours " + request.getSchoolStartTime() + " – "
                        + request.getSchoolEndTime());
            }
        }
        sorted.sort(Comparator.comparing(TimetablePeriodRequest::getStartTime));
        for (int i = 1; i < sorted.size(); i++) {
            TimetablePeriodRequest prev = sorted.get(i - 1);
            TimetablePeriodRequest cur = sorted.get(i);
            if (cur.getStartTime().isBefore(prev.getEndTime())) {
                throw new BusinessException("'" + prev.getName() + "' (" + prev.getStartTime() + " – "
                        + prev.getEndTime() + ") overlaps '" + cur.getName() + "' (" + cur.getStartTime()
                        + " – " + cur.getEndTime() + ")");
            }
        }
    }

    private void renamePeriods(TimetableConfiguration config, List<TimetablePeriodRequest> requested) {
        if (requested == null) return;
        Map<Short, String> names = new HashMap<>();
        for (TimetablePeriodRequest p : requested) {
            if (p.getPeriodNumber() != null && StringUtils.hasText(p.getName())) {
                names.put(p.getPeriodNumber(), p.getName().trim());
            }
        }
        List<TimetablePeriod> changed = new ArrayList<>();
        for (TimetablePeriod period : periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(
                        config.getTimetableConfigurationId())) {
            String name = names.get(period.getPeriodNumber());
            if (name != null && !name.equals(period.getName())) {
                period.setName(name);
                changed.add(period);
            }
        }
        if (!changed.isEmpty()) periodRepository.saveAll(changed);
    }

    private boolean isStructuralChange(TimetableConfiguration config, TimetableConfigurationRequest request,
                                       TimetableShiftType shift) {
        if (config.getShiftType() != shift
                || !Objects.equals(config.getSchoolStartTime(), request.getSchoolStartTime())
                || !Objects.equals(config.getSchoolEndTime(), request.getSchoolEndTime())) {
            return true;
        }
        Long configId = config.getTimetableConfigurationId();
        if (request.getWorkingDays() != null) {
            Set<DayOfWeek> current = workingDayRepository
                    .findByTimetableConfiguration_TimetableConfigurationId(configId).stream()
                    .filter(w -> Boolean.TRUE.equals(w.getWorking()))
                    .map(TimetableWorkingDay::getDayOfWeek)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
            Set<DayOfWeek> requested = request.getWorkingDays().stream()
                    .filter(w -> Boolean.TRUE.equals(w.getWorking()))
                    .map(TimetableWorkingDayRequest::getDayOfWeek)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
            if (!current.equals(requested)) return true;
        }
        if (request.getPeriods() != null) {
            Set<String> current = periodRepository
                    .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId).stream()
                    .map(p -> periodSignature(p.getPeriodNumber(), p.getStartTime(), p.getEndTime(), p.getSlotKind()))
                    .collect(Collectors.toSet());
            Set<String> requested = request.getPeriods().stream()
                    .map(p -> periodSignature(p.getPeriodNumber(), p.getStartTime(), p.getEndTime(), p.getSlotKind()))
                    .collect(Collectors.toSet());
            return !current.equals(requested);
        }
        return false;
    }

    private static String periodSignature(Short number, LocalTime start, LocalTime end, TimetableSlotKind kind) {
        return number + "|" + start + "|" + end + "|" + kind;
    }

    private TimetableConfigurationResponse defaultTemplate(AcademicYear year) {
        List<TimetableWorkingDayResponse> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            days.add(TimetableWorkingDayResponse.builder()
                    .dayOfWeek(day)
                    .working(day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY)
                    .build());
        }
        List<TimetablePeriodResponse> periods = new ArrayList<>();
        LocalTime t = LocalTime.of(8, 0);
        short number = 1;
        int teaching = 0;
        while (teaching < 8) {
            if (teaching == 3) {
                periods.add(templatePeriod(number++, "Short Break", t, t.plusMinutes(15), TimetableSlotKind.BREAK));
                t = t.plusMinutes(15);
            } else if (teaching == 5) {
                periods.add(templatePeriod(number++, "Lunch Break", t, t.plusMinutes(30), TimetableSlotKind.BREAK));
                t = t.plusMinutes(30);
            }
            teaching++;
            periods.add(templatePeriod(number++, "Period " + teaching, t, t.plusMinutes(40), TimetableSlotKind.TEACHING));
            t = t.plusMinutes(40);
        }
        return TimetableConfigurationResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .name("Regular Shift")
                .shiftType(TimetableShiftType.REGULAR)
                .schoolStartTime(LocalTime.of(8, 0))
                .schoolEndTime(t)
                .defaultPeriodDurationMin((short) 40)
                .maxTeacherWeeklyPeriods((short) 30)
                .isLocked(false)
                .active(true)
                .workingDays(days)
                .periods(periods)
                .templateSource("Standard school day: 8 periods of 40 minutes, Monday to Friday")
                .build();
    }

    private static TimetablePeriodResponse templatePeriod(short number, String name, LocalTime start, LocalTime end,
                                                          TimetableSlotKind kind) {
        return TimetablePeriodResponse.builder()
                .periodNumber(number)
                .name(name)
                .startTime(start)
                .endTime(end)
                .slotKind(kind)
                .build();
    }

    private Map<Long, Staff> currentPrimaryTeachers(Long yearId) {
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

    private TimetableGenerateResultResponse buildResult(TimetableVersion version,
                                                        TimetableGenerationStatus terminalStatus,
                                                        String message) {
        Long versionId = version.getTimetableVersionId();
        long entries = entryRepository.countByTimetableVersion_TimetableVersionId(versionId);
        long total = conflictRepository.countByTimetableVersion_TimetableVersionId(versionId);
        long openBlocking = conflictRepository.countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                versionId, TimetableConflictStatus.OPEN);
        long warnings = conflictRepository.countByTimetableVersion_TimetableVersionIdAndBlockingFalseAndStatus(
                versionId, TimetableConflictStatus.OPEN);

        TimetableGenerateResultResponse.ResultKind resultKind;
        if (terminalStatus == TimetableGenerationStatus.FAILED) {
            resultKind = TimetableGenerateResultResponse.ResultKind.FAILED;
        } else if (openBlocking > 0
                || version.getGenerationStatus() == TimetableGenerationStatus.GENERATED_WITH_CONFLICTS) {
            resultKind = TimetableGenerateResultResponse.ResultKind.BLOCKED;
        } else if (warnings > 0) {
            resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS_WITH_WARNINGS;
        } else {
            resultKind = TimetableGenerateResultResponse.ResultKind.SUCCESS;
        }

        return TimetableGenerateResultResponse.builder()
                .timetableVersionId(versionId)
                .versionNumber(version.getVersionNumber())
                .generationStatus(terminalStatus == TimetableGenerationStatus.FAILED
                        ? TimetableGenerationStatus.FAILED : version.getGenerationStatus())
                .totalEntries(entries)
                .totalConflicts(total)
                .openBlockingConflicts(openBlocking)
                .message(message)
                .algorithmVersion(TimetableGenerationEngine.ALGORITHM_VERSION)
                .resultKind(resultKind)
                .build();
    }

    // ─── Mappers ──────────────────────────────────────────────────────────

    private TimetableConfigurationResponse toConfigResponse(TimetableConfiguration config) {
        Long configId = config.getTimetableConfigurationId();
        Map<DayOfWeek, TimetableWorkingDay> stored = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(configId).stream()
                .collect(Collectors.toMap(TimetableWorkingDay::getDayOfWeek, w -> w, (a, b) -> a));
        List<TimetableWorkingDayResponse> wds = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            TimetableWorkingDay wd = stored.get(day);
            wds.add(TimetableWorkingDayResponse.builder()
                    .timetableWorkingDayId(wd == null ? null : wd.getTimetableWorkingDayId())
                    .dayOfWeek(day)
                    .working(wd != null && Boolean.TRUE.equals(wd.getWorking()))
                    .build());
        }
        List<TimetablePeriodResponse> periods = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId).stream()
                .map(this::toPeriodResponse)
                .toList();

        String lockedReason = null;
        if (Boolean.TRUE.equals(config.getIsLocked())) {
            lockedReason = "Working days, periods and timings are locked because a timetable using this "
                    + "configuration has been published. Name and teacher weekly limit can still be changed.";
        }

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
                .lockedReason(lockedReason)
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
        Long versionId = v.getTimetableVersionId();
        return TimetableVersionResponse.builder()
                .timetableVersionId(versionId)
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
                .totalEntries(entryRepository.countByTimetableVersion_TimetableVersionId(versionId))
                .totalConflicts(conflictRepository.countByTimetableVersion_TimetableVersionId(versionId))
                .openBlockingConflicts(conflictRepository
                        .countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                                versionId, TimetableConflictStatus.OPEN))
                .build();
    }

    private TimetableCellResponse toCellResponse(TimetableEntry entry, Map<Long, Staff> teacherByAllocation) {
        Staff teacher = entry.getTeacherAllocation() == null ? null
                : teacherByAllocation.get(entry.getTeacherAllocation().getTeacherAllocationId());
        AcademicSection section = entry.getSection();
        return TimetableCellResponse.builder()
                .dayOfWeek(entry.getDayOfWeek())
                .periodId(entry.getTimetablePeriod().getTimetablePeriodId())
                .periodNumber(entry.getTimetablePeriod().getPeriodNumber())
                .entryId(entry.getTimetableEntryId())
                .entryType(entry.getEntryType())
                .sectionId(section == null ? null : section.getSectionId())
                .sectionName(section == null ? null : section.getName())
                .academicGroupName(section == null || section.getAcademicGroup() == null
                        ? null : section.getAcademicGroup().getName())
                .subjectName(entry.getSubjectNameSnapshot())
                .staffId(teacher == null ? null : teacher.getStaffId())
                .staffName(teacher == null ? null : TimetablePlanningData.staffName(teacher))
                .resourceId(entry.getResource() == null ? null : entry.getResource().getAcademicResourceId())
                .resourceName(entry.getResource() == null ? null : entry.getResource().getName())
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
                .relatedEntryId(A.getRelatedTimetableEntry() == null
                        ? null : A.getRelatedTimetableEntry().getTimetableEntryId())
                .sectionId(section == null ? null : section.getSectionId())
                .sectionName(section == null ? null : section.getName())
                .academicGroupName(section == null || section.getAcademicGroup() == null
                        ? null : section.getAcademicGroup().getName())
                .teacherAllocationId(A.getTeacherAllocation() == null
                        ? null : A.getTeacherAllocation().getTeacherAllocationId())
                .resourceId(A.getResource() == null ? null : A.getResource().getAcademicResourceId())
                .dayOfWeek(A.getDayOfWeek())
                .periodId(A.getTimetablePeriod() == null ? null : A.getTimetablePeriod().getTimetablePeriodId())
                .periodName(A.getTimetablePeriod() == null ? null : A.getTimetablePeriod().getName())
                .resolvedAt(A.getResolvedAt())
                .resolvedByUserId(A.getResolvedByUserId())
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
