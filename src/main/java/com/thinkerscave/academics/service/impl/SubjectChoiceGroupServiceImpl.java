package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectChoiceGroupRequest;
import com.thinkerscave.academics.dto.response.SubjectChoiceGroupResponse;
import com.thinkerscave.academics.dto.response.SubjectChoiceGroupSubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectOptionResponse;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.SubjectChoiceGroup;
import com.thinkerscave.academics.entity.SubjectChoiceGroupMember;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.repository.AcademicGroupChoiceMappingRepository;
import com.thinkerscave.academics.repository.SubjectChoiceGroupMemberRepository;
import com.thinkerscave.academics.repository.SubjectChoiceGroupRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ConflictException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectChoiceGroupServiceImpl implements com.thinkerscave.academics.service.SubjectChoiceGroupService {

    static final int NAME_MAX = 100;
    static final int MIN_MEMBERS = 2;
    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 10;

    private final SubjectChoiceGroupRepository groupRepository;
    private final SubjectChoiceGroupMemberRepository memberRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicGroupChoiceMappingRepository choiceMappingRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectChoiceGroupResponse> getDirectory(
            String q, SubjectType subjectType, Boolean active, int page, int size) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        Page<SubjectChoiceGroup> groups = groupRepository.findAll(
                directoryFilter(q, subjectType, active),
                PageRequest.of(Math.max(0, page), clampSize(size), Sort.by("name").ascending()
                        .and(Sort.by("subjectChoiceGroupId"))));
        Map<Long, List<SubjectChoiceGroupMember>> membersByGroup =
                membersByGroup(groups.getContent().stream()
                        .map(SubjectChoiceGroup::getSubjectChoiceGroupId).toList());
        return PageResponse.of(groups, group -> toResponse(
                group, membersByGroup.getOrDefault(group.getSubjectChoiceGroupId(), List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubjectOptionResponse> getSubjectOptions(
            String q, SubjectType subjectType, int page, int size) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        if (subjectType == null) throw new BusinessException("Select a subject type.");
        String pattern = SubjectInputRules.containsPattern(q);
        Specification<Subject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("active"), true));
            predicates.add(cb.equal(root.get("subjectType"), subjectType));
            if (pattern != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '!'),
                        cb.like(cb.lower(root.get("code")), pattern, '!')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Subject> subjects = subjectRepository.findAll(spec, PageRequest.of(
                Math.max(0, page), clampSize(size), Sort.by("name").ascending().and(Sort.by("subjectId"))));
        return PageResponse.of(subjects, subject -> SubjectOptionResponse.builder()
                .subjectId(subject.getSubjectId())
                .name(subject.getName())
                .code(subject.getCode())
                .subjectType(subject.getSubjectType())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectChoiceGroupResponse getById(Long groupId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        SubjectChoiceGroup group = requireGroup(groupId);
        return toResponse(group, memberRepository.findByChoiceGroup_SubjectChoiceGroupId(groupId));
    }

    @Override
    public SubjectChoiceGroupResponse create(SubjectChoiceGroupRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        String name = requireName(request.getName());
        assertUniqueName(name, null);
        List<Subject> subjects = requireEligibleSubjects(request.getSubjectIds(), request.getSubjectType());

        SubjectChoiceGroup group = new SubjectChoiceGroup();
        group.setName(name);
        group.setSubjectType(request.getSubjectType());
        group.setActive(true);
        try {
            group = groupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A choice group with this name already exists.");
        }
        saveMembers(group, subjects);
        return toResponse(group, memberRepository.findByChoiceGroup_SubjectChoiceGroupId(group.getSubjectChoiceGroupId()));
    }

    @Override
    public SubjectChoiceGroupResponse update(Long groupId, SubjectChoiceGroupRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        SubjectChoiceGroup group = requireGroup(groupId);
        String name = requireName(request.getName());
        assertUniqueName(name, groupId);
        List<Subject> subjects = requireEligibleSubjects(request.getSubjectIds(), request.getSubjectType());

        List<Long> existingIds = memberRepository.findByChoiceGroup_SubjectChoiceGroupId(groupId).stream()
                .map(member -> member.getSubject().getSubjectId()).sorted().toList();
        List<Long> requestedIds = subjects.stream().map(Subject::getSubjectId).sorted().toList();
        boolean definitionChanged = group.getSubjectType() != request.getSubjectType()
                || !existingIds.equals(requestedIds);
        if (definitionChanged && choiceMappingRepository.countActiveMappings(groupId) > 0) {
            throw new ConflictException("Unmap this choice group from active academic groups before changing "
                    + "its subject type or membership. Existing teacher assignments and timetables are preserved.");
        }

        group.setName(name);
        group.setSubjectType(request.getSubjectType());
        try {
            group = groupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("A choice group with this name already exists.");
        }
        if (definitionChanged) {
            List<SubjectChoiceGroupMember> existingMembers =
                    memberRepository.findByChoiceGroup_SubjectChoiceGroupId(groupId);
            Set<Long> requestedSubjectIds = requestedIds.stream().collect(Collectors.toSet());
            List<SubjectChoiceGroupMember> removedMembers = existingMembers.stream()
                    .filter(member -> !requestedSubjectIds.contains(member.getSubject().getSubjectId()))
                    .toList();
            if (!removedMembers.isEmpty()) memberRepository.deleteAllInBatch(removedMembers);
            Set<Long> existingSubjectIds = existingMembers.stream()
                    .map(member -> member.getSubject().getSubjectId())
                    .collect(Collectors.toSet());
            saveMembers(group, subjects.stream()
                    .filter(subject -> !existingSubjectIds.contains(subject.getSubjectId()))
                    .toList());
        }
        return toResponse(group, memberRepository.findByChoiceGroup_SubjectChoiceGroupId(groupId));
    }

    @Override
    public SubjectChoiceGroupResponse deactivate(Long groupId) {
        return setActive(groupId, false);
    }

    @Override
    public SubjectChoiceGroupResponse activate(Long groupId) {
        return setActive(groupId, true);
    }

    private SubjectChoiceGroupResponse setActive(Long groupId, boolean active) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_SUBJECTS);
        SubjectChoiceGroup group = requireGroup(groupId);
        if (!active && choiceMappingRepository.countActiveMappings(groupId) > 0) {
            throw new ConflictException("Unmap this choice group from active academic groups before deactivating it.");
        }
        if (active) {
            List<Subject> subjects = memberRepository.findByChoiceGroup_SubjectChoiceGroupId(groupId).stream()
                    .map(SubjectChoiceGroupMember::getSubject).toList();
            validateSubjects(subjects, group.getSubjectType());
        }
        group.setActive(active);
        groupRepository.save(group);
        return toResponse(group, memberRepository.findByChoiceGroup_SubjectChoiceGroupId(groupId));
    }

    private Specification<SubjectChoiceGroup> directoryFilter(String q, SubjectType subjectType, Boolean active) {
        String pattern = SubjectInputRules.containsPattern(q);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (pattern != null) {
                Subquery<Long> matchingMember = query.subquery(Long.class);
                Root<SubjectChoiceGroupMember> member = matchingMember.from(SubjectChoiceGroupMember.class);
                Join<SubjectChoiceGroupMember, Subject> subject = member.join("subject");
                matchingMember.select(member.get("subjectChoiceGroupMemberId")).where(
                        cb.equal(member.get("choiceGroup"), root),
                        cb.or(
                                cb.like(cb.lower(subject.get("name")), pattern, '!'),
                                cb.like(cb.lower(subject.get("code")), pattern, '!')));
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '!'),
                        cb.exists(matchingMember)));
            }
            if (subjectType != null) predicates.add(cb.equal(root.get("subjectType"), subjectType));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Map<Long, List<SubjectChoiceGroupMember>> membersByGroup(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) return Map.of();
        return memberRepository.findDetailedByGroupIds(groupIds).stream()
                .collect(Collectors.groupingBy(
                        member -> member.getChoiceGroup().getSubjectChoiceGroupId(),
                        Collectors.toList()));
    }

    private List<Subject> requireEligibleSubjects(List<Long> rawIds, SubjectType type) {
        if (type == null) throw new BusinessException("Select a subject type.");
        if (rawIds == null || rawIds.size() < MIN_MEMBERS) {
            throw new BusinessException("Select at least two subjects for a choice group.");
        }
        List<Long> ids = rawIds.stream().filter(java.util.Objects::nonNull).toList();
        if (ids.size() < MIN_MEMBERS || new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException("Select at least two different subjects.");
        }
        List<Subject> subjects = subjectRepository.findAllById(ids);
        if (subjects.size() != ids.size()) {
            throw new BusinessException("One or more selected subjects do not exist in this organization.");
        }
        validateSubjects(subjects, type);
        return subjects.stream().sorted(java.util.Comparator.comparing(
                Subject::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private void validateSubjects(List<Subject> subjects, SubjectType type) {
        if (subjects.size() < MIN_MEMBERS) {
            throw new BusinessException("A choice group must contain at least two active subjects.");
        }
        if (subjects.stream().anyMatch(subject -> !subject.isActive())) {
            throw new BusinessException("Inactive subjects cannot be added to a choice group.");
        }
        if (subjects.stream().anyMatch(subject -> subject.getSubjectType() != type)) {
            throw new BusinessException("All subjects in a choice group must have the selected subject type.");
        }
    }

    private void saveMembers(SubjectChoiceGroup group, List<Subject> subjects) {
        memberRepository.saveAll(subjects.stream().map(subject -> {
            SubjectChoiceGroupMember member = new SubjectChoiceGroupMember();
            member.setChoiceGroup(group);
            member.setSubject(subject);
            return member;
        }).toList());
    }

    private String requireName(String raw) {
        String name = SubjectInputRules.cleanText(raw);
        if (!StringUtils.hasText(name)) throw new BusinessException("Choice group name is required.");
        if (name.length() > NAME_MAX) {
            throw new BusinessException("Choice group name cannot exceed " + NAME_MAX + " characters.");
        }
        return name;
    }

    private void assertUniqueName(String name, Long excludeId) {
        boolean exists = excludeId == null
                ? groupRepository.existsByNameIgnoreCase(name)
                : groupRepository.existsByNameIgnoreCaseAndSubjectChoiceGroupIdNot(name, excludeId);
        if (exists) throw new ConflictException("A choice group with this name already exists.");
    }

    private SubjectChoiceGroup requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject choice group not found: " + groupId));
    }

    private int clampSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private SubjectChoiceGroupResponse toResponse(
            SubjectChoiceGroup group, List<SubjectChoiceGroupMember> members) {
        return SubjectChoiceGroupResponse.builder()
                .subjectChoiceGroupId(group.getSubjectChoiceGroupId())
                .name(group.getName())
                .subjectType(group.getSubjectType())
                .active(group.isActive())
                .subjects(members.stream().map(member -> SubjectChoiceGroupSubjectResponse.builder()
                        .subjectId(member.getSubject().getSubjectId())
                        .name(member.getSubject().getName())
                        .code(member.getSubject().getCode())
                        .active(member.getSubject().isActive())
                        .build()).toList())
                .createdOn(group.getCreatedOn())
                .build();
    }
}
