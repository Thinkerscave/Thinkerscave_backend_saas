package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubjectServiceImplTest {

    private SubjectRepository subjectRepo;
    private AcademicGroupSubjectMappingRepository mappingRepo;
    private SubjectServiceImpl service;

    @BeforeEach
    void setUp() {
        subjectRepo = mock(SubjectRepository.class);
        mappingRepo = mock(AcademicGroupSubjectMappingRepository.class);
        SubjectMappingCoverageLoader coverage = new SubjectMappingCoverageLoader(mock(SectionRepository.class));
        service = new SubjectServiceImpl(subjectRepo, mappingRepo, coverage, mock(AcademicsAccessGuard.class));
        when(subjectRepo.saveAndFlush(any(Subject.class))).thenAnswer(inv -> {
            Subject subject = inv.getArgument(0);
            if (subject.getSubjectId() == null) subject.setSubjectId(7L);
            return subject;
        });
    }

    @Test
    void createSanitizesNameAndDescriptionAndGeneratesCode() {
        SubjectRequest request = request("<b>Mathematics</b>   ", null, SubjectType.ACADEMIC_SUBJECT);
        request.setDescription("  Core <i>maths</i>  ");

        SubjectResponse saved = service.create(request);

        assertEquals("Mathematics", saved.getName());
        assertEquals("MAT", saved.getCode());
        assertEquals("Core maths", saved.getDescription());
        assertEquals(SubjectType.ACADEMIC_SUBJECT, saved.getSubjectType());
        assertTrue(saved.getActive());
    }

    @Test
    void createRejectsNameUsedByAnotherSubjectAndReportsField() {
        when(subjectRepo.existsByNameIgnoreCase("Physics")).thenReturn(true);

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> service.create(request("Physics", "PHY", SubjectType.ACADEMIC_SUBJECT)));
        assertEquals("name", ex.getField());
        verify(subjectRepo, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsCodeAlreadyUsedInstitutionWide() {
        when(subjectRepo.existsByCodeIgnoreCase("PHY")).thenReturn(true);

        AlreadyExistsException ex = assertThrows(AlreadyExistsException.class,
                () -> service.create(request("Physics", "phy", SubjectType.PRACTICAL_LAB)));
        assertEquals("code", ex.getField());
    }

    @Test
    void createRejectsMalformedCode() {
        assertThrows(BusinessException.class,
                () -> service.create(request("Physics", "PH@Y", SubjectType.ACADEMIC_SUBJECT)));
    }

    @Test
    void createRequiresSubjectType() {
        assertThrows(BusinessException.class, () -> service.create(request("Physics", "PHY", null)));
    }

    @Test
    void deactivateChangesOnlyTheCatalogRecord() {
        Subject subject = subject(3L, "Art", "ART", true);
        when(subjectRepo.findById(3L)).thenReturn(Optional.of(subject));

        SubjectResponse response = service.deactivate(3L);

        assertEquals(false, response.getActive());
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void directoryClampsPageSizeAndAppliesFilters() {
        when(subjectRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(subject(1L, "Music", "MUS", true))));

        PageResponse<SubjectResponse> page = service.getDirectory("mu", SubjectType.ACTIVITY, true, -3, 500);

        assertEquals(1, page.getContent().size());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(subjectRepo).findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void usageCountsComeFromBatchQueryWithoutMappings() {
        when(subjectRepo.findById(anyLong())).thenReturn(Optional.of(subject(4L, "Odia", "ODI", true)));
        when(mappingRepo.findActiveWithGroupBySubjectIds(any())).thenReturn(List.of());

        SubjectResponse response = service.getById(4L);

        assertEquals(0, response.getMappedAcademicGroupCount());
        assertEquals(0, response.getMappedSectionCount());
    }

    private static SubjectRequest request(String name, String code, SubjectType type) {
        SubjectRequest request = new SubjectRequest();
        request.setName(name);
        request.setCode(code);
        request.setSubjectType(type);
        return request;
    }

    private static Subject subject(long id, String name, String code, boolean active) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName(name);
        subject.setCode(code);
        subject.setSubjectType(SubjectType.ACADEMIC_SUBJECT);
        subject.setActive(active);
        return subject;
    }
}
