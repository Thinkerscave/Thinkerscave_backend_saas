package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicGroupRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicGroupResponse;
import com.thinkerscave.academics.dto.response.AcademicGroupSummaryResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.dto.response.AcademicGroupsDashboardResponse;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicResource;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.SectionTeacherAssignment;
import com.thinkerscave.academics.enums.AcademicStage;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicResourceRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.AcademicGroupRepository;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.SectionTeacherAssignmentRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.AcademicStructureService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicStructureServiceImpl implements AcademicStructureService {

    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED,
            AcademicYearStatus.ARCHIVED
    );

    private final AcademicYearRepository academicYearRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SectionRepository sectionRepository;
    private final SectionTeacherAssignmentRepository SectionTeacherAssignmentRepository;
    private final AcademicResourceRepository academicResourceRepository;
    private final StaffRepository staffRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AcademicGroupSubjectMappingRepository academicGroupSubjectMappingRepository;
    private final TeacherAllocationTeacherRepository TeacherAllocationTeacherRepository;
    private final TimetableVersionRepository timetableVersionRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public AcademicGroupsDashboardResponse getDashboard(
            Long academicYearId, String q, AcademicStage stage, Boolean active) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicYear year = requireYear(academicYearId);

        List<AcademicGroup> academicGroups = academicGroupRepository
                .findWithYearByAcademicYearIdOrderByDisplayOrderAsc(academicYearId)
                .stream()
                .filter(A -> stage == null || A.getStage() == stage)
                .filter(A -> active == null || Objects.equals(A.getActive(), active))
                .filter(A -> matchesQuery(A, q))
                .toList();

        // Bulk counts: one query per metric for the whole year, not per group.
        Map<Long, long[]> sectionCountsByAcademicGroup = new HashMap<>();
        for (AcademicSection s : sectionRepository.findWithAcademicGroupByYearId(academicYearId)) {
            long[] counts = sectionCountsByAcademicGroup.computeIfAbsent(s.getAcademicGroup().getAcademicGroupId(), k -> new long[2]);
            counts[0]++;
            if (s.isActive()) counts[1]++;
        }
        Map<Long, Long> studentsByAcademicGroup = new HashMap<>();
        for (Object[] row : studentEnrollmentRepository.countActiveByAcademicGroupForYear(academicYearId)) {
            studentsByAcademicGroup.put((Long) row[0], (Long) row[1]);
        }
        List<AcademicGroupResponse> academicGroupResponses = academicGroups.stream()
                .map(A -> {
                    long[] counts = sectionCountsByAcademicGroup.getOrDefault(A.getAcademicGroupId(), new long[2]);
                    return toDirectoryResponse(A, counts[0], counts[1],
                            studentsByAcademicGroup.getOrDefault(A.getAcademicGroupId(), 0L));
                })
                .toList();

        long sectionCount = sectionRepository.countByAcademicGroup_AcademicYear_AcademicYearId(academicYearId);
        long sectionsActive = sectionRepository.countByAcademicGroup_AcademicYear_AcademicYearIdAndActiveTrue(academicYearId);

        return AcademicGroupsDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .academicGroupCount(academicGroupRepository.countByAcademicYear_AcademicYearId(academicYearId))
                .academicGroupsActive(academicGroupRepository.countByAcademicYear_AcademicYearIdAndActiveTrue(academicYearId))
                .sectionCount(sectionCount)
                .sectionsActive(sectionsActive)
                .studentCount(studentEnrollmentRepository.countByAcademicYearAcademicYearIdAndActiveTrue(academicYearId))
                .academicGroups(academicGroupResponses)
                .build();
    }

    @Override
    public AcademicGroupResponse createAcademicGroup(AcademicGroupRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicYear year = requireYear(request.getAcademicYearId());
        assertYearMutable(year);

        String name = request.getName().trim();
        String code = StringUtils.hasText(request.getCode())
                ? request.getCode().trim().toUpperCase(Locale.ROOT)
                : generateUniqueAcademicGroupCode(year.getAcademicYearId(), name);
        validateAcademicGroupUniqueness(year.getAcademicYearId(), name, code, null);

        AcademicGroup entity = new AcademicGroup();
        entity.setAcademicYear(year);
        entity.setName(name);
        entity.setCode(code);
        entity.setStage(request.getStage());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setDisplayOrder(request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextAcademicGroupDisplayOrder(year.getAcademicYearId()));
        entity.setActive(true);

        List<AcademicSectionRequest> sectionRequests = request.getSections() == null
                ? List.of()
                : request.getSections();
        validateNewSectionNames(sectionRequests);
        AcademicGroup saved = academicGroupRepository.save(entity);

        // Sections are saved in this same transaction; any failure rolls back the group too.
        for (AcademicSectionRequest sectionRequest : sectionRequests) {
            createSectionEntity(saved, sectionRequest);
        }
        return toAcademicGroupResponse(saved, true, true);
    }

    @Override
    public AcademicGroupResponse updateAcademicGroup(Long academicGroupId, AcademicGroupRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicGroup entity = requireAcademicGroup(academicGroupId);
        assertYearMutable(entity.getAcademicYear());

        String name = request.getName().trim();
        String code = StringUtils.hasText(request.getCode())
                ? request.getCode().trim().toUpperCase(Locale.ROOT)
                : entity.getCode();
        validateAcademicGroupUniqueness(entity.getAcademicYear().getAcademicYearId(), name, code, academicGroupId);

        entity.setName(name);
        entity.setCode(code);
        entity.setStage(request.getStage());
        entity.setDescription(trimToNull(request.getDescription()));
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        return toAcademicGroupResponse(academicGroupRepository.save(entity), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicGroupResponse getAcademicGroupById(Long academicGroupId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        return toAcademicGroupResponse(requireAcademicGroup(academicGroupId), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicGroupResponse> getAcademicGroupsByYear(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        requireYear(academicYearId);
        return academicGroupRepository.findWithYearByAcademicYearIdOrderByDisplayOrderAsc(academicYearId).stream()
                .map(A -> toAcademicGroupResponse(A, true, false))
                .toList();
    }

    @Override
    public AcademicGroupResponse deactivateAcademicGroup(Long academicGroupId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicGroup entity = requireAcademicGroup(academicGroupId);
        assertYearMutable(entity.getAcademicYear());
        long activeStudents = studentEnrollmentRepository.countByAcademicGroupEntityAcademicGroupIdAndActiveTrue(academicGroupId);
        if (activeStudents > 0) {
            throw new BusinessException("Cannot deactivate " + entity.getName() + ": " + activeStudents
                    + " active student(s) are enrolled. Move or withdraw them first.");
        }
        entity.setActive(false);
        return toAcademicGroupResponse(academicGroupRepository.save(entity), true, false);
    }

    @Override
    public AcademicGroupResponse activateAcademicGroup(Long academicGroupId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicGroup entity = requireAcademicGroup(academicGroupId);
        assertYearMutable(entity.getAcademicYear());
        entity.setActive(true);
        return toAcademicGroupResponse(academicGroupRepository.save(entity), true, false);
    }

    @Override
    public AcademicSectionResponse createSection(Long academicGroupId, AcademicSectionRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicGroup academicGroup = requireAcademicGroup(academicGroupId);
        assertYearMutable(academicGroup.getAcademicYear());

        return toSectionResponse(createSectionEntity(academicGroup, request));
    }

    @Override
    public AcademicSectionResponse updateSection(Long sectionId, AcademicSectionRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicSection section = requireSection(sectionId);
        assertYearMutable(section.getAcademicGroup().getAcademicYear());

        String name = request.getName().trim();
        String code = StringUtils.hasText(request.getCode())
                ? request.getCode().trim().toUpperCase(Locale.ROOT)
                : section.getCode();
        validateSectionUniqueness(section.getAcademicGroup().getAcademicGroupId(), name, code, sectionId);

        section.setName(name);
        section.setCode(code);
        // Capacity is retained in schema for compatibility but is not part of the
        // active Class/Section admin UX — only overwrite when explicitly provided.
        if (request.getCapacity() != null) {
            section.setCapacity(request.getCapacity());
        }
        if (request.getDisplayOrder() != null) {
            section.setDisplayOrder(request.getDisplayOrder());
        }
        section.setDefaultResource(resolveResource(request.getDefaultResourceId()));
        AcademicSection saved = sectionRepository.save(section);

        // Class teacher belongs to the Section. Null clears the active assignment.
        if (request.getSectionTeacherStaffId() != null) {
            assignSectionTeacher(saved, request.getSectionTeacherStaffId());
        } else {
            clearSectionTeacher(saved);
        }
        return toSectionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicSectionResponse getSectionById(Long sectionId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        return toSectionResponse(requireSection(sectionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicSectionResponse> getSectionsByAcademicGroup(Long academicGroupId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        requireAcademicGroup(academicGroupId);
        return sectionRepository.findWithAcademicGroupByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(academicGroupId).stream()
                .map(this::toSectionResponse)
                .toList();
    }

    @Override
    public AcademicSectionResponse deactivateSection(Long sectionId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicSection section = requireSection(sectionId);
        assertYearMutable(section.getAcademicGroup().getAcademicYear());
        long activeStudents = studentEnrollmentRepository.countBySectionSectionIdAndActiveTrue(sectionId);
        if (activeStudents > 0) {
            throw new BusinessException("Cannot deactivate " + section.getName() + ": " + activeStudents
                    + " active student(s) are enrolled. Move or withdraw them first.");
        }
        section.setActive(false);
        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    public AcademicSectionResponse activateSection(Long sectionId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicSection section = requireSection(sectionId);
        assertYearMutable(section.getAcademicGroup().getAcademicYear());
        section.setActive(true);
        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicGroupSummaryResponse getGroupSummary(Long academicGroupId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicGroup academicGroup = requireAcademicGroup(academicGroupId);
        Long yearId = academicGroup.getAcademicYear().getAcademicYearId();
        return AcademicGroupSummaryResponse.builder()
                .academicGroupId(academicGroupId)
                .sectionCount(sectionRepository.countByAcademicGroup_AcademicGroupId(academicGroupId))
                .activeSectionCount(sectionRepository.countByAcademicGroup_AcademicGroupIdAndActiveTrue(academicGroupId))
                .studentCount(studentEnrollmentRepository.countByAcademicGroupEntityAcademicGroupIdAndActiveTrue(academicGroupId))
                .subjectCount(academicGroupSubjectMappingRepository.countByAcademicGroup_AcademicGroupIdAndActiveTrue(academicGroupId))
                .teacherCount(TeacherAllocationTeacherRepository.countDistinctPrimaryTeachersByAcademicGroup(academicGroupId))
                .timetableStatus(resolveTimetableStatus(yearId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicStructureTreeResponse> getStructureTree(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        requireYear(academicYearId);

        List<AcademicGroup> academicGroups = academicGroupRepository
                .findWithYearByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);

        Map<AcademicStage, List<AcademicGroup>> byStage = academicGroups.stream()
                .collect(Collectors.groupingBy(
                        AcademicGroup::getStage,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<AcademicStructureTreeResponse> tree = new ArrayList<>();
        for (AcademicStage stage : AcademicStage.values()) {
            List<AcademicGroup> stageClasses = byStage.getOrDefault(stage, List.of());
            if (stageClasses.isEmpty()) {
                continue;
            }
            List<AcademicStructureTreeResponse.AcademicGroupNode> classNodes = stageClasses.stream()
                    .map(A -> AcademicStructureTreeResponse.AcademicGroupNode.builder()
                            .academicGroupId(A.getAcademicGroupId())
                            .name(A.getName())
                            .code(A.getCode())
                            .active(A.getActive())
                            .displayOrder(A.getDisplayOrder())
                            .sections(sectionRepository
                                    .findByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(A.getAcademicGroupId())
                                    .stream()
                                    .map(s -> AcademicStructureTreeResponse.SectionNode.builder()
                                            .sectionId(s.getSectionId())
                                            .name(s.getName())
                                            .code(s.getCode())
                                            .capacity(s.getCapacity())
                                            .active(s.getActive())
                                            .build())
                                    .toList())
                            .build())
                    .toList();
            tree.add(AcademicStructureTreeResponse.builder()
                    .stage(stage)
                    .academicGroups(classNodes)
                    .build());
        }
        return tree;
    }

    private AcademicGroupResponse toAcademicGroupResponse(AcademicGroup entity, boolean withCounts, boolean withSections) {
        long sectionCount = 0;
        long sectionsActive = 0;
        long studentCount = 0;
        List<AcademicSectionResponse> sections = null;

        if (withCounts || withSections) {
            List<AcademicSection> sectionEntities = sectionRepository
                    .findByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(entity.getAcademicGroupId());
            sectionCount = sectionEntities.size();
            sectionsActive = sectionEntities.stream().filter(AcademicSection::isActive).count();
            studentCount = studentEnrollmentRepository.countByAcademicGroupEntityAcademicGroupIdAndActiveTrue(entity.getAcademicGroupId());

            if (withSections) {
                sections = sectionEntities.stream().map(this::toSectionResponse).toList();
            }
        }

        return buildAcademicGroupResponse(entity, sectionCount, sectionsActive, studentCount, sections);
    }

    private AcademicGroupResponse toDirectoryResponse(AcademicGroup entity, long sectionCount,
                                                      long sectionsActive, long studentCount) {
        return buildAcademicGroupResponse(entity, sectionCount, sectionsActive, studentCount, null);
    }

    private AcademicGroupResponse buildAcademicGroupResponse(AcademicGroup entity, long sectionCount, long sectionsActive,
                                                     long studentCount, List<AcademicSectionResponse> sections) {
        AcademicYear year = entity.getAcademicYear();

        // Class Teacher is a Section relationship only — never roll up a single
        // teacher onto the Class response (that implied an incorrect Class→Teacher model).
        return AcademicGroupResponse.builder()
                .academicGroupId(entity.getAcademicGroupId())
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .name(entity.getName())
                .code(entity.getCode())
                .stage(entity.getStage())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.getActive())
                .sectionCount(sectionCount)
                .sectionsActive(sectionsActive)
                .studentCount(studentCount)
                .sectionTeacherName(null)
                .sectionTeacherStaffId(null)
                .sections(sections)
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    private AcademicSectionResponse toSectionResponse(AcademicSection section) {
        AcademicGroup academicGroup = section.getAcademicGroup();
        SectionTeacherAssignment teacher = SectionTeacherAssignmentRepository
                .findFirstBySection_SectionIdAndActiveTrueAndEffectiveToIsNullOrderByEffectiveFromDesc(
                        section.getSectionId())
                .orElse(null);

        return AcademicSectionResponse.builder()
                .sectionId(section.getSectionId())
                .academicGroupId(academicGroup.getAcademicGroupId())
                .academicGroupName(academicGroup.getName())
                .academicGroupCode(academicGroup.getCode())
                .name(section.getName())
                .code(section.getCode())
                .capacity(section.getCapacity())
                .displayOrder(section.getDisplayOrder())
                .defaultResourceId(section.getDefaultResource() == null
                        ? null
                        : section.getDefaultResource().getAcademicResourceId())
                .active(section.getActive())
                .studentCount(studentEnrollmentRepository.countBySectionSectionIdAndActiveTrue(section.getSectionId()))
                .sectionTeacherName(teacher == null || teacher.getStaff() == null
                        ? null
                        : staffDisplayName(teacher.getStaff()))
                .sectionTeacherStaffId(teacher == null || teacher.getStaff() == null
                        ? null
                        : teacher.getStaff().getStaffId())
                .createdBy(section.getCreatedBy())
                .createdOn(section.getCreatedOn())
                .updatedBy(section.getUpdatedBy())
                .updatedOn(section.getUpdatedOn())
                .build();
    }

    private void assignSectionTeacher(AcademicSection section, Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        clearSectionTeacher(section);

        SectionTeacherAssignment assignment = new SectionTeacherAssignment();
        assignment.setSection(section);
        assignment.setStaff(staff);
        assignment.setEffectiveFrom(LocalDate.now());
        assignment.setActive(true);
        SectionTeacherAssignmentRepository.save(assignment);
    }

    private void clearSectionTeacher(AcademicSection section) {
        SectionTeacherAssignmentRepository.findBySection_SectionIdAndEffectiveToIsNull(section.getSectionId())
                .ifPresent(existing -> {
                    LocalDate end = LocalDate.now();
                    // CHECK (effective_to IS NULL OR effective_to >= effective_from)
                    if (existing.getEffectiveFrom() != null && end.isBefore(existing.getEffectiveFrom())) {
                        end = existing.getEffectiveFrom();
                    }
                    existing.setEffectiveTo(end);
                    existing.setActive(false);
                    SectionTeacherAssignmentRepository.save(existing);
                });
    }

    private AcademicResource resolveResource(Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        return academicResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic resource not found: " + resourceId));
    }

    private void validateAcademicGroupUniqueness(Long yearId, String name, String code, Long excludeId) {
        boolean codeTaken = excludeId == null
                ? academicGroupRepository.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(yearId, code)
                : academicGroupRepository.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCaseAndAcademicGroupIdNot(yearId, code, excludeId);
        if (codeTaken) {
            throw new BusinessException("Group code already exists in this academic year: " + code);
        }
        boolean nameTaken = excludeId == null
                ? academicGroupRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(yearId, name)
                : academicGroupRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndAcademicGroupIdNot(yearId, name, excludeId);
        if (nameTaken) {
            throw new BusinessException("Group name already exists in this academic year: " + name);
        }
    }

    private void validateSectionUniqueness(Long academicGroupId, String name, String code, Long excludeId) {
        boolean codeTaken = excludeId == null
                ? sectionRepository.existsByAcademicGroup_AcademicGroupIdAndCodeIgnoreCase(academicGroupId, code)
                : sectionRepository.existsByAcademicGroup_AcademicGroupIdAndCodeIgnoreCaseAndSectionIdNot(academicGroupId, code, excludeId);
        if (codeTaken) {
            throw new BusinessException("Section code already exists in this Academic Group: " + code);
        }
        boolean nameTaken = excludeId == null
                ? sectionRepository.existsByAcademicGroup_AcademicGroupIdAndNameIgnoreCase(academicGroupId, name)
                : sectionRepository.existsByAcademicGroup_AcademicGroupIdAndNameIgnoreCaseAndSectionIdNot(academicGroupId, name, excludeId);
        if (nameTaken) {
            throw new BusinessException("Section name already exists in this Academic Group: " + name);
        }
    }

    private String generateUniqueAcademicGroupCode(Long yearId, String name) {
        String base = "AG-" + codeSlug(name, "GROUP");
        return nextFreeCode(base, 50, candidate ->
                academicGroupRepository.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(yearId, candidate));
    }

    private String generateUniqueSectionCode(String academicGroupCode, Long academicGroupId, String sectionName) {
        String base = (StringUtils.hasText(academicGroupCode) ? academicGroupCode : "SEC") + "-" + codeSlug(sectionName, "A");
        return nextFreeCode(base, 50, candidate ->
                sectionRepository.existsByAcademicGroup_AcademicGroupIdAndCodeIgnoreCase(academicGroupId, candidate));
    }

    /** cppends -2, -3, ... until the candidate is free. Bounded so a bad data set cannot loop forever. */
    private static String nextFreeCode(String base, int maxLength, Predicate<String> taken) {
        String first = base.length() > maxLength ? base.substring(0, maxLength) : base;
        if (!taken.test(first)) {
            return first;
        }
        for (int n = 2; n < 1000; n++) {
            String suffix = "-" + n;
            int stemLength = Math.min(first.length(), maxLength - suffix.length());
            String candidate = first.substring(0, stemLength) + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("Unable to generate a unique code for " + base);
    }

    private static String codeSlug(String text, String fallback) {
        String cleaned = text.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return (cleaned.isEmpty() ? fallback : cleaned).toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Section names must be unique within one group request, case-insensitively. */
    private void validateNewSectionNames(List<AcademicSectionRequest> sections) {
        Set<String> seen = new java.util.HashSet<>();
        for (AcademicSectionRequest section : sections) {
            String key = section.getName().trim().toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                throw new BusinessException("Two sections in this group have the same name: " + section.getName().trim());
            }
        }
    }

    private AcademicSection createSectionEntity(AcademicGroup academicGroup, AcademicSectionRequest request) {
        Long academicGroupId = academicGroup.getAcademicGroupId();
        String name = request.getName().trim();
        String code = StringUtils.hasText(request.getCode())
                ? request.getCode().trim().toUpperCase(Locale.ROOT)
                : generateUniqueSectionCode(academicGroup.getCode(), academicGroupId, name);
        validateSectionUniqueness(academicGroupId, name, code, null);

        AcademicSection section = new AcademicSection();
        section.setAcademicGroup(academicGroup);
        section.setName(name);
        section.setCode(code);
        section.setCapacity(request.getCapacity());
        section.setDisplayOrder(request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextSectionDisplayOrder(academicGroupId));
        section.setDefaultResource(resolveResource(request.getDefaultResourceId()));
        section.setActive(true);
        AcademicSection saved = sectionRepository.save(section);

        if (request.getSectionTeacherStaffId() != null) {
            assignSectionTeacher(saved, request.getSectionTeacherStaffId());
        }
        return saved;
    }

    /** Live timetable state for the year: PUBLISHED, DRAFT, or NOT_GENERATED. */
    private String resolveTimetableStatus(Long yearId) {
        if (timetableVersionRepository
                .findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED).isPresent()) {
            return "PUBLISHED";
        }
        boolean hasDrafts = !timetableVersionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId).isEmpty();
        return hasDrafts ? "DRAFT" : "NOT_GENERATED";
    }

    private int nextAcademicGroupDisplayOrder(Long yearId) {
        return academicGroupRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(yearId).stream()
                .map(AcademicGroup::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private int nextSectionDisplayOrder(Long academicGroupId) {
        return sectionRepository.findByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(academicGroupId).stream()
                .map(AcademicSection::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private boolean matchesQuery(AcademicGroup A, String q) {
        if (!StringUtils.hasText(q)) {
            return true;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        return (A.getName() != null && A.getName().toLowerCase(Locale.ROOT).contains(needle))
                || (A.getCode() != null && A.getCode().toLowerCase(Locale.ROOT).contains(needle));
    }

    private boolean isYearReadOnly(AcademicYear year) {
        return READ_ONLY_YEAR_STATUSES.contains(year.getStatus());
    }

    private void assertYearMutable(AcademicYear year) {
        if (isYearReadOnly(year)) {
            throw new BusinessException("Historical academic years are read-only");
        }
        if (!year.isActive()) {
            throw new BusinessException("Cannot modify structure for an inactive academic year");
        }
    }

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private AcademicGroup requireAcademicGroup(Long academicGroupId) {
        return academicGroupRepository.findByIdWithYear(academicGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Group not found: " + academicGroupId));
    }

    private AcademicSection requireSection(Long sectionId) {
        return sectionRepository.findByIdWithAcademicGroup(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
    }

    private String staffDisplayName(Staff staff) {
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return (staff.getFirstName() + middle + " " + staff.getLastName()).trim();
    }
}
