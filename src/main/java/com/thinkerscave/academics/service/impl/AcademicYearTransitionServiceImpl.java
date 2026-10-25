package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicYearTransitionRequest;
import com.thinkerscave.academics.dto.response.AcademicYearTransitionResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.AcademicTransitionStatus;
import com.thinkerscave.academics.enums.SubjectMappingScope;
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
import java.util.Objects;
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
    private final AcademicGroupRepository academicGroupRepo;
    private final SectionRepository sectionRepo;
    private final AcademicGroupSubjectMappingRepository mappingRepo;
    private final AcademicGroupSubjectMappingSectionRepository mappingSectionRepo;
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
        transition.setCopyAcademicGroups(request.isCopyAcademicGroups());
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

        if (Boolean.TRUE.equals(transition.getCopyAcademicGroups())) {
            copyAcademicGroups(sourceYearId, targetYear, transition);
        }

        if (Boolean.TRUE.equals(transition.getCopyMappings())) {
            copyMappings(sourceYearId, targetYear);
        }

        if (Boolean.TRUE.equals(transition.getCopyAllocations())) {
            copyAllocations(sourceYearId, targetYear);
        }
    }

    private void copyAcademicGroups(Long sourceYearId, AcademicYear targetYear,
                             AcademicYearTransition transition) {
        List<AcademicGroup> sourceClasses = academicGroupRepo
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(sourceYearId);

        for (AcademicGroup src : sourceClasses) {
            if (academicGroupRepo.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                    targetYear.getAcademicYearId(), src.getCode())) {
                continue;
            }

            AcademicGroup target = new AcademicGroup();
            target.setAcademicYear(targetYear);
            target.setName(src.getName());
            target.setCode(src.getCode());
            target.setStage(src.getStage());
            target.setDisplayOrder(src.getDisplayOrder());
            target.setActive(true);
            target = academicGroupRepo.save(target);

            if (Boolean.TRUE.equals(transition.getCopySections())) {
                copySections(src.getAcademicGroupId(), target);
            }
        }
    }

    private void copySections(Long sourceAcademicGroupId, AcademicGroup targetClass) {
        List<AcademicSection> sourceSections = sectionRepo
                .findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(sourceAcademicGroupId);

        for (AcademicSection src : sourceSections) {
            if (sectionRepo.existsByAcademicGroup_AcademicGroupIdAndCodeIgnoreCase(
                    targetClass.getAcademicGroupId(), src.getCode())) {
                continue;
            }

            AcademicSection target = new AcademicSection();
            target.setAcademicGroup(targetClass);
            target.setName(src.getName());
            target.setCode(src.getCode());
            target.setCapacity(src.getCapacity());
            target.setDisplayOrder(src.getDisplayOrder());
            target.setActive(true);
            sectionRepo.save(target);
        }
    }

    private void copyMappings(Long sourceYearId, AcademicYear targetYear) {
        List<AcademicGroup> sourceClasses = academicGroupRepo
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(sourceYearId);

        for (AcademicGroup srcClass : sourceClasses) {
            Optional<AcademicGroup> targetClassOpt = academicGroupRepo
                    .findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                            targetYear.getAcademicYearId(), srcClass.getCode());
            if (targetClassOpt.isEmpty()) continue;
            AcademicGroup targetClass = targetClassOpt.get();

            List<AcademicGroupSubjectMapping> srcMappings = mappingRepo
                    .findByAcademicGroup_AcademicGroupIdAndActiveTrue(srcClass.getAcademicGroupId());

            for (AcademicGroupSubjectMapping srcMapping : srcMappings) {
                Subject sharedSubject = srcMapping.getSubject();
                if (!sharedSubject.isActive()) continue;

                Optional<AcademicGroupSubjectMapping> existing = mappingRepo
                        .findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(
                        targetClass.getAcademicGroupId(), sharedSubject.getSubjectId());
                if (existing.isPresent()) continue;

                AcademicGroupSubjectMapping targetMapping = new AcademicGroupSubjectMapping();
                targetMapping.setAcademicGroup(targetClass);
                targetMapping.setSubject(sharedSubject);
                targetMapping.setWeeklyPeriods(srcMapping.getWeeklyPeriods());
                targetMapping.setTimetablePreference(srcMapping.getTimetablePreference());
                targetMapping.setRequirement(srcMapping.getRequirement());
                targetMapping.setScope(srcMapping.getScope());
                targetMapping.setActive(true);
                if (srcMapping.getScope() == SubjectMappingScope.SELECTED_SECTIONS) {
                    List<AcademicSection> targetSections = mappingSectionRepo
                            .findByMapping_AcademicGroupSubjectMappingId(srcMapping.getAcademicGroupSubjectMappingId())
                            .stream()
                            .map(AcademicGroupSubjectMappingSection::getSection)
                            .map(sourceSection -> sectionRepo.findByAcademicGroup_AcademicGroupIdAndCodeIgnoreCase(
                                    targetClass.getAcademicGroupId(), sourceSection.getCode()).orElse(null))
                            .filter(Objects::nonNull)
                            .toList();
                    if (targetSections.isEmpty()) continue;
                    targetMapping = mappingRepo.save(targetMapping);
                    for (AcademicSection targetSection : targetSections) {
                        AcademicGroupSubjectMappingSection mappingSection = new AcademicGroupSubjectMappingSection();
                        mappingSection.setMapping(targetMapping);
                        mappingSection.setSection(targetSection);
                        mappingSectionRepo.save(mappingSection);
                    }
                } else {
                    mappingRepo.save(targetMapping);
                }
            }
        }
    }

    private void copyAllocations(Long sourceYearId, AcademicYear targetYear) {
        List<TeacherAllocation> srcAllocations = allocationRepo
                .findActiveWithDetailsByYear(sourceYearId);

        for (TeacherAllocation srcAlloc : srcAllocations) {
            AcademicSection srcSection = srcAlloc.getSection();
            AcademicGroup srcClass = srcSection.getAcademicGroup();

            Optional<AcademicGroup> targetClassOpt = academicGroupRepo
                    .findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(
                            targetYear.getAcademicYearId(), srcClass.getCode());
            if (targetClassOpt.isEmpty()) continue;

            Optional<AcademicSection> targetSectionOpt = sectionRepo
                    .findByAcademicGroup_AcademicGroupIdAndCodeIgnoreCase(
                            targetClassOpt.get().getAcademicGroupId(), srcSection.getCode());
            if (targetSectionOpt.isEmpty()) continue;

            Subject sharedSubject = srcAlloc.getAcademicGroupSubjectMapping().getSubject();

            Optional<AcademicGroupSubjectMapping> targetMappingOpt = mappingRepo
                    .findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(
                            targetClassOpt.get().getAcademicGroupId(),
                        sharedSubject.getSubjectId());
            if (targetMappingOpt.isEmpty()) continue;

            Optional<TeacherAllocation> existing = allocationRepo
                    .findBySection_SectionIdAndAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(
                            targetSectionOpt.get().getSectionId(),
                            targetMappingOpt.get().getAcademicGroupSubjectMappingId());
            if (existing.isPresent()) continue;

            TeacherAllocation targetAlloc = new TeacherAllocation();
            targetAlloc.setSection(targetSectionOpt.get());
            targetAlloc.setAcademicGroupSubjectMapping(targetMappingOpt.get());
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
                .copyAcademicGroups(t.getCopyAcademicGroups())
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
