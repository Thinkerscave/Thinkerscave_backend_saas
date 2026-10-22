package com.thinkerscave.staff.service;

import com.thinkerscave.document.entity.Document;
import com.thinkerscave.document.repository.DocumentRepository;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.staff.dto.response.DocumentResponse;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffDocumentServiceTest {

    private static final long STAFF_ID = 7L;
    private static final String STORED_PATH = "/data/staff_7/stored.pdf";

    private DocumentRepository documentRepository;
    private LocalFileStorageService fileStorage;
    private StaffDocumentService service;

    @BeforeEach
    void setUp() throws Exception {
        documentRepository = mock(DocumentRepository.class);
        StaffRepository staffRepository = mock(StaffRepository.class);
        fileStorage = mock(LocalFileStorageService.class);
        StaffActivityService activityService = mock(StaffActivityService.class);
        service = new StaffDocumentService(documentRepository, staffRepository, fileStorage, activityService);

        when(staffRepository.findById(STAFF_ID)).thenReturn(Optional.of(new Staff()));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorage.sanitizeFilename(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorage.store(any(), anyString())).thenReturn(STORED_PATH);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void uploadPersistsEnteredNameAsDisplayNameAndOriginalFileName() {
        DocumentResponse response = service.upload(STAFF_ID, pdf(), "OTHER", "Appointment letter 2026");

        assertEquals("Appointment letter 2026", response.getDocumentName());
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeastOnce()).save(captor.capture());
        assertEquals("appointment.pdf", captor.getValue().getFileName());
    }

    @Test
    void uploadDeletesStoredFileWhenTransactionRollsBack() {
        service.upload(STAFF_ID, pdf(), "OTHER", "Certificate");

        completeSynchronizations(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorage).deleteQuietly(STORED_PATH);
    }

    @Test
    void uploadKeepsStoredFileWhenTransactionCommits() {
        service.upload(STAFF_ID, pdf(), "OTHER", "Certificate");

        completeSynchronizations(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileStorage, never()).deleteQuietly(anyString());
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "appointment.pdf", "application/pdf", new byte[]{1, 2, 3});
    }

    private static void completeSynchronizations(int status) {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(status);
        }
    }
}
