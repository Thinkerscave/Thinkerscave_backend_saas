package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.shared.exceptions.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableApprovalLifecycleTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private TimetableVersionRepository versionRepository;
    @Mock private AcademicsAccessGuard accessGuard;

    @InjectMocks private TimetableServiceImpl service;

    @Test
    void draftCannotBePublishedBeforeApproval() {
        assertCannotPublishWithStatus(TimetableStatus.DRAFT);
    }

    @Test
    void readyForReviewCannotBePublishedBeforeApproval() {
        assertCannotPublishWithStatus(TimetableStatus.READY_FOR_REVIEW);
    }

    @Test
    void generationIsBlockedWhileApprovalIsPending() {
        assertCannotGenerateWhileApprovalInProgress(TimetableStatus.PENDING_APPROVAL);
    }

    @Test
    void generationIsBlockedUntilApprovedTimetableIsPublished() {
        assertCannotGenerateWhileApprovalInProgress(TimetableStatus.APPROVED);
    }

    private void assertCannotPublishWithStatus(TimetableStatus status) {
        TimetableVersion version = mock(TimetableVersion.class);
        when(version.getStatus()).thenReturn(status);
        when(versionRepository.findById(41L)).thenReturn(Optional.of(version));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.publishVersion(41L));

        assertTrue(exception.getMessage().contains("Only an approved timetable version can be published"));
        verify(versionRepository, never()).save(version);
    }

    private void assertCannotGenerateWhileApprovalInProgress(TimetableStatus status) {
        AcademicYear year = mock(AcademicYear.class);
        TimetableVersion version = mock(TimetableVersion.class);
        when(academicYearRepository.findById(2026L)).thenReturn(Optional.of(year));
        when(year.getStatus()).thenReturn(AcademicYearStatus.CURRENT);
        when(year.isActive()).thenReturn(true);
        when(versionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(2026L))
                .thenReturn(List.of(version));
        when(version.getStatus()).thenReturn(status);
        when(version.getVersionNumber()).thenReturn(2);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.startGeneration(2026L, null));

        assertTrue(exception.getMessage().contains("Complete the current approval workflow first"));
        verify(versionRepository, never()).save(version);
    }
}
