package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicYearTransitionRequest;
import com.thinkerscave.academics.dto.response.AcademicYearTransitionResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.AcademicTransitionStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.AcademicYearTransitionService;
import com.thinkerscave.shared.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AcademicYearTransitionServiceImpl implements AcademicYearTransitionService {

    private final AcademicsAccessGuard accessGuard;
    private final AcademicYearRepository yearRepo;
    private final AcademicYearTransitionRepository transitionRepo;
    private final ClassRepository classRepo;
    private final SectionRepository sectionRepo;
    private final SubjectRepository subjectRepo;
    private final ClassSubjectMappingRepository mappingRepo;
    private final TeacherAllocationRepository allocationRepo;
    private final TeacherAllocationTeacherRepository tatRepo;

    @Override
    public AcademicYearTransitionResponse create(Long sourceYearId, AcademicYearTransitionRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);

        AcademicYear source = yearRepo.findById(sourceYearId)
                .orElseThrow(() -> new BusinessException("Source academic year not found"));
        AcademicYear target = yearRepo.findById(request.getTargetAcademicYearId())
                .orElseThrow(() -> new BusinessException("Target academic year not found"));

        if (source.getAcademicYearId().equals(target.getAcademicYearId())) {
            throw new BusinessException("Source and target academic years must be different");
        }

        transitionRepo.findBySourceAcademicYear_AcademicYearIdAndTargetAcademicYear_AcademicYearId(
                sourceYearId, target.getAcademicYearId()).ifPresent(existing -> {
            throw new BusinessException("Transition already exists between these academic years");
        });

        AcademicYearTransition transition = new AcademicYearTransition();
        transition.setSourceAcademicYear(source);
        transition.setTargetAcademicYear(target);
        transition.setStatus(AcademicTransitionStatus.NOT_STARTED);
        transition.setCopyClasses(request.isCopyClasses());
        transition.setCopySections(request.isCopySections());
        transition.setCopySubjects(request.isCopySubjects());
        transition.setCopyMappings(request.isCopyMappings());
        transition.setCopyAllocations(request.isCopyAllocations());

        transition = transitionRepo.save(transition);
        return toResponse(transition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYearTransitionResponse> listByYear(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        yearRepo.findById(yearId)
                .orElseThrow(() -> new BusinessException("Academic year not found"));

        List<AcademicYearTransition> transitions = transitionRepo.findBySourceOrTargetYear(yearId);

        return transitions.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public AcademicYearTransitionResponse start(Long transitionId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);

        AcademicYearTransition transition = transitionRepo.findById(transitionId)
                .orElseThrow(() -> new BusinessException("Transition not found"));

        if (transition.getStatus() != AcademicTransitionStatus.NOT_STARTED) {
            throw new BusinessException("Transition can only be started from NOT_STARTED status");
        }

        transition.setStatus(AcademicTransitionStatus.PREPARING);
        transition.setStartedAt(LocalDateTime.now());
        transitionRepo.save(transition);

        try {
            executeCopy(transition);
            transition.setStatus(AcademicTransitionStatus.READY);
            transition.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Transition {} failed: {}", transitionId, e.getMessage(), e);
            transition.setStatus(AcademicTransitionStatus.FAILED);
            transition.setFailureReason(e.getMessage());
        }

        transition = transitionRepo.save(transition);
        return toResponse(transition);
    }

    @Override
    public AcademicYearTransitionResponse approve(Long transitionId) {
        accessGuard.requireApprove(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);

        AcademicYearTransition transition = transitionRepo.findById(transitionId)
                .orElseThrow(() -> new BusinessException("Transition not found"));

        if (transition.getStatus() != AcademicTransitionStatus.READY
                && transition.getStatus() != AcademicTransitionStatus.PENDING_APPROVAL) {
            throw new BusinessException("Transition must be in READY or PENDING_APPROVAL status to approve");
        }

        transition.setStatus(AcademicTransitionStatus.APPROVED);
        transition.setApprovedAt(LocalDateTime.now());
        transition.setApprovedByUserId(accessGuard.currentUserIdOrNull());

        transition = transitionRepo.save(transition);
        return toResponse(transition);
    }

    // ─── Copy Logic ───────────────────────────────────────────────────────

    private void executeCopy(AcademicYearTransition transition) {
        Long sourceYearId = transition.getSourceAcademicYear().getAcademicYearId();
        AcademicYear targetYear = transition.getTargetAcademicYear();

        if (Boolean.TRUE.equals(transition.getCopyClasses())) {
            copyClasses(sourceYearId, targetYear, transition);
        }

        if (Boolean.TRUE.equals(transition.getCopySubjects())) {
            copySubjects(sourceYearId, targetYear, transition);
        }

        if (Boolean.TRUE.equals(transition.getCopyMappings())) {
            copyMappings(sourceYearId, targetYear);
        }

        if (Boolean.TRUE.equals(transition.getCopyAllocations())) {
            copyAllocations(sourceYearId, targetYear);
        }
    }

    private void copyClasses(Long sourceYearId, AcademicYear targetYear,
                             AcademicYearTransition transition) {
        List<AcademicClass> sourceClasses = classRepo
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(sourceYearId);

        for (AcademicClass src : sourceClasses) {
            if (classRepo.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                    targetYear.getAcademicYearId(), src.getCode())) {
                continue;
            }

            AcademicClass target = new AcademicClass();
            target.setAcademicYear(targetYear);
            target.setName(src.getName());
            target.setCode(src.getCode());
            target.setStage(src.getStage());
            target.setDisplayOrder(src.getDisplayOrder());
            target.setActive(true);
            target = classRepo.save(target);

            if (Boolean.TRUE.equals(transition.getCopySections())) {
                copySections(src.getClassId(), target);
            }
        }
    }

    private void copySections(Long sourceClassId, AcademicClass targetClass) {
        List<AcademicSection> sourceSections = sectionRepo
                .findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(sourceClassId);

        for (AcademicSection src : sourceSections) {
            if (sectionRepo.existsByAcademicClass_ClassIdAndCodeIgnoreCase(
                    targetClass.getClassId(), src.getCode())) {
                continue;
            }

            AcademicSection target = new AcademicSection();
            target.setAcademicClass(targetClass);
            target.setName(src.getName());
            target.setCode(src.getCode());
            target.setCapacity(src.getCapacity());
            target.setDisplayOrder(src.getDisplayOrder());
            target.setActive(true);
            sectionRepo.save(target);
        }
    }

    private void copySubjects(Long sourceYearId, AcademicYear targetYear,
                              AcademicYearTransition transition) {
        List<Subject> sourceSubjects = subjectRepo
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByNameAsc(sourceYearId);

        for (Subject src : sourceSubjects) {
            if (subjectRepo.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                    targetYear.getAcademicYearId(), src.getCode())) {
                continue;
            }

            Subject target = new Subject();
            target.setAcademicYear(targetYear);
            target.setName(src.getName());
            target.setCode(src.getCode());
            target.setCategory(src.getCategory());
            target.setDefaultWeeklyPeriods(src.getDefaultWeeklyPeriods());
            target.setTimetablePreference(src.getTimetablePreference());
            target.setDescription(src.getDescription());
            target.setActive(true);
            subjectRepo.save(target);
        }
    }

    private void copyMappings(Long sourceYearId, AcademicYear targetYear) {
        List<AcademicClass> sourceClasses = classRepo
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(sourceYearId);

        for (AcademicClass srcClass : sourceClasses) {
            Optional<AcademicClass> targetClassOpt = classRepo
                    .findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                            targetYear.getAcademicYearId(), srcClass.getCode());
            if (targetClassOpt.isEmpty()) continue;
            AcademicClass targetClass = targetClassOpt.get();

            List<ClassSubjectMapping> srcMappings = mappingRepo
                    .findByAcademicClass_ClassIdAndActiveTrue(srcClass.getClassId());

            for (ClassSubjectMapping srcMapping : srcMappings) {
                Optional<Subject> targetSubjectOpt = subjectRepo
                        .findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                                targetYear.getAcademicYearId(), srcMapping.getSubject().getCode());
                if (targetSubjectOpt.isEmpty()) continue;

                Optional<ClassSubjectMapping> existing = mappingRepo
                        .findByAcademicClass_ClassIdAndSubject_SubjectId(
                                targetClass.getClassId(), targetSubjectOpt.get().getSubjectId());
                if (existing.isPresent()) continue;

                ClassSubjectMapping targetMapping = new ClassSubjectMapping();
                targetMapping.setAcademicClass(targetClass);
                targetMapping.setSubject(targetSubjectOpt.get());
                targetMapping.setWeeklyPeriods(srcMapping.getWeeklyPeriods());
                targetMapping.setTimetablePreference(srcMapping.getTimetablePreference());
                targetMapping.setActive(true);
                mappingRepo.save(targetMapping);
            }
        }
    }

    private void copyAllocations(Long sourceYearId, AcademicYear targetYear) {
        List<TeacherAllocation> srcAllocations = allocationRepo
                .findActiveWithDetailsByYear(sourceYearId);

        for (TeacherAllocation srcAlloc : srcAllocations) {
            AcademicSection srcSection = srcAlloc.getSection();
            AcademicClass srcClass = srcSection.getAcademicClass();

            Optional<AcademicClass> targetClassOpt = classRepo
                    .findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                            targetYear.getAcademicYearId(), srcClass.getCode());
            if (targetClassOpt.isEmpty()) continue;

            Optional<AcademicSection> targetSectionOpt = sectionRepo
                    .findByAcademicClass_ClassIdAndCodeIgnoreCase(
                            targetClassOpt.get().getClassId(), srcSection.getCode());
            if (targetSectionOpt.isEmpty()) continue;

            Optional<Subject> targetSubjectOpt = subjectRepo
                    .findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                            targetYear.getAcademicYearId(),
                            srcAlloc.getClassSubjectMapping().getSubject().getCode());
            if (targetSubjectOpt.isEmpty()) continue;

            Optional<ClassSubjectMapping> targetMappingOpt = mappingRepo
                    .findByAcademicClass_ClassIdAndSubject_SubjectId(
                            targetClassOpt.get().getClassId(),
                            targetSubjectOpt.get().getSubjectId());
            if (targetMappingOpt.isEmpty()) continue;

            Optional<TeacherAllocation> existing = allocationRepo
                    .findBySection_SectionIdAndClassSubjectMapping_ClassSubjectMappingId(
                            targetSectionOpt.get().getSectionId(),
                            targetMappingOpt.get().getClassSubjectMappingId());
            if (existing.isPresent()) continue;

            TeacherAllocation targetAlloc = new TeacherAllocation();
            targetAlloc.setSection(targetSectionOpt.get());
            targetAlloc.setClassSubjectMapping(targetMappingOpt.get());
            targetAlloc.setStatus(srcAlloc.getStatus());
            targetAlloc.setActive(true);
            allocationRepo.save(targetAlloc);
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private AcademicYearTransitionResponse toResponse(AcademicYearTransition t) {
        return AcademicYearTransitionResponse.builder()
                .academicYearTransitionId(t.getAcademicYearTransitionId())
                .sourceAcademicYearId(t.getSourceAcademicYear().getAcademicYearId())
                .sourceAcademicYearName(t.getSourceAcademicYear().getName())
                .targetAcademicYearId(t.getTargetAcademicYear().getAcademicYearId())
                .targetAcademicYearName(t.getTargetAcademicYear().getName())
                .status(t.getStatus())
                .copyClasses(t.getCopyClasses())
                .copySections(t.getCopySections())
                .copySubjects(t.getCopySubjects())
                .copyMappings(t.getCopyMappings())
                .copyAllocations(t.getCopyAllocations())
                .startedAt(t.getStartedAt())
                .completedAt(t.getCompletedAt())
                .approvedAt(t.getApprovedAt())
                .approvedByUserId(t.getApprovedByUserId())
                .failureReason(t.getFailureReason())
                .build();
    }
}
