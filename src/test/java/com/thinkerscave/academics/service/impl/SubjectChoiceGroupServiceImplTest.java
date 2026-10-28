package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectChoiceGroupRequest;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.SubjectChoiceGroup;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.repository.AcademicGroupChoiceMappingRepository;
import com.thinkerscave.academics.repository.SubjectChoiceGroupMemberRepository;
import com.thinkerscave.academics.repository.SubjectChoiceGroupRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubjectChoiceGroupServiceImplTest {

    private SubjectChoiceGroupRepository groupRepository;
    private SubjectChoiceGroupMemberRepository memberRepository;
    private SubjectRepository subjectRepository;
    private AcademicGroupChoiceMappingRepository choiceMappingRepository;
    private SubjectChoiceGroupServiceImpl service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(SubjectChoiceGroupRepository.class);
        memberRepository = mock(SubjectChoiceGroupMemberRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        choiceMappingRepository = mock(AcademicGroupChoiceMappingRepository.class);
        service = new SubjectChoiceGroupServiceImpl(
                groupRepository, memberRepository, subjectRepository, choiceMappingRepository,
                mock(AcademicsAccessGuard.class));
    }

    @Test
    void createTrimsNameAndPersistsExistingSameTypeSubjects() {
        Subject first = subject(1L, "Hindi", SubjectType.ACADEMIC_SUBJECT);
        Subject second = subject(2L, "Sanskrit", SubjectType.ACADEMIC_SUBJECT);
        when(subjectRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(groupRepository.saveAndFlush(any(SubjectChoiceGroup.class))).thenAnswer(invocation -> {
            SubjectChoiceGroup group = invocation.getArgument(0);
            group.setSubjectChoiceGroupId(9L);
            return group;
        });

        service.create(request("  Second Language  ", SubjectType.ACADEMIC_SUBJECT, List.of(1L, 2L)));

        ArgumentCaptor<SubjectChoiceGroup> groupCaptor = ArgumentCaptor.forClass(SubjectChoiceGroup.class);
        verify(groupRepository).saveAndFlush(groupCaptor.capture());
        assertEquals("Second Language", groupCaptor.getValue().getName());
        assertEquals(SubjectType.ACADEMIC_SUBJECT, groupCaptor.getValue().getSubjectType());
        verify(memberRepository).saveAll(anyList());
    }

    @Test
    void createRejectsDuplicateSubjectIdsBeforeLoadingSubjects() {
        assertThrows(BusinessException.class, () -> service.create(
                request("Second Language", SubjectType.ACADEMIC_SUBJECT, List.of(1L, 1L))));

        verify(subjectRepository, never()).findAllById(any());
        verify(groupRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsSubjectsWithDifferentTypes() {
        when(subjectRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                subject(1L, "Hindi", SubjectType.ACADEMIC_SUBJECT),
                subject(2L, "Dance", SubjectType.ACTIVITY)));

        assertThrows(BusinessException.class, () -> service.create(
                request("Alternatives", SubjectType.ACADEMIC_SUBJECT, List.of(1L, 2L))));

        verify(groupRepository, never()).saveAndFlush(any());
    }

    @Test
    void deactivateRejectsGroupUsedByActiveCurriculumMappings() {
        SubjectChoiceGroup group = new SubjectChoiceGroup();
        group.setSubjectChoiceGroupId(9L);
        group.setActive(true);
        when(groupRepository.findById(9L)).thenReturn(Optional.of(group));
        when(choiceMappingRepository.countActiveMappings(9L)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.deactivate(9L));

        verify(groupRepository, never()).save(any());
    }

    private static SubjectChoiceGroupRequest request(String name, SubjectType type, List<Long> subjectIds) {
        SubjectChoiceGroupRequest request = new SubjectChoiceGroupRequest();
        request.setName(name);
        request.setSubjectType(type);
        request.setSubjectIds(subjectIds);
        return request;
    }

    private static Subject subject(long id, String name, SubjectType type) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName(name);
        subject.setCode(name.substring(0, 3).toUpperCase());
        subject.setSubjectType(type);
        subject.setActive(true);
        return subject;
    }
}
