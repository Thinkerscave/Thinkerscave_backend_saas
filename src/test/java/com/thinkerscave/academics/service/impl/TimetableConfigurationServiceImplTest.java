package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.GroupAssignmentMatrixResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableConfigurationServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private AcademicGroupRepository academicGroupRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SchedulePatternRepository patternRepository;
    @Mock private TimetablePeriodRepository periodRepository;
    @Mock private GroupScheduleAssignmentRepository assignmentRepository;
    @Mock private FixedActivityRepository fixedActivityRepository;
    @Mock private TimetableVersionRepository versionRepository;
    @Mock private TimetableConfigurationRepository configurationRepository;
    @Mock private TimetableWorkingDayRepository workingDayRepository;
    @Mock private AcademicsAccessGuard accessGuard;

    private TimetableConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TimetableConfigurationServiceImpl(
                academicYearRepository,
                academicGroupRepository,
                sectionRepository,
                patternRepository,
                periodRepository,
                assignmentRepository,
                fixedActivityRepository,
                versionRepository,
                configurationRepository,
                workingDayRepository,
                accessGuard);
    }

    @Test
    void exposesGroupWideAssignmentsAsAnAllSectionsMatrixRow() {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(1L);
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));

        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(3L);
        group.setName("Class 7");
        group.setCode("C7");
        group.setActive(true);
        group.setDisplayOrder(1);
        when(academicGroupRepository.findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(group));

        AcademicSection sectionA = section(group, 11L, "A");
        AcademicSection sectionB = section(group, 12L, "B");
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA, sectionB));

        SchedulePattern pattern = new SchedulePattern();
        pattern.setSchedulePatternId(5L);
        pattern.setName("Standard School Day");
        when(periodRepository.findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(5L))
                .thenReturn(List.of());

        GroupScheduleAssignment assignment = new GroupScheduleAssignment();
        assignment.setGroupScheduleAssignmentId(21L);
        assignment.setAcademicYear(year);
        assignment.setAcademicGroup(group);
        assignment.setSchedulePattern(pattern);
        assignment.setDayOfWeek(DayOfWeek.MONDAY);
        assignment.setActive(true);
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(assignment));

        GroupAssignmentMatrixResponse matrix = service.getAssignmentMatrix(1L);

        assertEquals(3, matrix.getRows().size());
        GroupAssignmentMatrixResponse.Row allSections = matrix.getRows().get(0);
        assertEquals("All Sections", allSections.getScopeLabel());
        assertEquals(1, allSections.getCells().size());
        assertEquals(21L, allSections.getCells().get(0).getGroupScheduleAssignmentId());
        assertTrue(matrix.getRows().get(1).getCells().isEmpty());
        assertTrue(matrix.getRows().get(2).getCells().isEmpty());
    }

    private AcademicSection section(AcademicGroup group, Long id, String name) {
        AcademicSection section = new AcademicSection();
        section.setSectionId(id);
        section.setAcademicGroup(group);
        section.setName(name);
        section.setCode(name);
        section.setActive(true);
        return section;
    }
}
