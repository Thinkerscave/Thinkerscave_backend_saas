package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.TimetableReadinessResponse;
import com.thinkerscave.academics.dto.response.TimetableWorkspaceResponse;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TimetableGenerationStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TimetableConflictRepository;
import com.thinkerscave.academics.repository.TimetableEntryRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningDataLoader;
import com.thinkerscave.academics.timetable.engine.TimetableReadinessEvaluator;
import com.thinkerscave.shared.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableWorkspaceServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private TimetableVersionRepository versionRepository;
    @Mock private TimetableEntryRepository entryRepository;
    @Mock private TimetableConflictRepository conflictRepository;
    @Mock private TimetablePlanningDataLoader planningDataLoader;
    @Mock private TimetableReadinessEvaluator readinessEvaluator;
    @Mock private AcademicsAccessGuard accessGuard;

    @InjectMocks private TimetableWorkspaceServiceImpl service;

    @Test
    void showsPendingApprovalVersionAfterReloadAndBlocksGeneration() {
        AcademicYear year = academicYear(true);
        TimetableVersion pending = version(year, 12L, 3, TimetableStatus.PENDING_APPROVAL);
        TimetableVersion published = version(year, 11L, 2, TimetableStatus.PUBLISHED);
        TimetablePlanningData data = planningData(List.of(pending, published));
        stubWorkspace(year, data);

        TimetableWorkspaceResponse workspace = service.getWorkspace(2026L, LocalDate.of(2026, 10, 31));

        assertEquals("DRAFT", workspace.getState());
        assertTrue(workspace.isYearActive());
        assertNotNull(workspace.getWorkingVersion());
        assertTrue(workspace.getWorkingVersion().getStatus() == TimetableStatus.PENDING_APPROVAL);
        assertFalse(workspace.getWorkingVersion().isEditable());
        assertFalse(workspace.isCanGenerate());
        assertTrue(workspace.getGenerateBlockedReason().contains("pending timetable"));
    }

    @Test
    void showsApprovedVersionAfterReloadUntilItIsPublished() {
        AcademicYear year = academicYear(true);
        TimetableVersion approved = version(year, 12L, 3, TimetableStatus.APPROVED);
        TimetablePlanningData data = planningData(List.of(approved));
        stubWorkspace(year, data);

        TimetableWorkspaceResponse workspace = service.getWorkspace(2026L, LocalDate.of(2026, 10, 31));

        assertNotNull(workspace.getWorkingVersion());
        assertEquals(TimetableStatus.APPROVED, workspace.getWorkingVersion().getStatus());
        assertFalse(workspace.isCanGenerate());
        assertTrue(workspace.getGenerateBlockedReason().contains("Publish the approved timetable"));
    }

    @Test
    void inactiveYearCannotGenerateEvenWhenReadinessPasses() {
        AcademicYear year = academicYear(false);
        TimetablePlanningData data = planningData(List.of());
        stubWorkspace(year, data);

        TimetableWorkspaceResponse workspace = service.getWorkspace(2026L, LocalDate.of(2026, 10, 31));

        assertFalse(workspace.isCanGenerate());
        assertFalse(workspace.isYearActive());
        assertTrue(workspace.getGenerateBlockedReason().contains("Activate this academic year"));
    }

    @Test
    void manualDraftIsBlockedWhileApprovalIsPending() {
        AcademicYear year = academicYear(true);
        TimetableVersion pending = org.mockito.Mockito.mock(TimetableVersion.class);
        when(pending.getStatus()).thenReturn(TimetableStatus.PENDING_APPROVAL);
        when(pending.getVersionNumber()).thenReturn(3);
        when(academicYearRepository.findById(2026L)).thenReturn(Optional.of(year));
        when(versionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(2026L))
                .thenReturn(List.of(pending));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.createManualDraft(2026L));

        assertTrue(exception.getMessage().contains("Complete the current approval workflow first"));
    }

    @Test
    void manualDraftIsBlockedForInactiveYear() {
        AcademicYear year = academicYear(false);
        when(academicYearRepository.findById(2026L)).thenReturn(Optional.of(year));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.createManualDraft(2026L));

        assertTrue(exception.getMessage().contains("inactive academic year"));
        verify(versionRepository, never())
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(2026L);
    }

    private void stubWorkspace(AcademicYear year, TimetablePlanningData data) {
        when(year.getAcademicYearId()).thenReturn(2026L);
        when(year.getName()).thenReturn("2026");
        when(academicYearRepository.findById(2026L)).thenReturn(Optional.of(year));
        when(planningDataLoader.load(eq(2026L), isNull(), any(LocalDate.class))).thenReturn(data);
        when(readinessEvaluator.evaluate(data)).thenReturn(TimetableReadinessResponse.builder()
                .ready(true)
                .checks(List.of())
                .blockingIssues(List.of())
                .warnings(List.of())
                .build());
        if (!data.getVersions().isEmpty()) {
            when(conflictRepository.countByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                    any(), any())).thenReturn(0L);
            when(conflictRepository.countByTimetableVersion_TimetableVersionId(any())).thenReturn(0L);
            when(conflictRepository.findByTimetableVersion_TimetableVersionId(any())).thenReturn(List.of());
            when(entryRepository.countByTimetableVersion_TimetableVersionId(any())).thenReturn(0L);
        }
    }

    private AcademicYear academicYear(boolean active) {
        AcademicYear year = org.mockito.Mockito.mock(AcademicYear.class);
        when(year.getStatus()).thenReturn(active ? AcademicYearStatus.CURRENT : AcademicYearStatus.APPROVED);
        when(year.isActive()).thenReturn(active);
        return year;
    }

    private TimetableVersion version(AcademicYear year, Long id, int number, TimetableStatus status) {
        TimetableVersion version = org.mockito.Mockito.mock(TimetableVersion.class);
        when(version.getTimetableVersionId()).thenReturn(id);
        when(version.getAcademicYear()).thenReturn(year);
        when(version.getVersionNumber()).thenReturn(number);
        when(version.getGenerationStatus()).thenReturn(TimetableGenerationStatus.GENERATED);
        when(version.getStatus()).thenReturn(status);
        return version;
    }

    private TimetablePlanningData planningData(List<TimetableVersion> versions) {
        return TimetablePlanningData.builder()
                .patterns(List.of())
                .scheduleByUnit(Map.of())
                .units(List.of())
                .referenceDate(LocalDate.of(2026, 10, 31))
                .sections(List.of())
                .mappings(List.of())
                .lessons(List.of())
                .missingTeachers(List.of())
                .versions(versions)
                .build();
    }
}
