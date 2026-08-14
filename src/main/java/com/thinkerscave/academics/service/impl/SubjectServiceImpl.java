package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.ClassSubjectMappingRequest;
import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.ClassMappingBoardResponse;
import com.thinkerscave.academics.dto.response.ClassSubjectMappingResponse;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectsMappingDashboardResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.ClassSubjectMapping;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.SubjectCategory;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.ClassSubjectMappingRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.repository.TeacherAllocationRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.SubjectService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED,
            AcademicYearStatus.ARCHIVED
    );

    private final AcademicYearRepository academicYearRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final ClassSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository teacherAllocationRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public SubjectsMappingDashboardResponse getDashboard(
            Long academicYearId, String q, SubjectCategory category, Boolean active) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        AcademicYear year = requireYear(academicYearId);

        List<SubjectResponse> subjects = subjectRepository
                .findWithYearByAcademicYearIdOrderByNameAsc(academicYearId)
                .stream()
                .filter(s -> category == null || s.getCategory() == category)
                .filter(s -> active == null || Objects.equals(s.getActive(), active))
                .filter(s -> matchesQuery(s, q))
                .map(s -> toSubjectResponse(s, true, false))
                .toList();

        return SubjectsMappingDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .classCount(classRepository.countByAcademicYear_AcademicYearId(academicYearId))
                .sectionCount(sectionRepository.countByAcademicClass_AcademicYear_AcademicYearId(academicYearId))
                .subjectCount(subjectRepository.countByAcademicYear_AcademicYearId(academicYearId))
                .subjectsActive(subjectRepository.countByAcademicYear_AcademicYearIdAndActiveTrue(academicYearId))
                .unmappedSubjectCount(mappingRepository.countUnmappedActiveSubjects(academicYearId))
                .subjects(subjects)
                .build();
    }

    @Override
    public SubjectResponse create(SubjectRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        AcademicYear year = requireYear(request.getAcademicYearId());
        assertYearMutable(year);

        String name = request.getName().trim();
        String code = resolveCode(request.getCode(), name);
        validateUniqueness(year.getAcademicYearId(), name, code, null);

        Subject subject = new Subject();
        subject.setAcademicYear(year);
        subject.setName(name);
        subject.setCode(code);
        subject.setCategory(request.getCategory());
        subject.setDefaultWeeklyPeriods(request.getDefaultWeeklyPeriods());
        subject.setTimetablePreference(request.getTimetablePreference() == null
                ? SubjectTimetablePreference.ANY
                : request.getTimetablePreference());
        subject.setDescription(trimToNull(request.getDescription()));
        subject.setActive(true);
        return toSubjectResponse(subjectRepository.save(subject), true, false);
    }

    @Override
    public SubjectResponse update(Long subjectId, SubjectRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        Subject subject = requireSubject(subjectId);
        assertYearMutable(subject.getAcademicYear());

        String name = request.getName().trim();
        String code = resolveCode(request.getCode(), name);
        validateUniqueness(subject.getAcademicYear().getAcademicYearId(), name, code, subjectId);

        subject.setName(name);
        subject.setCode(code);
        subject.setCategory(request.getCategory());
        subject.setDefaultWeeklyPeriods(request.getDefaultWeeklyPeriods());
        subject.setTimetablePreference(request.getTimetablePreference() == null
                ? SubjectTimetablePreference.ANY
                : request.getTimetablePreference());
        subject.setDescription(trimToNull(request.getDescription()));
        return toSubjectResponse(subjectRepository.save(subject), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getById(Long subjectId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        return toSubjectResponse(requireSubject(subjectId), true, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getByYear(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        requireYear(academicYearId);
        return subjectRepository.findWithYearByAcademicYearIdOrderByNameAsc(academicYearId).stream()
                .map(s -> toSubjectResponse(s, true, false))
                .toList();
    }

    @Override
    public SubjectResponse deactivate(Long subjectId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        Subject subject = requireSubject(subjectId);
        assertYearMutable(subject.getAcademicYear());
        subject.setActive(false);
        return toSubjectResponse(subjectRepository.save(subject), true, false);
    }

    @Override
    public SubjectResponse activate(Long subjectId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        Subject subject = requireSubject(subjectId);
        assertYearMutable(subject.getAcademicYear());
        subject.setActive(true);
        return toSubjectResponse(subjectRepository.save(subject), true, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassMappingBoardResponse getClassMappingBoard(Long classId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        AcademicClass academicClass = requireClass(classId);
        Long yearId = academicClass.getAcademicYear().getAcademicYearId();

        List<Subject> subjects = subjectRepository
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByNameAsc(yearId);

        List<ClassSubjectMappingResponse> mappings = subjects.stream()
                .map(subject -> {
                    ClassSubjectMapping existing = mappingRepository
                            .findByAcademicClass_ClassIdAndSubject_SubjectId(classId, subject.getSubjectId())
                            .orElse(null);
                    return toMappingResponse(academicClass, subject, existing);
                })
                .toList();

        List<String> sectionNames = sectionRepository
                .findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(classId)
                .stream()
                .map(AcademicSection::getName)
                .toList();

        long included = mappings.stream().filter(ClassSubjectMappingResponse::isIncluded).count();
        long missingTeachers = mappings.stream()
                .filter(ClassSubjectMappingResponse::isIncluded)
                .filter(m -> "MISSING".equals(m.getTeacherStatus()))
                .count();

        return ClassMappingBoardResponse.builder()
                .classId(academicClass.getClassId())
                .className(academicClass.getName())
                .classCode(academicClass.getCode())
                .stage(academicClass.getStage())
                .sectionNames(sectionNames)
                .mappings(mappings)
                .includedCount(included)
                .missingTeacherCount(missingTeachers)
                .build();
    }

    @Override
    public ClassSubjectMappingResponse upsertClassMapping(Long classId, ClassSubjectMappingRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        AcademicClass academicClass = requireClass(classId);
        assertYearMutable(academicClass.getAcademicYear());

        Subject subject = requireSubject(request.getSubjectId());
        if (!Objects.equals(subject.getAcademicYear().getAcademicYearId(),
                academicClass.getAcademicYear().getAcademicYearId())) {
            throw new BusinessException("Subject and class must belong to the same academic year");
        }
        if (!subject.isActive() && Boolean.TRUE.equals(request.getIncluded())) {
            throw new BusinessException("Cannot map an inactive subject");
        }

        ClassSubjectMapping mapping = mappingRepository
                .findByAcademicClass_ClassIdAndSubject_SubjectId(classId, subject.getSubjectId())
                .orElseGet(ClassSubjectMapping::new);

        if (mapping.getClassSubjectMappingId() == null) {
            mapping.setAcademicClass(academicClass);
            mapping.setSubject(subject);
        }

        boolean included = Boolean.TRUE.equals(request.getIncluded());
        mapping.setActive(included);
        Short weekly = request.getWeeklyPeriods() != null
                ? request.getWeeklyPeriods()
                : (mapping.getWeeklyPeriods() != null
                ? mapping.getWeeklyPeriods()
                : subject.getDefaultWeeklyPeriods());
        mapping.setWeeklyPeriods(weekly);
        mapping.setTimetablePreference(request.getTimetablePreference() != null
                ? request.getTimetablePreference()
                : (mapping.getTimetablePreference() != null
                ? mapping.getTimetablePreference()
                : subject.getTimetablePreference()));

        ClassSubjectMapping saved = mappingRepository.save(mapping);
        return toMappingResponse(academicClass, subject, saved);
    }

    private SubjectResponse toSubjectResponse(Subject subject, boolean withCounts, boolean withMappings) {
        AcademicYear year = subject.getAcademicYear();
        long mappedClassCount = 0;
        long teacherAllocationCount = 0;
        List<ClassSubjectMappingResponse> mappings = null;

        if (withCounts || withMappings) {
            List<ClassSubjectMapping> subjectMappings = mappingRepository
                    .findBySubject_SubjectIdAndActiveTrue(subject.getSubjectId());
            mappedClassCount = subjectMappings.size();
            teacherAllocationCount = subjectMappings.stream()
                    .mapToLong(m -> teacherAllocationRepository
                            .countByClassSubjectMapping_ClassSubjectMappingId(m.getClassSubjectMappingId()))
                    .sum();
            if (withMappings) {
                mappings = subjectMappings.stream()
                        .map(m -> toMappingResponse(m.getAcademicClass(), subject, m))
                        .toList();
            }
        }

        return SubjectResponse.builder()
                .subjectId(subject.getSubjectId())
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .name(subject.getName())
                .code(subject.getCode())
                .category(subject.getCategory())
                .defaultWeeklyPeriods(subject.getDefaultWeeklyPeriods())
                .timetablePreference(subject.getTimetablePreference())
                .description(subject.getDescription())
                .active(subject.getActive())
                .mappedClassCount(mappedClassCount)
                .teacherAllocationCount(teacherAllocationCount)
                .mappings(mappings)
                .createdBy(subject.getCreatedBy())
                .createdOn(subject.getCreatedOn())
                .updatedBy(subject.getUpdatedBy())
                .updatedOn(subject.getUpdatedOn())
                .build();
    }

    private ClassSubjectMappingResponse toMappingResponse(
            AcademicClass academicClass, Subject subject, ClassSubjectMapping mapping) {
        boolean included = mapping != null && Boolean.TRUE.equals(mapping.getActive());
        Short weekly = mapping != null && mapping.getWeeklyPeriods() != null
                ? mapping.getWeeklyPeriods()
                : subject.getDefaultWeeklyPeriods();
        SubjectTimetablePreference preference = mapping != null && mapping.getTimetablePreference() != null
                ? mapping.getTimetablePreference()
                : subject.getTimetablePreference();
        boolean overridden = mapping != null
                && mapping.getWeeklyPeriods() != null
                && !Objects.equals(mapping.getWeeklyPeriods(), subject.getDefaultWeeklyPeriods());

        long allocationCount = mapping == null
                ? 0
                : teacherAllocationRepository.countByClassSubjectMapping_ClassSubjectMappingId(
                mapping.getClassSubjectMappingId());
        long assignedCount = mapping == null
                ? 0
                : teacherAllocationRepository.countByClassSubjectMapping_ClassSubjectMappingIdAndStatus(
                mapping.getClassSubjectMappingId(), TeacherAllocationStatus.ASSIGNED);

        String teacherStatus = "NONE";
        if (included) {
            teacherStatus = assignedCount > 0 ? "ASSIGNED" : "MISSING";
        }

        return ClassSubjectMappingResponse.builder()
                .classSubjectMappingId(mapping == null ? null : mapping.getClassSubjectMappingId())
                .classId(academicClass.getClassId())
                .className(academicClass.getName())
                .classCode(academicClass.getCode())
                .subjectId(subject.getSubjectId())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .category(subject.getCategory())
                .included(included)
                .weeklyPeriods(weekly)
                .defaultWeeklyPeriods(subject.getDefaultWeeklyPeriods())
                .periodsOverridden(overridden)
                .timetablePreference(preference)
                .active(mapping == null ? null : mapping.getActive())
                .teacherStatus(teacherStatus)
                .teacherAllocationCount(allocationCount)
                .build();
    }

    private void validateUniqueness(Long yearId, String name, String code, Long excludeId) {
        boolean codeTaken = excludeId == null
                ? subjectRepository.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(yearId, code)
                : subjectRepository.existsByAcademicYear_AcademicYearIdAndCodeIgnoreCaseAndSubjectIdNot(
                yearId, code, excludeId);
        if (codeTaken) {
            throw new BusinessException("Subject code already exists in this academic year: " + code);
        }
        boolean nameTaken = excludeId == null
                ? subjectRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(yearId, name)
                : subjectRepository.existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndSubjectIdNot(
                yearId, name, excludeId);
        if (nameTaken) {
            throw new BusinessException("Subject name already exists in this academic year: " + name);
        }
    }

    private String resolveCode(String requested, String name) {
        if (StringUtils.hasText(requested)) {
            return requested.trim().toUpperCase(Locale.ROOT);
        }
        String letters = name.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (letters.length() >= 3) {
            return letters.substring(0, 3);
        }
        return (letters + "SUB").substring(0, Math.min(3, letters.length() + 3)).toUpperCase(Locale.ROOT);
    }

    private boolean matchesQuery(Subject subject, String q) {
        if (!StringUtils.hasText(q)) {
            return true;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        return (subject.getName() != null && subject.getName().toLowerCase(Locale.ROOT).contains(needle))
                || (subject.getCode() != null && subject.getCode().toLowerCase(Locale.ROOT).contains(needle));
    }

    private boolean isYearReadOnly(AcademicYear year) {
        return READ_ONLY_YEAR_STATUSES.contains(year.getStatus());
    }

    private void assertYearMutable(AcademicYear year) {
        if (isYearReadOnly(year)) {
            throw new BusinessException("Historical academic years are read-only");
        }
        if (!year.isActive()) {
            throw new BusinessException("Cannot modify subjects for an inactive academic year");
        }
    }

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private Subject requireSubject(Long subjectId) {
        return subjectRepository.findByIdWithYear(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
    }

    private AcademicClass requireClass(Long classId) {
        return classRepository.findByIdWithYear(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
