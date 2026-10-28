package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectUsageResponse;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.SubjectService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final SubjectRepository subjectRepository;
    private final AcademicGroupSubjectMappingRepository mappingRepository;
    private final SubjectMappingCoverageLoader coverageLoader;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> getDirectory(String q, SubjectType subjectType, Boolean active, int page, int size) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        Page<Subject> result = subjectRepository.findAll(
                directoryFilter(q, subjectType, active), pageRequest(page, size));
        Map<Long, UsageSummary> usage = usageSummaries(result.getContent().stream().map(Subject::getSubjectId).toList());
        return PageResponse.of(result, subject ->
                toResponse(subject, usage.getOrDefault(subject.getSubjectId(), UsageSummary.NONE)));
    }

    @Override
    public SubjectResponse create(SubjectRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        String name = SubjectInputRules.requireName(request.getName());
        String code = StringUtils.hasText(request.getCode())
                ? SubjectInputRules.normalizeCode(request.getCode())
                : generateCode(name);
        assertUnique(name, code, null);

        Subject subject = new Subject();
        subject.setName(name);
        subject.setCode(code);
        subject.setSubjectType(requireType(request.getSubjectType()));
        subject.setDescription(SubjectInputRules.cleanDescription(request.getDescription()));
        subject.setDefaultTimetablePreference(request.getDefaultTimetablePreference());
        subject.setActive(true);
        return toResponse(saveWithUniqueGuard(subject), UsageSummary.NONE);
    }

    @Override
    public SubjectResponse update(Long subjectId, SubjectRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        Subject subject = requireSubject(subjectId);
        String name = SubjectInputRules.requireName(request.getName());
        String code = StringUtils.hasText(request.getCode())
                ? SubjectInputRules.normalizeCode(request.getCode())
                : subject.getCode();
        assertUnique(name, code, subjectId);

        subject.setName(name);
        subject.setCode(code);
        subject.setSubjectType(requireType(request.getSubjectType()));
        subject.setDescription(SubjectInputRules.cleanDescription(request.getDescription()));
        subject.setDefaultTimetablePreference(request.getDefaultTimetablePreference());
        return toResponse(saveWithUniqueGuard(subject), usageOf(subjectId));
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getById(Long subjectId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        return toResponse(requireSubject(subjectId), usageOf(subjectId));
    }

    @Override
    public SubjectResponse deactivate(Long subjectId) {
        return setActive(subjectId, false);
    }

    @Override
    public SubjectResponse activate(Long subjectId) {
        return setActive(subjectId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectUsageResponse> getUsages(Long subjectId, String q, int page, int size) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        requireSubject(subjectId);
        String pattern = SubjectInputRules.containsPattern(q);
        Page<AcademicGroupSubjectMapping> result = mappingRepository.searchActiveBySubject(
                subjectId, pattern == null ? "%" : pattern, PageRequest.of(clampPage(page), clampSize(size)));

        List<AcademicGroupSubjectMapping> mappings = result.getContent();
        Map<Long, Integer> activeSections = coverageLoader.activeSectionCounts(mappings.stream()
                .map(m -> m.getAcademicGroup().getAcademicGroupId()).distinct().toList());

        return PageResponse.of(result, mapping -> {
            var group = mapping.getAcademicGroup();
            var year = group.getAcademicYear();
            int sectionCount = activeSections.getOrDefault(group.getAcademicGroupId(), 0);
            return SubjectUsageResponse.builder()
                    .mappingId(mapping.getAcademicGroupSubjectMappingId())
                    .academicGroupId(group.getAcademicGroupId())
                    .academicGroupName(group.getName())
                    .academicGroupCode(group.getCode())
                    .academicYearId(year.getAcademicYearId())
                    .academicYearName(year.getName())
                    .academicYearStatus(year.getStatus())
                    .requirement(mapping.getRequirement())
                    .weeklyPeriods(mapping.getWeeklyPeriods())
                    .timetablePreference(mapping.getTimetablePreference())
                    .sectionCount(sectionCount)
                    .totalSectionCount(sectionCount)
                    .active(mapping.isActive())
                    .build();
        });
    }

    private SubjectResponse setActive(Long subjectId, boolean active) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        Subject subject = requireSubject(subjectId);
        subject.setActive(active);
        return toResponse(saveWithUniqueGuard(subject), usageOf(subjectId));
    }

    private Specification<Subject> directoryFilter(String q, SubjectType subjectType, Boolean active) {
        String pattern = SubjectInputRules.containsPattern(q);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (pattern != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '!'),
                        cb.like(cb.lower(root.get("code")), pattern, '!')));
            }
            if (subjectType != null) {
                predicates.add(cb.equal(root.get("subjectType"), subjectType));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Active usage per subject, computed with one mapping query and two batch count queries. */
    private Map<Long, UsageSummary> usageSummaries(Collection<Long> subjectIds) {
        if (subjectIds.isEmpty()) return Map.of();
        List<AcademicGroupSubjectMapping> mappings = mappingRepository.findActiveWithGroupBySubjectIds(subjectIds);
        Map<Long, Integer> activeSections = coverageLoader.activeSectionCounts(mappings.stream()
                .map(m -> m.getAcademicGroup().getAcademicGroupId()).distinct().toList());

        Map<Long, UsageSummary> summaries = new HashMap<>();
        for (AcademicGroupSubjectMapping mapping : mappings) {
            int covered = activeSections.getOrDefault(mapping.getAcademicGroup().getAcademicGroupId(), 0);
            summaries.merge(mapping.getSubject().getSubjectId(), new UsageSummary(1, covered),
                    (a, b) -> new UsageSummary(a.groups() + b.groups(), a.sections() + b.sections()));
        }
        return summaries;
    }

    private UsageSummary usageOf(Long subjectId) {
        return usageSummaries(List.of(subjectId)).getOrDefault(subjectId, UsageSummary.NONE);
    }

    private SubjectResponse toResponse(Subject subject, UsageSummary usage) {
        return SubjectResponse.builder()
                .subjectId(subject.getSubjectId())
                .name(subject.getName())
                .code(subject.getCode())
                .subjectType(subject.getSubjectType())
                .description(subject.getDescription())
                .defaultTimetablePreference(subject.getDefaultTimetablePreference())
                .active(subject.getActive())
                .mappedAcademicGroupCount(usage.groups())
                .mappedSectionCount(usage.sections())
                .createdBy(subject.getCreatedBy())
                .createdOn(subject.getCreatedOn())
                .updatedBy(subject.getUpdatedBy())
                .updatedOn(subject.getUpdatedOn())
                .build();
    }

    private void assertUnique(String name, String code, Long excludeSubjectId) {
        boolean codeTaken = excludeSubjectId == null
                ? subjectRepository.existsByCodeIgnoreCase(code)
                : subjectRepository.existsByCodeIgnoreCaseAndSubjectIdNot(code, excludeSubjectId);
        if (codeTaken) {
            throw new AlreadyExistsException("Subject code " + code + " is already used by another subject", "code");
        }
        boolean nameTaken = excludeSubjectId == null
                ? subjectRepository.existsByNameIgnoreCase(name)
                : subjectRepository.existsByNameIgnoreCaseAndSubjectIdNot(name, excludeSubjectId);
        if (nameTaken) {
            throw new AlreadyExistsException("A subject named " + name + " already exists", "name");
        }
    }

    private Subject saveWithUniqueGuard(Subject subject) {
        try {
            return subjectRepository.saveAndFlush(subject);
        } catch (DataIntegrityViolationException ex) {
            throw new AlreadyExistsException("A subject with this name or code already exists");
        }
    }

    private String generateCode(String name) {
        String letters = name.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        String base = letters.length() >= 3 ? letters.substring(0, 3) : (letters + "SUB").substring(0, 3);
        String candidate = base;
        int suffix = 2;
        while (subjectRepository.existsByCodeIgnoreCase(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private SubjectType requireType(SubjectType type) {
        if (type == null) throw new BusinessException("Subject type is mandatory");
        return type;
    }

    private Subject requireSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(clampPage(page), clampSize(size),
                Sort.by("name").ascending().and(Sort.by("subjectId")));
    }

    private int clampPage(int page) {
        return Math.max(page, 0);
    }

    private int clampSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private record UsageSummary(long groups, long sections) {
        static final UsageSummary NONE = new UsageSummary(0, 0);
    }
}
