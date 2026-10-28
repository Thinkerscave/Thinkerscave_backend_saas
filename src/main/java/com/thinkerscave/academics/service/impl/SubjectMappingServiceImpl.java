package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectMappingEntryRequest;
import com.thinkerscave.academics.dto.request.SubjectMappingSaveRequest;
import com.thinkerscave.academics.dto.response.AvailableSubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingBoardResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingImpactResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingRowResponse;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.repository.AcademicGroupRepository;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.IdCount;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.SubjectMappingService;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ConflictException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectMappingServiceImpl implements SubjectMappingService {

    static final short MIN_WEEKLY_PERIODS = 1;
    static final short MAX_WEEKLY_PERIODS = 40;

    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED,
            AcademicYearStatus.ARCHIVED
    );

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AcademicGroupRepository academicGroupRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicGroupSubjectMappingRepository mappingRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public SubjectMappingBoardResponse getBoard(Long academicGroupId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        AcademicGroup group = requireGroup(academicGroupId);
        List<AcademicGroupSubjectMapping> mappings = mappingRepository.findActiveWithSubjectByAcademicGroupId(academicGroupId);
        return buildBoard(group, mappings);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AvailableSubjectResponse> getAvailableSubjects(
            Long academicGroupId, String q, SubjectType subjectType, int page, int size) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        requireGroup(academicGroupId);

        Map<Long, AcademicGroupSubjectMapping> mappedBySubject = mappingRepository
                .findByAcademicGroup_AcademicGroupIdAndActiveTrue(academicGroupId).stream()
                .collect(Collectors.toMap(m -> m.getSubject().getSubjectId(), m -> m, (a, b) -> a));
        String pattern = SubjectInputRules.containsPattern(q);

        Specification<Subject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate offered = cb.equal(root.get("active"), true);
            if (!mappedBySubject.isEmpty()) {
                offered = cb.or(offered, root.get("subjectId").in(mappedBySubject.keySet()));
            }
            predicates.add(offered);
            if (pattern != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '!'),
                        cb.like(cb.lower(root.get("code")), pattern, '!')));
            }
            if (subjectType != null) {
                predicates.add(cb.equal(root.get("subjectType"), subjectType));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Subject> result = subjectRepository.findAll(spec, PageRequest.of(
                Math.max(page, 0), clampSize(size), Sort.by("name").ascending().and(Sort.by("subjectId"))));
        return PageResponse.of(result, subject -> {
            AcademicGroupSubjectMapping mapping = mappedBySubject.get(subject.getSubjectId());
            return AvailableSubjectResponse.builder()
                    .subjectId(subject.getSubjectId())
                    .subjectName(subject.getName())
                    .subjectCode(subject.getCode())
                    .subjectType(subject.getSubjectType())
                    .defaultTimetablePreference(subject.getDefaultTimetablePreference())
                    .subjectActive(subject.isActive())
                    .mapped(mapping != null)
                    .mappingId(mapping == null ? null : mapping.getAcademicGroupSubjectMappingId())
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectMappingImpactResponse preview(Long academicGroupId, SubjectMappingSaveRequest request) {
        return toImpactResponse(plan(academicGroupId, request));
    }

    @Override
    public SubjectMappingBoardResponse save(Long academicGroupId, SubjectMappingSaveRequest request) {
        Plan plan = plan(academicGroupId, request);
        if (!plan.impacts().isEmpty() && !request.isConfirmImpact()) {
            throw new ConflictException("This change affects existing teacher assignments or timetable lessons. "
                    + "Review the impact and confirm to continue.");
        }
        apply(plan);
        return getBoard(academicGroupId);
    }

    private Plan plan(Long academicGroupId, SubjectMappingSaveRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        AcademicGroup group = requireGroup(academicGroupId);
        AcademicYear year = group.getAcademicYear();
        if (request.getAcademicYearId() != null && !Objects.equals(request.getAcademicYearId(), year.getAcademicYearId())) {
            throw new BusinessException("This academic group belongs to a different academic year.");
        }
        assertYearMutable(year);

        List<SubjectMappingEntryRequest> upserts = request.getUpserts() == null ? List.of() : request.getUpserts();
        List<Long> removedIds = request.getRemovedSubjectIds() == null ? List.of()
                : request.getRemovedSubjectIds().stream().filter(Objects::nonNull).distinct().toList();
        if (upserts.isEmpty() && removedIds.isEmpty()) {
            throw new BusinessException("There are no subject mapping changes to save.");
        }

        Set<Long> upsertIds = new HashSet<>();
        for (SubjectMappingEntryRequest entry : upserts) {
            if (entry.getSubjectId() == null || !upsertIds.add(entry.getSubjectId())) {
                throw new BusinessException("Each subject can appear only once in a save.");
            }
        }
        for (Long removedId : removedIds) {
            if (upsertIds.contains(removedId)) {
                throw new BusinessException("A subject cannot be added and removed in the same save.");
            }
        }

        Map<Long, AcademicGroupSubjectMapping> existingBySubject = mappingRepository
                .findByAcademicGroup_AcademicGroupId(academicGroupId).stream()
                .collect(Collectors.toMap(m -> m.getSubject().getSubjectId(), m -> m, (a, b) -> a));

        Set<Long> allSubjectIds = new HashSet<>(upsertIds);
        allSubjectIds.addAll(removedIds);
        Map<Long, Subject> subjects = subjectRepository.findAllById(allSubjectIds).stream()
                .collect(Collectors.toMap(Subject::getSubjectId, s -> s));
        for (Long subjectId : allSubjectIds) {
            if (!subjects.containsKey(subjectId)) {
                throw new ResourceNotFoundException("Subject not found: " + subjectId);
            }
        }

        List<Long> existingMappingIds = allSubjectIds.stream()
                .map(existingBySubject::get)
                .filter(Objects::nonNull)
                .map(AcademicGroupSubjectMapping::getAcademicGroupSubjectMappingId)
                .toList();
        Map<Long, Long> allocationCounts = countsOf(mappingRepository.countActiveAllocationsByMappingIds(existingMappingIds));
        Map<Long, Long> entryCounts = countsOf(mappingRepository.countTimetableEntriesByMappingIds(existingMappingIds));

        List<PlannedUpsert> planned = new ArrayList<>();
        List<SubjectMappingImpactResponse.Impact> impacts = new ArrayList<>();

        for (SubjectMappingEntryRequest entry : upserts) {
            Subject subject = subjects.get(entry.getSubjectId());
            AcademicGroupSubjectMapping existing = existingBySubject.get(subject.getSubjectId());
            boolean existingActive = existing != null && existing.isActive();
            if (!subject.isActive() && !existingActive) {
                throw new BusinessException("'" + subject.getName()
                        + "' is inactive. Reactivate it in Subjects before adding it to a curriculum.");
            }

            short weeklyPeriods = validateWeeklyPeriods(entry.getWeeklyPeriods(), subject.getName());
            SubjectRequirement requirement = entry.getRequirement() == null
                    ? SubjectRequirement.COMPULSORY : entry.getRequirement();
            SubjectTimetablePreference preference = entry.getTimetablePreference() == null
                    ? SubjectTimetablePreference.ANY : entry.getTimetablePreference();
            String choiceGroup = entry.getChoiceGroup() == null ? null : entry.getChoiceGroup().trim();
            if (choiceGroup != null && choiceGroup.isEmpty()) choiceGroup = null;
            if (choiceGroup != null && requirement != SubjectRequirement.OPTIONAL) {
                throw new BusinessException("Choice group can only be set for optional subjects.");
            }
            short consecutivePeriods = entry.getConsecutivePeriods() == null ? 1 : entry.getConsecutivePeriods();
            if (consecutivePeriods < 1 || consecutivePeriods > 10) {
                throw new BusinessException("Consecutive periods must be between 1 and 10.");
            }
            if (existingActive) {
                boolean scheduleChanged = !Objects.equals(existing.getWeeklyPeriods(), weeklyPeriods)
                        || existing.getRequirement() != requirement
                        || existing.getTimetablePreference() != preference
                        || !Objects.equals(existing.getChoiceGroup(), choiceGroup)
                        || !Objects.equals(existing.getConsecutivePeriods(), consecutivePeriods);
                long entries = entryCounts.getOrDefault(existing.getAcademicGroupSubjectMappingId(), 0L);
                if (scheduleChanged && entries > 0) {
                    impacts.add(impact(subject, "SCHEDULE_CHANGE",
                            allocationCounts.getOrDefault(existing.getAcademicGroupSubjectMappingId(), 0L), entries,
                            "Changing the schedule of '" + subject.getName() + "' affects " + entries
                                    + " timetable lesson(s). Review the timetable after saving."));
                }
            }

            planned.add(new PlannedUpsert(subject, existing, requirement, weeklyPeriods, preference,
                    choiceGroup, consecutivePeriods));
        }

        List<PlannedRemoval> removals = new ArrayList<>();
        for (Long subjectId : removedIds) {
            Subject subject = subjects.get(subjectId);
            AcademicGroupSubjectMapping existing = existingBySubject.get(subjectId);
            if (existing == null || !existing.isActive()) {
                throw new BusinessException("'" + subject.getName() + "' is not mapped to this academic group.");
            }
            long allocations = allocationCounts.getOrDefault(existing.getAcademicGroupSubjectMappingId(), 0L);
            long entries = entryCounts.getOrDefault(existing.getAcademicGroupSubjectMappingId(), 0L);
            if (allocations > 0 || entries > 0) {
                impacts.add(impact(subject, "REMOVAL", allocations, entries,
                        "Removing '" + subject.getName() + "' keeps " + allocations + " teacher assignment(s) and "
                                + entries + " timetable lesson(s) as history. They stop being used for scheduling."));
            }
            removals.add(new PlannedRemoval(subject, existing));
        }

        return new Plan(group, planned, removals, impacts);
    }

    private void apply(Plan plan) {
        for (PlannedUpsert upsert : plan.upserts()) {
            AcademicGroupSubjectMapping mapping = upsert.existing() != null
                    ? upsert.existing() : newMapping(plan.group(), upsert.subject());
            mapping.setRequirement(upsert.requirement());
            mapping.setWeeklyPeriods(upsert.weeklyPeriods());
            mapping.setTimetablePreference(upsert.preference());
            mapping.setChoiceGroup(upsert.choiceGroup());
            mapping.setConsecutivePeriods(upsert.consecutivePeriods());
            mapping.setActive(true);
            mappingRepository.save(mapping);
        }
        for (PlannedRemoval removal : plan.removals()) {
            removal.existing().setActive(false);
            mappingRepository.save(removal.existing());
        }
    }

    private AcademicGroupSubjectMapping newMapping(AcademicGroup group, Subject subject) {
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setAcademicGroup(group);
        mapping.setSubject(subject);
        return mapping;
    }

    private short validateWeeklyPeriods(Short value, String subjectName) {
        if (value == null) {
            throw new BusinessException("Weekly periods are required for '" + subjectName + "'.");
        }
        if (value < MIN_WEEKLY_PERIODS || value > MAX_WEEKLY_PERIODS) {
            throw new BusinessException("Weekly periods for '" + subjectName + "' must be between "
                    + MIN_WEEKLY_PERIODS + " and " + MAX_WEEKLY_PERIODS + ".");
        }
        return value;
    }

    private SubjectMappingBoardResponse buildBoard(AcademicGroup group, List<AcademicGroupSubjectMapping> mappings) {
        List<Long> mappingIds = mappings.stream().map(AcademicGroupSubjectMapping::getAcademicGroupSubjectMappingId).toList();
        Map<Long, Long> allocationCounts = countsOf(mappingRepository.countActiveAllocationsByMappingIds(mappingIds));
        Map<Long, Long> entryCounts = countsOf(mappingRepository.countTimetableEntriesByMappingIds(mappingIds));

        List<SubjectMappingRowResponse> rows = mappings.stream()
                .sorted(Comparator.comparing((AcademicGroupSubjectMapping m) -> m.getSubject().getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(mapping -> {
                    Long id = mapping.getAcademicGroupSubjectMappingId();
                    Subject subject = mapping.getSubject();
                    return SubjectMappingRowResponse.builder()
                            .mappingId(id)
                            .subjectId(subject.getSubjectId())
                            .subjectName(subject.getName())
                            .subjectCode(subject.getCode())
                            .subjectType(subject.getSubjectType())
                            .subjectActive(subject.isActive())
                            .requirement(mapping.getRequirement())
                            .weeklyPeriods(mapping.getWeeklyPeriods())
                            .timetablePreference(mapping.getTimetablePreference())
                            .choiceGroup(mapping.getChoiceGroup())
                            .consecutivePeriods(mapping.getConsecutivePeriods())
                            .teacherAllocationCount(allocationCounts.getOrDefault(id, 0L))
                            .timetableEntryCount(entryCounts.getOrDefault(id, 0L))
                            .build();
                })
                .toList();

        AcademicYear year = group.getAcademicYear();
        return SubjectMappingBoardResponse.builder()
                .academicGroupId(group.getAcademicGroupId())
                .academicGroupName(group.getName())
                .academicGroupCode(group.getCode())
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .mappings(rows)
                .mappedCount(rows.size())
                .compulsoryCount(rows.stream().filter(r -> r.getRequirement() == SubjectRequirement.COMPULSORY).count())
                .optionalCount(rows.stream().filter(r -> r.getRequirement() == SubjectRequirement.OPTIONAL).count())
                .teacherPendingCount(rows.stream().filter(r -> r.getTeacherAllocationCount() == 0).count())
                .build();
    }

    private SubjectMappingImpactResponse toImpactResponse(Plan plan) {
        return SubjectMappingImpactResponse.builder()
                .valid(true)
                .requiresConfirmation(!plan.impacts().isEmpty())
                .impacts(plan.impacts())
                .build();
    }

    private SubjectMappingImpactResponse.Impact impact(Subject subject, String type, long allocations,
                                                       long entries, String message) {
        return SubjectMappingImpactResponse.Impact.builder()
                .subjectId(subject.getSubjectId())
                .subjectName(subject.getName())
                .type(type)
                .teacherAllocationCount(allocations)
                .timetableEntryCount(entries)
                .message(message)
                .build();
    }

    private Map<Long, Long> countsOf(List<IdCount> rows) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (IdCount row : rows) {
            counts.put(row.getId(), row.getTotal());
        }
        return counts;
    }

    private AcademicGroup requireGroup(Long academicGroupId) {
        return academicGroupRepository.findByIdWithYear(academicGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic Group not found: " + academicGroupId));
    }

    private void assertYearMutable(AcademicYear year) {
        if (isYearReadOnly(year)) {
            throw new BusinessException("Historical academic years are read-only");
        }
        if (!year.isActive()) {
            throw new BusinessException("Cannot modify curriculum for an inactive academic year");
        }
    }

    private boolean isYearReadOnly(AcademicYear year) {
        return READ_ONLY_YEAR_STATUSES.contains(year.getStatus());
    }

    private int clampSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private record PlannedUpsert(Subject subject, AcademicGroupSubjectMapping existing,
                                 SubjectRequirement requirement, short weeklyPeriods,
                                 SubjectTimetablePreference preference, String choiceGroup,
                                 short consecutivePeriods) {
    }

    private record PlannedRemoval(Subject subject, AcademicGroupSubjectMapping existing) {
    }

    private record Plan(AcademicGroup group, List<PlannedUpsert> upserts, List<PlannedRemoval> removals,
                        List<SubjectMappingImpactResponse.Impact> impacts) {
    }
}
