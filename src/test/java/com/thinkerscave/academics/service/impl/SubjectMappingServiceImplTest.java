package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectMappingEntryRequest;
import com.thinkerscave.academics.dto.request.SubjectMappingSaveRequest;
import com.thinkerscave.academics.dto.response.SubjectMappingBoardResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingImpactResponse;
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
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubjectMappingServiceImplTest {

    private static final long GROUP_ID = 1L;
    private static final long YEAR_ID = 5L;

    private AcademicGroupRepository groupRepo;
    private SubjectRepository subjectRepo;
    private AcademicGroupSubjectMappingRepository mappingRepo;
    private SubjectMappingServiceImpl service;

    private AcademicGroup group;
    private Subject maths;

    @BeforeEach
    void setUp() {
        groupRepo = mock(AcademicGroupRepository.class);
        subjectRepo = mock(SubjectRepository.class);
        mappingRepo = mock(AcademicGroupSubjectMappingRepository.class);
        service = new SubjectMappingServiceImpl(groupRepo, subjectRepo, mappingRepo, mock(AcademicsAccessGuard.class));

        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(YEAR_ID);
        year.setName("2026-27");
        year.setStatus(AcademicYearStatus.CURRENT);
        year.setActive(true);

        group = new AcademicGroup();
        group.setAcademicGroupId(GROUP_ID);
        group.setName("Class 6");
        group.setCode("CLS-6");
        group.setAcademicYear(year);
        group.setActive(true);

        maths = new Subject();
        maths.setSubjectId(10L);
        maths.setName("Mathematics");
        maths.setCode("MATH");
        maths.setSubjectType(SubjectType.ACADEMIC_SUBJECT);
        maths.setActive(true);

        when(groupRepo.findByIdWithYear(GROUP_ID)).thenReturn(Optional.of(group));
        when(subjectRepo.findAllById(anyCollection())).thenReturn(List.of(maths));
        when(mappingRepo.save(any(AcademicGroupSubjectMapping.class))).thenAnswer(inv -> {
            AcademicGroupSubjectMapping saved = inv.getArgument(0);
            if (saved.getAcademicGroupSubjectMappingId() == null) saved.setAcademicGroupSubjectMappingId(100L);
            return saved;
        });
    }

    @Test
    void savesNewGroupWideMapping() {
        service.save(GROUP_ID, request(entry(10L, 4)));

        ArgumentCaptor<AcademicGroupSubjectMapping> captor = ArgumentCaptor.forClass(AcademicGroupSubjectMapping.class);
        verify(mappingRepo).save(captor.capture());
        AcademicGroupSubjectMapping saved = captor.getValue();
        assertEquals((short) 4, saved.getWeeklyPeriods());
        assertEquals(SubjectRequirement.COMPULSORY, saved.getRequirement());
        assertTrue(saved.isActive());
    }

    @Test
    void rejectsWeeklyPeriodsOutsideSupportedRange() {
        assertThrows(BusinessException.class, () -> service.preview(GROUP_ID, request(entry(10L, 0))));
        assertThrows(BusinessException.class, () -> service.preview(GROUP_ID, request(entry(10L, 41))));
    }

    @Test
    void rejectsInactiveSubjectForNewMapping() {
        maths.setActive(false);
        assertThrows(BusinessException.class, () ->
                service.save(GROUP_ID, request(entry(10L, 4))));
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void availableSubjectsIncludeCatalogPreferredTime() {
        maths.setDefaultTimetablePreference(SubjectTimetablePreference.FIRST_HALF);
        when(mappingRepo.findByAcademicGroup_AcademicGroupIdAndActiveTrue(GROUP_ID)).thenReturn(List.of());
        when(subjectRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(maths)));

        var available = service.getAvailableSubjects(GROUP_ID, null, null, 0, 10);

        assertEquals(SubjectTimetablePreference.FIRST_HALF,
                available.getContent().get(0).getDefaultTimetablePreference());
    }

    @Test
    void rejectsYearThatDoesNotOwnTheGroup() {
        SubjectMappingSaveRequest request = request(entry(10L, 4));
        request.setAcademicYearId(99L);
        assertThrows(BusinessException.class, () -> service.save(GROUP_ID, request));
    }

    @Test
    void rejectsDuplicateSubjectInOneSave() {
        SubjectMappingSaveRequest request = request(
                entry(10L, 4),
                entry(10L, 2));
        assertThrows(BusinessException.class, () -> service.save(GROUP_ID, request));
    }

    @Test
    void removalWithTimetableDependencyRequiresConfirmationThenDeactivatesOnly() {
        AcademicGroupSubjectMapping existing = existingMapping();
        when(mappingRepo.findByAcademicGroup_AcademicGroupId(GROUP_ID)).thenReturn(List.of(existing));
        when(mappingRepo.countTimetableEntriesByMappingIds(anyCollection())).thenReturn(List.of(idCount(100L, 2L)));

        SubjectMappingSaveRequest removal = removalRequest(10L, false);
        SubjectMappingImpactResponse preview = service.preview(GROUP_ID, removal);
        assertTrue(preview.isRequiresConfirmation());
        assertEquals("REMOVAL", preview.getImpacts().get(0).getType());
        assertThrows(ConflictException.class, () -> service.save(GROUP_ID, removal));

        verify(mappingRepo, never()).save(any());
        service.save(GROUP_ID, removalRequest(10L, true));

        ArgumentCaptor<AcademicGroupSubjectMapping> captor = ArgumentCaptor.forClass(AcademicGroupSubjectMapping.class);
        verify(mappingRepo).save(captor.capture());
        assertFalse(captor.getValue().isActive());
        assertEquals(maths, captor.getValue().getSubject());
    }

    @Test
    void removingUnmappedSubjectIsRejected() {
        when(mappingRepo.findByAcademicGroup_AcademicGroupId(GROUP_ID)).thenReturn(List.of());
        assertThrows(BusinessException.class, () -> service.save(GROUP_ID, removalRequest(10L, true)));
    }

    @Test
    void scheduleChangeWithTimetableDependencyRequiresConfirmation() {
        AcademicGroupSubjectMapping existing = existingMapping();
        when(mappingRepo.findByAcademicGroup_AcademicGroupId(GROUP_ID)).thenReturn(List.of(existing));
        when(mappingRepo.countTimetableEntriesByMappingIds(anyCollection())).thenReturn(List.of(idCount(100L, 3L)));

        SubjectMappingImpactResponse preview = service.preview(GROUP_ID, request(entry(10L, 6)));
        assertTrue(preview.isRequiresConfirmation());
        assertEquals("SCHEDULE_CHANGE", preview.getImpacts().get(0).getType());
    }

    @Test
    void previewNeverWrites() {
        service.preview(GROUP_ID, request(entry(10L, 4)));
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void savedBoardUsesGroupWideMappings() {
        when(mappingRepo.findActiveWithSubjectByAcademicGroupId(GROUP_ID))
                .thenReturn(List.of(existingMapping()));
        SubjectMappingBoardResponse board = service.save(GROUP_ID, request(entry(10L, 4)));
        assertEquals(GROUP_ID, board.getAcademicGroupId());
        assertEquals(1, board.getMappings().size());
        assertEquals((short) 4, board.getMappings().get(0).getWeeklyPeriods());
    }

    private AcademicGroupSubjectMapping existingMapping() {
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setAcademicGroupSubjectMappingId(100L);
        mapping.setAcademicGroup(group);
        mapping.setSubject(maths);
        mapping.setWeeklyPeriods((short) 4);
        mapping.setRequirement(SubjectRequirement.COMPULSORY);
        mapping.setTimetablePreference(com.thinkerscave.academics.enums.SubjectTimetablePreference.ANY);
        mapping.setActive(true);
        return mapping;
    }

    private SubjectMappingSaveRequest removalRequest(long subjectId, boolean confirm) {
        SubjectMappingSaveRequest request = new SubjectMappingSaveRequest();
        request.setAcademicYearId(YEAR_ID);
        request.setRemovedSubjectIds(List.of(subjectId));
        request.setConfirmImpact(confirm);
        return request;
    }

    private static IdCount idCount(long id, long total) {
        return new IdCount() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    private SubjectMappingSaveRequest request(SubjectMappingEntryRequest... entries) {
        SubjectMappingSaveRequest request = new SubjectMappingSaveRequest();
        request.setAcademicYearId(YEAR_ID);
        request.setUpserts(List.of(entries));
        return request;
    }

    private static SubjectMappingEntryRequest entry(long subjectId, int weeklyPeriods) {
        SubjectMappingEntryRequest entry = new SubjectMappingEntryRequest();
        entry.setSubjectId(subjectId);
        entry.setWeeklyPeriods((short) weeklyPeriods);
        return entry;
    }
}
