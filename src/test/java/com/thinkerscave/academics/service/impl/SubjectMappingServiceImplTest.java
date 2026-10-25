package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectMappingEntryRequest;
import com.thinkerscave.academics.dto.request.SubjectMappingSaveRequest;
import com.thinkerscave.academics.dto.response.SubjectMappingBoardResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingImpactResponse;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMappingSection;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.TeacherAllocation;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.SubjectMappingScope;
import com.thinkerscave.academics.enums.SubjectRequirement;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.repository.AcademicGroupRepository;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingSectionRepository;
import com.thinkerscave.academics.repository.IdCount;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.repository.TeacherAllocationRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubjectMappingServiceImplTest {

    private static final long GROUP_ID = 1L;
    private static final long YEAR_ID = 5L;

    private AcademicGroupRepository groupRepo;
    private SectionRepository sectionRepo;
    private SubjectRepository subjectRepo;
    private AcademicGroupSubjectMappingRepository mappingRepo;
    private AcademicGroupSubjectMappingSectionRepository mappingSectionRepo;
    private TeacherAllocationRepository allocationRepo;
    private SubjectMappingServiceImpl service;

    private AcademicGroup group;
    private AcademicSection sectionA;
    private AcademicSection sectionB;
    private Subject maths;

    @BeforeEach
    void setUp() {
        groupRepo = mock(AcademicGroupRepository.class);
        sectionRepo = mock(SectionRepository.class);
        subjectRepo = mock(SubjectRepository.class);
        mappingRepo = mock(AcademicGroupSubjectMappingRepository.class);
        mappingSectionRepo = mock(AcademicGroupSubjectMappingSectionRepository.class);
        allocationRepo = mock(TeacherAllocationRepository.class);
        SubjectMappingCoverageLoader coverage = new SubjectMappingCoverageLoader(sectionRepo, mappingSectionRepo);
        service = new SubjectMappingServiceImpl(groupRepo, sectionRepo, subjectRepo, mappingRepo,
                mappingSectionRepo, allocationRepo, coverage, mock(AcademicsAccessGuard.class));

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

        sectionA = section(11L, "6A", 1);
        sectionB = section(12L, "6B", 2);

        maths = new Subject();
        maths.setSubjectId(10L);
        maths.setName("Mathematics");
        maths.setCode("MATH");
        maths.setSubjectType(SubjectType.ACADEMIC_SUBJECT);
        maths.setActive(true);

        when(groupRepo.findByIdWithYear(GROUP_ID)).thenReturn(Optional.of(group));
        when(sectionRepo.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(GROUP_ID))
                .thenReturn(List.of(sectionA, sectionB));
        when(subjectRepo.findAllById(anyCollection())).thenReturn(List.of(maths));
        when(mappingRepo.save(any(AcademicGroupSubjectMapping.class))).thenAnswer(inv -> {
            AcademicGroupSubjectMapping saved = inv.getArgument(0);
            if (saved.getAcademicGroupSubjectMappingId() == null) saved.setAcademicGroupSubjectMappingId(100L);
            return saved;
        });
    }

    @Test
    void savesNewAllSectionsMappingWithoutSectionRows() {
        service.save(GROUP_ID, request(entry(10L, 4, SubjectMappingScope.ALL_SECTIONS)));

        ArgumentCaptor<AcademicGroupSubjectMapping> captor = ArgumentCaptor.forClass(AcademicGroupSubjectMapping.class);
        verify(mappingRepo).save(captor.capture());
        AcademicGroupSubjectMapping saved = captor.getValue();
        assertEquals((short) 4, saved.getWeeklyPeriods());
        assertEquals(SubjectMappingScope.ALL_SECTIONS, saved.getScope());
        assertEquals(SubjectRequirement.COMPULSORY, saved.getRequirement());
        assertTrue(saved.isActive());
        verify(mappingSectionRepo, never()).saveAll(anyList());
    }

    @Test
    void savesSelectedSectionsAsSectionRowsOnly() {
        service.save(GROUP_ID, request(entry(10L, 3, SubjectMappingScope.SELECTED_SECTIONS, 12L)));

        verify(mappingSectionRepo).saveAll(argThat((List<AcademicGroupSubjectMappingSection> rows) ->
                rows.size() == 1 && rows.get(0).getSection().getSectionId().equals(12L)));
    }

    @Test
    void rejectsSelectedScopeWithoutSections() {
        assertThrows(BusinessException.class, () ->
                service.save(GROUP_ID, request(entry(10L, 3, SubjectMappingScope.SELECTED_SECTIONS))));
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void rejectsSectionsOutsideTheGroup() {
        assertThrows(BusinessException.class, () ->
                service.save(GROUP_ID, request(entry(10L, 3, SubjectMappingScope.SELECTED_SECTIONS, 999L))));
    }

    @Test
    void rejectsWeeklyPeriodsOutsideSupportedRange() {
        assertThrows(BusinessException.class, () -> service.preview(GROUP_ID, request(entry(10L, 0, SubjectMappingScope.ALL_SECTIONS))));
        assertThrows(BusinessException.class, () -> service.preview(GROUP_ID, request(entry(10L, 41, SubjectMappingScope.ALL_SECTIONS))));
    }

    @Test
    void rejectsInactiveSubjectForNewMapping() {
        maths.setActive(false);
        assertThrows(BusinessException.class, () ->
                service.save(GROUP_ID, request(entry(10L, 4, SubjectMappingScope.ALL_SECTIONS))));
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void rejectsYearThatDoesNotOwnTheGroup() {
        SubjectMappingSaveRequest request = request(entry(10L, 4, SubjectMappingScope.ALL_SECTIONS));
        request.setAcademicYearId(99L);
        assertThrows(BusinessException.class, () -> service.save(GROUP_ID, request));
    }

    @Test
    void rejectsDuplicateSubjectInOneSave() {
        SubjectMappingSaveRequest request = request(
                entry(10L, 4, SubjectMappingScope.ALL_SECTIONS),
                entry(10L, 2, SubjectMappingScope.ALL_SECTIONS));
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
    void uncoveringSectionWithTeacherAssignmentIsBlockedEvenWhenConfirmed() {
        AcademicGroupSubjectMapping existing = existingMapping();
        when(mappingRepo.findByAcademicGroup_AcademicGroupId(GROUP_ID)).thenReturn(List.of(existing));
        TeacherAllocation allocation = new TeacherAllocation();
        allocation.setSection(sectionB);
        allocation.setActive(true);
        when(allocationRepo.findByAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(100L)).thenReturn(List.of(allocation));

        SubjectMappingSaveRequest request = request(entry(10L, 4, SubjectMappingScope.SELECTED_SECTIONS, 11L));
        request.setConfirmImpact(true);
        assertThrows(BusinessException.class, () -> service.save(GROUP_ID, request));
        verify(mappingRepo, never()).save(any());
    }

    @Test
    void scheduleChangeWithTimetableDependencyRequiresConfirmation() {
        AcademicGroupSubjectMapping existing = existingMapping();
        when(mappingRepo.findByAcademicGroup_AcademicGroupId(GROUP_ID)).thenReturn(List.of(existing));
        when(mappingRepo.countTimetableEntriesByMappingIds(anyCollection())).thenReturn(List.of(idCount(100L, 3L)));

        SubjectMappingImpactResponse preview = service.preview(GROUP_ID,
                request(entry(10L, 6, SubjectMappingScope.ALL_SECTIONS)));
        assertTrue(preview.isRequiresConfirmation());
        assertEquals("SCHEDULE_CHANGE", preview.getImpacts().get(0).getType());
    }

    @Test
    void previewNeverWrites() {
        service.preview(GROUP_ID, request(entry(10L, 4, SubjectMappingScope.ALL_SECTIONS)));
        verify(mappingRepo, never()).save(any());
        verify(mappingSectionRepo, never()).saveAll(anyList());
        verify(mappingSectionRepo, never()).deleteAll(anyList());
    }

    @Test
    void savedBoardReflectsActiveRowsOnly() {
        SubjectMappingBoardResponse board = service.save(GROUP_ID, request(entry(10L, 4, SubjectMappingScope.ALL_SECTIONS)));
        assertEquals(GROUP_ID, board.getAcademicGroupId());
        assertEquals(2, board.getSections().size());
        assertTrue(board.isHasSections());
    }

    private AcademicGroupSubjectMapping existingMapping() {
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setAcademicGroupSubjectMappingId(100L);
        mapping.setAcademicGroup(group);
        mapping.setSubject(maths);
        mapping.setWeeklyPeriods((short) 4);
        mapping.setRequirement(SubjectRequirement.COMPULSORY);
        mapping.setScope(SubjectMappingScope.ALL_SECTIONS);
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

    private static AcademicSection section(long id, String name, int order) {
        AcademicSection section = new AcademicSection();
        section.setSectionId(id);
        section.setName(name);
        section.setCode(name);
        section.setDisplayOrder(order);
        section.setActive(true);
        return section;
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

    private static SubjectMappingEntryRequest entry(long subjectId, int weeklyPeriods,
                                                    SubjectMappingScope scope, Long... sectionIds) {
        SubjectMappingEntryRequest entry = new SubjectMappingEntryRequest();
        entry.setSubjectId(subjectId);
        entry.setWeeklyPeriods((short) weeklyPeriods);
        entry.setScope(scope);
        entry.setSectionIds(List.of(sectionIds));
        return entry;
    }
}
