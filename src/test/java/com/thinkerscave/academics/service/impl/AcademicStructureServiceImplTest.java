package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.AcademicGroupsDashboardResponse;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicGroupRepository;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.IdCount;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicStructureServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private AcademicGroupRepository academicGroupRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock private AcademicGroupSubjectMappingRepository academicGroupSubjectMappingRepository;
    @Mock private TeacherAllocationTeacherRepository teacherAllocationTeacherRepository;
    @Mock private TimetableVersionRepository timetableVersionRepository;
    @Mock private AcademicsAccessGuard accessGuard;

    @InjectMocks private AcademicStructureServiceImpl service;

    @Test
    void getDashboardIncludesBulkSubjectTeacherAndTimetableSummaries() {
        AcademicYear year = mock(AcademicYear.class);
        when(year.getAcademicYearId()).thenReturn(2026L);
        when(year.getName()).thenReturn("2026");
        when(year.getStatus()).thenReturn(AcademicYearStatus.APPROVED);
        when(academicYearRepository.findById(2026L)).thenReturn(Optional.of(year));

        AcademicGroup classEight = group(12L, "Class 8", year);
        AcademicGroup classSeven = group(13L, "Class 7", year);
        when(academicGroupRepository.findWithYearByAcademicYearIdOrderByDisplayOrderAsc(2026L))
                .thenReturn(List.of(classEight, classSeven));
        when(sectionRepository.findWithAcademicGroupByYearId(2026L)).thenReturn(List.of());
        when(studentEnrollmentRepository.countActiveByAcademicGroupForYear(2026L)).thenReturn(List.of());
        IdCount classEightSubjects = count(12L, 11L);
        IdCount classEightTeachers = count(12L, 9L);
        when(academicGroupSubjectMappingRepository.countActiveByAcademicGroupForYear(2026L))
                .thenReturn(List.of(classEightSubjects));
        when(teacherAllocationTeacherRepository.countDistinctPrimaryTeachersByAcademicYear(2026L))
                .thenReturn(List.of(classEightTeachers));
        when(timetableVersionRepository.findByAcademicYear_AcademicYearIdAndStatus(
                2026L, TimetableStatus.PUBLISHED)).thenReturn(Optional.empty());
        when(timetableVersionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(2026L))
                .thenReturn(List.of(mock(TimetableVersion.class)));

        AcademicGroupsDashboardResponse dashboard = service.getDashboard(2026L, null, null, null);

        assertEquals(11L, dashboard.getAcademicGroups().get(0).getSubjectCount());
        assertEquals(9L, dashboard.getAcademicGroups().get(0).getTeacherCount());
        assertEquals("DRAFT", dashboard.getAcademicGroups().get(0).getTimetableStatus());
        assertEquals(0L, dashboard.getAcademicGroups().get(1).getSubjectCount());
        assertEquals(0L, dashboard.getAcademicGroups().get(1).getTeacherCount());
        assertEquals("DRAFT", dashboard.getAcademicGroups().get(1).getTimetableStatus());
        verify(academicGroupSubjectMappingRepository).countActiveByAcademicGroupForYear(2026L);
        verify(teacherAllocationTeacherRepository).countDistinctPrimaryTeachersByAcademicYear(2026L);
    }

    private AcademicGroup group(Long id, String name, AcademicYear year) {
        AcademicGroup group = mock(AcademicGroup.class);
        when(group.getAcademicGroupId()).thenReturn(id);
        when(group.getName()).thenReturn(name);
        when(group.getAcademicYear()).thenReturn(year);
        when(group.getActive()).thenReturn(true);
        return group;
    }

    private IdCount count(Long id, Long total) {
        IdCount count = mock(IdCount.class);
        when(count.getId()).thenReturn(id);
        when(count.getTotal()).thenReturn(total);
        return count;
    }
}
