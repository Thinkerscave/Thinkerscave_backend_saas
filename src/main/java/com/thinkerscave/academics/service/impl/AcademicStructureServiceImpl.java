package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicClassRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicClassResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.dto.response.ClassesSectionsDashboardResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicResource;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.ClassTeacherAssignment;
import com.thinkerscave.academics.enums.AcademicStage;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.repository.AcademicResourceRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.ClassTeacherAssignmentRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final ClassTeacherAssignmentRepository classTeacherAssignmentRepository;
    private final AcademicResourceRepository academicResourceRepository;
    private final StaffRepository staffRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public ClassesSectionsDashboardResponse getDashboard(
            Long academicYearId, String q, AcademicStage stage, Boolean active) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicYear year = requireYear(academicYearId);

        List<AcademicClass> classes = classRepository
                .findWithYearByAcademicYearIdOrderByDisplayOrderAsc(academicYearId)
                .stream()
                .filter(c -> stage == null || c.getStage() == stage)
                .filter(c -> active == null || Objects.equals(c.getActive(), active))
                .filter(c -> matchesQuery(c, q))
                .toList();

        List<AcademicClassResponse> classResponses = classes.stream()
                .map(c -> toClassResponse(c, true, false))
                .toList();

        long sectionCount = sectionRepository.countByAcademicClass_AcademicYear_AcademicYearId(academicYearId);
        long sectionsActive = sectionRepository.countByAcademicClass_AcademicYear_AcademicYearIdAndActiveTrue(academicYearId);

        return ClassesSectionsDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .classCount(classRepository.countByAcademicYear_AcademicYearId(academicYearId))
                .classesActive(classRepository.countByAcademicYear_AcademicYearIdAndActiveTrue(academicYearId))
                .sectionCount(sectionCount)
                .sectionsActive(sectionsActive)
                .studentCount(studentEnrollmentRepository.countByAcademicYearAcademicYearIdAndActiveTrue(academicYearId))
                .classes(classResponses)
                .build();
    }

    @Override
    public AcademicClassResponse createClass(AcademicClassRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicYear year = requireYear(request.getAcademicYearId());
        assertYearMutable(year);

        String name = request.getName().trim();
        String code = resolveClassCode(request.getCode(), name);
        validateClassUniqueness(year.getAcademicYearId(), name, code, null);

        AcademicClass entity = new AcademicClass();
        entity.setAcademicYear(year);
        entity.setName(name);
        entity.setCode(code);
        entity.setStage(request.getStage());
        entity.setDisplayOrder(request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextClassDisplayOrder(year.getAcademicYearId()));
        entity.setActive(true);
        return toClassResponse(classRepository.save(entity), true, false);
    }

    @Override
    public AcademicClassResponse updateClass(Long classId, AcademicClassRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicClass entity = requireClass(classId);
        assertYearMutable(entity.getAcademicYear());

        String name = request.getName().trim();
        String code = resolveClassCode(request.getCode(), name);
        validateClassUniqueness(entity.getAcademicYear().getAcademicYearId(), name, code, classId);

        entity.setName(name);
        entity.setCode(code);
        entity.setStage(request.getStage());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        return toClassResponse(classRepository.save(entity), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicClassResponse getClassById(Long classId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        return toClassResponse(requireClass(classId), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicClassResponse> getClassesByYear(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        requireYear(academicYearId);
        return classRepository.findWithYearByAcademicYearIdOrderByDisplayOrderAsc(academicYearId).stream()
                .map(c -> toClassResponse(c, true, false))
                .toList();
    }

    @Override
    public AcademicClassResponse deactivateClass(Long classId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicClass entity = requireClass(classId);
        assertYearMutable(entity.getAcademicYear());
        entity.setActive(false);
        return toClassResponse(classRepository.save(entity), true, false);
    }

    @Override
    public AcademicClassResponse activateClass(Long classId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicClass entity = requireClass(classId);
        assertYearMutable(entity.getAcademicYear());
        entity.setActive(true);
        return toClassResponse(classRepository.save(entity), true, false);
    }

    @Override
    public AcademicSectionResponse createSection(Long classId, AcademicSectionRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicClass academicClass = requireClass(classId);
        assertYearMutable(academicClass.getAcademicYear());

        String name = request.getName().trim();
        String code = resolveSectionCode(request.getCode(), academicClass.getCode(), name);
        validateSectionUniqueness(classId, name, code, null);

        AcademicSection section = new AcademicSection();
        section.setAcademicClass(academicClass);
        section.setName(name);
        section.setCode(code);
        section.setCapacity(request.getCapacity());
        section.setDisplayOrder(request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextSectionDisplayOrder(classId));
        section.setDefaultResource(resolveResource(request.getDefaultResourceId()));
        section.setActive(true);
        AcademicSection saved = sectionRepository.save(section);

        if (request.getClassTeacherStaffId() != null) {
            assignClassTeacher(saved, request.getClassTeacherStaffId());
        }
        return toSectionResponse(saved);
    }

    @Override
    public AcademicSectionResponse updateSection(Long sectionId, AcademicSectionRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicSection section = requireSection(sectionId);
        assertYearMutable(section.getAcademicClass().getAcademicYear());

        String name = request.getName().trim();
        String code = resolveSectionCode(request.getCode(), section.getAcademicClass().getCode(), name);
        validateSectionUniqueness(section.getAcademicClass().getClassId(), name, code, sectionId);

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
        if (request.getClassTeacherStaffId() != null) {
            assignClassTeacher(saved, request.getClassTeacherStaffId());
        } else {
            clearClassTeacher(saved);
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
    public List<AcademicSectionResponse> getSectionsByClass(Long classId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        requireClass(classId);
        return sectionRepository.findWithClassByAcademicClass_ClassIdOrderByDisplayOrderAsc(classId).stream()
                .map(this::toSectionResponse)
                .toList();
    }

    @Override
    public AcademicSectionResponse deactivateSection(Long sectionId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicSection section = requireSection(sectionId);
        assertYearMutable(section.getAcademicClass().getAcademicYear());
        section.setActive(false);
        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    public AcademicSectionResponse activateSection(Long sectionId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CLASSES);
        AcademicSection section = requireSection(sectionId);
        assertYearMutable(section.getAcademicClass().getAcademicYear());
        section.setActive(true);
        return toSectionResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicStructureTreeResponse> getStructureTree(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CLASSES);
        requireYear(academicYearId);

        List<AcademicClass> classes = classRepository
                .findWithYearByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);

        Map<AcademicStage, List<AcademicClass>> byStage = classes.stream()
                .collect(Collectors.groupingBy(
                        AcademicClass::getStage,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<AcademicStructureTreeResponse> tree = new ArrayList<>();
        for (AcademicStage stage : AcademicStage.values()) {
            List<AcademicClass> stageClasses = byStage.getOrDefault(stage, List.of());
            if (stageClasses.isEmpty()) {
                continue;
            }
            List<AcademicStructureTreeResponse.ClassNode> classNodes = stageClasses.stream()
                    .map(c -> AcademicStructureTreeResponse.ClassNode.builder()
                            .classId(c.getClassId())
                            .name(c.getName())
                            .code(c.getCode())
                            .active(c.getActive())
                            .displayOrder(c.getDisplayOrder())
                            .sections(sectionRepository
                                    .findByAcademicClass_ClassIdOrderByDisplayOrderAsc(c.getClassId())
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
                    .classes(classNodes)
                    .build());
        }
        return tree;
    }

    private AcademicClassResponse toClassResponse(AcademicClass entity, boolean withCounts, boolean withSections) {
        AcademicYear year = entity.getAcademicYear();
        long sectionCount = 0;
        long sectionsActive = 0;
        long studentCount = 0;
        List<AcademicSectionResponse> sections = null;

        if (withCounts || withSections) {
            List<AcademicSection> sectionEntities = sectionRepository
                    .findByAcademicClass_ClassIdOrderByDisplayOrderAsc(entity.getClassId());
            sectionCount = sectionEntities.size();
            sectionsActive = sectionEntities.stream().filter(AcademicSection::isActive).count();
            studentCount = studentEnrollmentRepository.countByClassEntityClassIdAndActiveTrue(entity.getClassId());

            if (withSections) {
                sections = sectionEntities.stream().map(this::toSectionResponse).toList();
            }
        }

        // Class Teacher is a Section relationship only — never roll up a single
        // teacher onto the Class response (that implied an incorrect Class→Teacher model).
        return AcademicClassResponse.builder()
                .classId(entity.getClassId())
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .name(entity.getName())
                .code(entity.getCode())
                .stage(entity.getStage())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.getActive())
                .sectionCount(sectionCount)
                .sectionsActive(sectionsActive)
                .studentCount(studentCount)
                .classTeacherName(null)
                .classTeacherStaffId(null)
                .sections(sections)
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    private AcademicSectionResponse toSectionResponse(AcademicSection section) {
        AcademicClass academicClass = section.getAcademicClass();
        ClassTeacherAssignment teacher = classTeacherAssignmentRepository
                .findFirstBySection_SectionIdAndActiveTrueAndEffectiveToIsNullOrderByEffectiveFromDesc(
                        section.getSectionId())
                .orElse(null);

        return AcademicSectionResponse.builder()
                .sectionId(section.getSectionId())
                .classId(academicClass.getClassId())
                .className(academicClass.getName())
                .classCode(academicClass.getCode())
                .name(section.getName())
                .code(section.getCode())
                .capacity(section.getCapacity())
                .displayOrder(section.getDisplayOrder())
                .defaultResourceId(section.getDefaultResource() == null
                        ? null
                        : section.getDefaultResource().getAcademicResourceId())
                .active(section.getActive())
                .studentCount(studentEnrollmentRepository.countBySectionSectionIdAndActiveTrue(section.getSectionId()))
                .classTeacherName(teacher == null || teacher.getStaff() == null
                        ? null
                        : staffDisplayName(teacher.getStaff()))
                .classTeacherStaffId(teacher == null || teacher.getStaff() == null
                        ? null
                        : teacher.getStaff().getStaffId())
                .createdBy(section.getCreatedBy())
                .createdOn(section.getCreatedOn())
                .updatedBy(section.getUpdatedBy())
                .updatedOn(section.getUpdatedOn())
                .build();
    }

    private void assignClassTeacher(AcademicSection section, Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        clearClassTeacher(section);

        ClassTeacherAssignment assignment = new ClassTeacherAssignment();
        assignment.setSection(section);
        assignment.setStaff(staff);
        assignment.setEffectiveFrom(LocalDate.now());
        assignment.setActive(true);
        classTeacherAssignmentRepository.save(assignment);
    }

    private void clearClassTeacher(AcademicSection section) {
        classTeacherAssignmentRepository.findBySection_SectionIdAndEffectiveToIsNull(section.getSectionId())
                .ifPresent(existing -> {
                    LocalDate end = LocalDate.now();
                    // CHECK (effective_to IS NULL OR effective_to >= effective_from)
                    if (existing.getEffectiveFrom() != null && end.isBefore(existing.getEffectiveFrom())) {
                        end = existing.getEffectiveFrom();
                    }
                    existing.setEffectiveTo(end);
                    existing.setActive(false);
                    classTeacherAssignmentRepository.save(existing);
                });
    }

    private AcademicResource resolveResource(Long resourceId) {
        if (resourceId == null) {
            return null;
        }
        return academicResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic resource not found: " + resourceId));
    }

    private void validateClassUniqueness(Long yearId, String name, String code, Long excludeId) {
        boolean codeTaken = excludeId == null
                ? classRepository.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(yearId, code)
                : classRepository.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCaseAndClassIdNot(yearId, code, excludeId);
        if (codeTaken) {
            throw new BusinessException("Class code already exists in this academic year: " + code);
        }
        boolean nameTaken = excludeId == null
                ? classRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(yearId, name)
                : classRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndClassIdNot(yearId, name, excludeId);
        if (nameTaken) {
            throw new BusinessException("Class name already exists in this academic year: " + name);
        }
    }

    private void validateSectionUniqueness(Long classId, String name, String code, Long excludeId) {
        boolean codeTaken = excludeId == null
                ? sectionRepository.existsByAcademicClass_ClassIdAndCodeIgnoreCase(classId, code)
                : sectionRepository.existsByAcademicClass_ClassIdAndCodeIgnoreCaseAndSectionIdNot(classId, code, excludeId);
        if (codeTaken) {
            throw new BusinessException("Section code already exists in this class: " + code);
        }
        boolean nameTaken = excludeId == null
                ? sectionRepository.existsByAcademicClass_ClassIdAndNameIgnoreCase(classId, name)
                : sectionRepository.existsByAcademicClass_ClassIdAndNameIgnoreCaseAndSectionIdNot(classId, name, excludeId);
        if (nameTaken) {
            throw new BusinessException("Section name already exists in this class: " + name);
        }
    }

    private String resolveClassCode(String requested, String name) {
        if (StringUtils.hasText(requested)) {
            return requested.trim().toUpperCase(Locale.ROOT);
        }
        String cleaned = name.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return ("CLS-" + cleaned).toUpperCase(Locale.ROOT);
    }

    private String resolveSectionCode(String requested, String classCode, String sectionName) {
        if (StringUtils.hasText(requested)) {
            return requested.trim().toUpperCase(Locale.ROOT);
        }
        String base = StringUtils.hasText(classCode) ? classCode : "SEC";
        return (base + "-" + sectionName.trim()).toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private int nextClassDisplayOrder(Long yearId) {
        return classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(yearId).stream()
                .map(AcademicClass::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private int nextSectionDisplayOrder(Long classId) {
        return sectionRepository.findByAcademicClass_ClassIdOrderByDisplayOrderAsc(classId).stream()
                .map(AcademicSection::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private boolean matchesQuery(AcademicClass c, String q) {
        if (!StringUtils.hasText(q)) {
            return true;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        return (c.getName() != null && c.getName().toLowerCase(Locale.ROOT).contains(needle))
                || (c.getCode() != null && c.getCode().toLowerCase(Locale.ROOT).contains(needle));
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

    private AcademicClass requireClass(Long classId) {
        return classRepository.findByIdWithYear(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
    }

    private AcademicSection requireSection(Long sectionId) {
        return sectionRepository.findByIdWithClass(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
    }

    private String staffDisplayName(Staff staff) {
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return (staff.getFirstName() + middle + " " + staff.getLastName()).trim();
    }
}
