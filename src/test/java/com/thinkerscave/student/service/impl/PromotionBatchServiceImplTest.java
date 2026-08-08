package com.thinkerscave.student.service.impl;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.student.dto.request.PromotionBatchCreateRequest;
import com.thinkerscave.student.dto.response.PromotionBatchResponse;
import com.thinkerscave.student.dto.response.PromotionRecordResponse;
import com.thinkerscave.student.entity.PromotionBatch;
import com.thinkerscave.student.entity.PromotionRecord;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.enums.PromotionBatchStatus;
import com.thinkerscave.student.enums.PromotionDecision;
import com.thinkerscave.student.repository.PromotionBatchRepository;
import com.thinkerscave.student.repository.PromotionRecordRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.StudentTimelineRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionBatchServiceImpl")
class PromotionBatchServiceImplTest {

    @Mock private PromotionBatchRepository batchRepository;
    @Mock private PromotionRecordRepository recordRepository;
    @Mock private StudentEnrollmentRepository enrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentTimelineRepository timelineRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ClassRepository classRepository;

    @InjectMocks
    private PromotionBatchServiceImpl service;

    @Test
    @DisplayName("create rejects same from/to academic year")
    void createRejectsSameYear() {
        PromotionBatchCreateRequest req = new PromotionBatchCreateRequest();
        req.setFromAcademicYearId(1L);
        req.setToAcademicYearId(1L);
        assertThrows(BadRequestException.class, () -> service.create(req));
        verifyNoInteractions(batchRepository);
    }

    @Test
    @DisplayName("create persists DRAFT batch")
    void createDraft() {
        AcademicYear from = year(10L);
        AcademicYear to = year(11L);
        when(academicYearRepository.findById(10L)).thenReturn(Optional.of(from));
        when(academicYearRepository.findById(11L)).thenReturn(Optional.of(to));
        when(batchRepository.save(any(PromotionBatch.class))).thenAnswer(inv -> {
            PromotionBatch b = inv.getArgument(0);
            b.setBatchId(5L);
            return b;
        });

        PromotionBatchCreateRequest req = new PromotionBatchCreateRequest();
        req.setFromAcademicYearId(10L);
        req.setToAcademicYearId(11L);

        PromotionBatchResponse resp = service.create(req);
        assertEquals(5L, resp.getId());
        assertEquals(PromotionBatchStatus.DRAFT, resp.getStatus());
        assertEquals(10L, resp.getFromAcademicYearId());
        assertEquals(11L, resp.getToAcademicYearId());
    }

    @Test
    @DisplayName("preview builds PROMOTED record to next display-order class")
    void previewMapsNextClass() {
        PromotionBatch batch = new PromotionBatch();
        batch.setBatchId(1L);
        batch.setFromAcademicYearId(10L);
        batch.setToAcademicYearId(11L);
        batch.setStatus(PromotionBatchStatus.DRAFT);

        AcademicClass c1 = clazz(100L, "1", 1);
        AcademicClass c2 = clazz(101L, "2", 2);
        AcademicClass t1 = clazz(200L, "1", 1);
        AcademicClass t2 = clazz(201L, "2", 2);

        Student student = new Student();
        student.setStudentId(9L);
        StudentEnrollment enr = new StudentEnrollment();
        enr.setEnrollmentId(50L);
        enr.setStudent(student);
        enr.setClassEntity(c1);
        enr.setStatus(EnrollmentStatus.ACTIVE);
        enr.setActive(true);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(c1, c2));
        when(classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(11L))
                .thenReturn(List.of(t1, t2));
        when(enrollmentRepository.findActiveByAcademicYearId(10L)).thenReturn(List.of(enr));
        when(recordRepository.save(any(PromotionRecord.class))).thenAnswer(inv -> {
            PromotionRecord r = inv.getArgument(0);
            r.setRecordId(77L);
            return r;
        });
        when(batchRepository.save(any(PromotionBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PromotionRecordResponse> records = service.preview(1L);

        assertEquals(1, records.size());
        assertEquals(PromotionDecision.PROMOTED, records.get(0).getDecision());
        assertEquals(201L, records.get(0).getToClassId());
        assertEquals(PromotionBatchStatus.IN_PROGRESS, batch.getStatus());
        assertEquals(1, batch.getPlannedCount());
    }

    @Test
    @DisplayName("execute applies PROMOTED enrollment mutation")
    void executePromoted() {
        PromotionBatch batch = new PromotionBatch();
        batch.setBatchId(1L);
        batch.setFromAcademicYearId(10L);
        batch.setToAcademicYearId(11L);
        batch.setStatus(PromotionBatchStatus.IN_PROGRESS);

        AcademicYear toYear = year(11L);
        AcademicClass toClass = clazz(201L, "2", 2);

        Student student = new Student();
        student.setStudentId(9L);

        StudentEnrollment from = new StudentEnrollment();
        from.setEnrollmentId(50L);
        from.setStudent(student);
        from.setActive(true);
        from.setStatus(EnrollmentStatus.ACTIVE);

        PromotionRecord record = new PromotionRecord();
        record.setRecordId(77L);
        record.setBatch(batch);
        record.setStudentId(9L);
        record.setFromEnrollmentId(50L);
        record.setFromClassId(100L);
        record.setToClassId(201L);
        record.setDecision(PromotionDecision.PROMOTED);

        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(recordRepository.findByBatch_BatchIdOrderByRecordIdAsc(1L)).thenReturn(List.of(record));
        when(academicYearRepository.findById(11L)).thenReturn(Optional.of(toYear));
        when(enrollmentRepository.findById(50L)).thenReturn(Optional.of(from));
        when(studentRepository.findById(9L)).thenReturn(Optional.of(student));
        when(classRepository.findById(201L)).thenReturn(Optional.of(toClass));
        when(enrollmentRepository.existsByStudentStudentIdAndAcademicYearAcademicYearId(9L, 11L)).thenReturn(false);
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(inv -> {
            StudentEnrollment e = inv.getArgument(0);
            if (e.getEnrollmentId() == null) {
                e.setEnrollmentId(90L);
            }
            return e;
        });
        when(batchRepository.save(any(PromotionBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recordRepository.save(any(PromotionRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timelineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromotionBatchResponse resp = service.execute(1L);

        assertEquals(PromotionBatchStatus.COMPLETED, resp.getStatus());
        assertEquals(1, resp.getProcessedCount());
        assertFalse(from.getActive());
        assertEquals(EnrollmentStatus.PROMOTED, from.getStatus());
        assertEquals(90L, record.getToEnrollmentId());

        ArgumentCaptor<StudentEnrollment> captor = ArgumentCaptor.forClass(StudentEnrollment.class);
        verify(enrollmentRepository, atLeast(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(e -> Boolean.TRUE.equals(e.getActive())
                && e.getClassEntity() != null
                && e.getClassEntity().getClassId().equals(201L)));
    }

    @Test
    @DisplayName("cancel rejects COMPLETED batch")
    void cancelRejectsCompleted() {
        PromotionBatch batch = new PromotionBatch();
        batch.setBatchId(1L);
        batch.setStatus(PromotionBatchStatus.COMPLETED);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        assertThrows(BadRequestException.class, () -> service.cancel(1L));
    }

    private static AcademicYear year(Long id) {
        AcademicYear y = new AcademicYear();
        y.setAcademicYearId(id);
        return y;
    }

    private static AcademicClass clazz(Long id, String code, int order) {
        AcademicClass c = new AcademicClass();
        c.setClassId(id);
        c.setClassCode(code);
        c.setClassName("Class " + code);
        c.setDisplayOrder(order);
        return c;
    }
}
