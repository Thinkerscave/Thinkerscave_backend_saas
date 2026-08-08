package com.thinkerscave.student.service.impl;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.dto.request.PromotionBatchCreateRequest;
import com.thinkerscave.student.dto.request.PromotionRecordUpdateRequest;
import com.thinkerscave.student.dto.response.PromotionBatchResponse;
import com.thinkerscave.student.dto.response.PromotionRecordResponse;
import com.thinkerscave.student.entity.PromotionBatch;
import com.thinkerscave.student.entity.PromotionRecord;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.entity.StudentTimeline;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.enums.PromotionBatchStatus;
import com.thinkerscave.student.enums.PromotionDecision;
import com.thinkerscave.student.enums.StudentStatus;
import com.thinkerscave.student.enums.StudentTimelineEventType;
import com.thinkerscave.student.repository.PromotionBatchRepository;
import com.thinkerscave.student.repository.PromotionRecordRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.StudentTimelineRepository;
import com.thinkerscave.student.service.PromotionBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PromotionBatchServiceImpl implements PromotionBatchService {

    private final PromotionBatchRepository batchRepository;
    private final PromotionRecordRepository recordRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final StudentTimelineRepository timelineRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;

    @Override
    public Page<PromotionBatchResponse> list(Pageable pageable) {
        return batchRepository.findAll(pageable).map(this::toBatchResponse);
    }

    @Override
    @Transactional
    public PromotionBatchResponse create(PromotionBatchCreateRequest request) {
        if (Objects.equals(request.getFromAcademicYearId(), request.getToAcademicYearId())) {
            throw new BadRequestException("fromAcademicYearId and toAcademicYearId must differ");
        }
        AcademicYear fromYear = academicYearRepository.findById(request.getFromAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("From academic year not found"));
        AcademicYear toYear = academicYearRepository.findById(request.getToAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("To academic year not found"));

        PromotionBatch batch = new PromotionBatch();
        batch.setBatchCode(StringUtils.hasText(request.getBatchCode())
                ? request.getBatchCode().trim()
                : "PROM-" + fromYear.getAcademicYearId() + "-" + toYear.getAcademicYearId() + "-" + System.currentTimeMillis() % 100000);
        batch.setFromAcademicYearId(fromYear.getAcademicYearId());
        batch.setToAcademicYearId(toYear.getAcademicYearId());
        batch.setStatus(PromotionBatchStatus.DRAFT);
        batch.setRemarks(request.getRemarks());
        batch.setPlannedCount(0);
        batch.setProcessedCount(0);
        return toBatchResponse(batchRepository.save(batch));
    }

    @Override
    @Transactional
    public List<PromotionRecordResponse> preview(Long batchId) {
        PromotionBatch batch = getBatch(batchId);
        if (batch.getStatus() == PromotionBatchStatus.COMPLETED
                || batch.getStatus() == PromotionBatchStatus.CANCELLED
                || batch.getStatus() == PromotionBatchStatus.ROLLED_BACK) {
            throw new BadRequestException("Cannot preview batch in status " + batch.getStatus());
        }

        recordRepository.deleteByBatch_BatchId(batchId);

        List<AcademicClass> fromClasses = classRepository
                .findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(batch.getFromAcademicYearId());
        List<AcademicClass> toClasses = classRepository
                .findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(batch.getToAcademicYearId());

        List<StudentEnrollment> enrollments = enrollmentRepository
                .findActiveByAcademicYearId(batch.getFromAcademicYearId());
        List<PromotionRecord> created = new ArrayList<>();

        for (StudentEnrollment enrollment : enrollments) {
            AcademicClass fromClass = enrollment.getClassEntity();
            Long nextClassId = resolveNextClassId(fromClass, fromClasses, toClasses);

            PromotionRecord record = new PromotionRecord();
            record.setBatch(batch);
            record.setStudentId(enrollment.getStudent().getStudentId());
            record.setFromEnrollmentId(enrollment.getEnrollmentId());
            record.setFromClassId(fromClass != null ? fromClass.getClassId() : null);
            record.setToClassId(nextClassId);
            if (nextClassId != null) {
                record.setDecision(PromotionDecision.PROMOTED);
            } else {
                record.setDecision(PromotionDecision.GRADUATED);
            }
            created.add(recordRepository.save(record));
        }

        batch.setPlannedCount(created.size());
        batch.setStatus(PromotionBatchStatus.IN_PROGRESS);
        batchRepository.save(batch);

        log.info("Promotion preview batch={} records={}", batchId, created.size());
        return created.stream().map(this::toRecordResponse).toList();
    }

    @Override
    public List<PromotionRecordResponse> records(Long batchId) {
        getBatch(batchId);
        return recordRepository.findByBatch_BatchIdOrderByRecordIdAsc(batchId).stream()
                .map(this::toRecordResponse)
                .toList();
    }

    @Override
    @Transactional
    public PromotionRecordResponse updateRecord(Long recordId, PromotionRecordUpdateRequest request) {
        PromotionRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion record not found: " + recordId));
        PromotionBatch batch = record.getBatch();
        if (batch.getStatus() == PromotionBatchStatus.COMPLETED
                || batch.getStatus() == PromotionBatchStatus.CANCELLED
                || batch.getStatus() == PromotionBatchStatus.ROLLED_BACK) {
            throw new BadRequestException("Cannot update records for batch in status " + batch.getStatus());
        }
        if (request.getDecision() != null) {
            record.setDecision(request.getDecision());
        }
        if (request.getToClassId() != null) {
            record.setToClassId(request.getToClassId());
        }
        if (request.getReason() != null) {
            record.setReason(request.getReason());
        }
        if (record.getDecision() == PromotionDecision.PROMOTED && record.getToClassId() == null) {
            throw new BadRequestException("PROMOTED decision requires toClassId");
        }
        return toRecordResponse(recordRepository.save(record));
    }

    @Override
    @Transactional
    public PromotionBatchResponse execute(Long batchId) {
        PromotionBatch batch = getBatch(batchId);
        if (batch.getStatus() != PromotionBatchStatus.DRAFT
                && batch.getStatus() != PromotionBatchStatus.IN_PROGRESS) {
            throw new BadRequestException("Only DRAFT/IN_PROGRESS batches can be executed");
        }
        List<PromotionRecord> records = recordRepository.findByBatch_BatchIdOrderByRecordIdAsc(batchId);
        if (records.isEmpty()) {
            throw new BadRequestException("No promotion records — run preview first");
        }

        AcademicYear toYear = academicYearRepository.findById(batch.getToAcademicYearId())
                .orElseThrow(() -> new ResourceNotFoundException("To academic year not found"));

        int processed = 0;
        for (PromotionRecord record : records) {
            applyDecision(record, toYear);
            processed++;
        }

        batch.setProcessedCount(processed);
        batch.setStatus(PromotionBatchStatus.COMPLETED);
        batch.setExecutedOn(LocalDateTime.now());
        return toBatchResponse(batchRepository.save(batch));
    }

    @Override
    @Transactional
    public PromotionBatchResponse rollback(Long batchId) {
        PromotionBatch batch = getBatch(batchId);
        if (batch.getStatus() != PromotionBatchStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED batches can be rolled back");
        }

        List<PromotionRecord> records = recordRepository.findByBatch_BatchIdOrderByRecordIdAsc(batchId);
        for (PromotionRecord record : records) {
            if (record.getToEnrollmentId() != null) {
                enrollmentRepository.findById(record.getToEnrollmentId()).ifPresent(enrollmentRepository::delete);
                record.setToEnrollmentId(null);
            }
            if (record.getFromEnrollmentId() != null) {
                enrollmentRepository.findById(record.getFromEnrollmentId()).ifPresent(from -> {
                    from.setActive(true);
                    from.setStatus(EnrollmentStatus.ACTIVE);
                    enrollmentRepository.save(from);
                });
            }
            if (record.getDecision() == PromotionDecision.GRADUATED) {
                studentRepository.findById(record.getStudentId()).ifPresent(student -> {
                    if (student.getStatus() == StudentStatus.ALUMNI) {
                        student.setStatus(StudentStatus.ACTIVE);
                        studentRepository.save(student);
                    }
                });
            }
            recordRepository.save(record);
        }

        batch.setStatus(PromotionBatchStatus.ROLLED_BACK);
        batch.setExecutedOn(null);
        batch.setProcessedCount(0);
        return toBatchResponse(batchRepository.save(batch));
    }

    @Override
    @Transactional
    public PromotionBatchResponse cancel(Long batchId) {
        PromotionBatch batch = getBatch(batchId);
        if (batch.getStatus() != PromotionBatchStatus.DRAFT
                && batch.getStatus() != PromotionBatchStatus.IN_PROGRESS) {
            throw new BadRequestException("Only DRAFT/IN_PROGRESS batches can be cancelled");
        }
        batch.setStatus(PromotionBatchStatus.CANCELLED);
        return toBatchResponse(batchRepository.save(batch));
    }

    private void applyDecision(PromotionRecord record, AcademicYear toYear) {
        StudentEnrollment from = record.getFromEnrollmentId() == null ? null
                : enrollmentRepository.findById(record.getFromEnrollmentId()).orElse(null);
        Student student = studentRepository.findById(record.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + record.getStudentId()));

        switch (record.getDecision()) {
            case PROMOTED -> {
                requireFrom(from);
                Long toClassId = record.getToClassId();
                if (toClassId == null) {
                    throw new BadRequestException("PROMOTED requires toClassId for student " + record.getStudentId());
                }
                AcademicClass toClass = classRepository.findById(toClassId)
                        .orElseThrow(() -> new ResourceNotFoundException("Target class not found: " + toClassId));
                if (enrollmentRepository.existsByStudentStudentIdAndAcademicYearAcademicYearId(
                        student.getStudentId(), toYear.getAcademicYearId())) {
                    throw new BadRequestException("Student already enrolled in target year: " + student.getStudentId());
                }
                from.setActive(false);
                from.setStatus(EnrollmentStatus.PROMOTED);
                enrollmentRepository.save(from);

                StudentEnrollment neu = new StudentEnrollment();
                neu.setStudent(student);
                neu.setAcademicYear(toYear);
                neu.setClassEntity(toClass);
                neu.setSection(null);
                neu.setStatus(EnrollmentStatus.ACTIVE);
                neu.setActive(true);
                neu = enrollmentRepository.save(neu);
                record.setToEnrollmentId(neu.getEnrollmentId());
                addTimeline(student, StudentTimelineEventType.PROMOTED,
                        "Promoted", "Promoted to class " + toClass.getClassName());
            }
            case RETAINED, WITHHELD -> {
                requireFrom(from);
                AcademicClass target = record.getToClassId() != null
                        ? classRepository.findById(record.getToClassId()).orElse(from.getClassEntity())
                        : from.getClassEntity();
                if (enrollmentRepository.existsByStudentStudentIdAndAcademicYearAcademicYearId(
                        student.getStudentId(), toYear.getAcademicYearId())) {
                    throw new BadRequestException("Student already enrolled in target year: " + student.getStudentId());
                }
                from.setActive(false);
                from.setStatus(EnrollmentStatus.PROMOTED);
                enrollmentRepository.save(from);

                StudentEnrollment neu = new StudentEnrollment();
                neu.setStudent(student);
                neu.setAcademicYear(toYear);
                neu.setClassEntity(target);
                neu.setStatus(EnrollmentStatus.ACTIVE);
                neu.setActive(true);
                neu = enrollmentRepository.save(neu);
                record.setToEnrollmentId(neu.getEnrollmentId());
                record.setToClassId(target.getClassId());
                addTimeline(student, StudentTimelineEventType.PROMOTED,
                        record.getDecision().name(),
                        "Carried to next year as " + record.getDecision().name());
            }
            case GRADUATED -> {
                requireFrom(from);
                from.setActive(false);
                from.setStatus(EnrollmentStatus.GRADUATED);
                enrollmentRepository.save(from);
                student.setStatus(StudentStatus.ALUMNI);
                studentRepository.save(student);
                addTimeline(student, StudentTimelineEventType.PROMOTED,
                        "Graduated", "Marked graduated via promotion batch");
            }
            case TRANSFERRED_OUT -> {
                requireFrom(from);
                from.setActive(false);
                from.setStatus(EnrollmentStatus.TRANSFERRED);
                enrollmentRepository.save(from);
                addTimeline(student, StudentTimelineEventType.PROMOTED,
                        "Transferred out", "Marked transferred out via promotion batch");
            }
        }
        recordRepository.save(record);
    }

    private static void requireFrom(StudentEnrollment from) {
        if (from == null) {
            throw new BadRequestException("Source enrollment missing for promotion record");
        }
    }

    private Long resolveNextClassId(AcademicClass fromClass,
                                    List<AcademicClass> fromClasses,
                                    List<AcademicClass> toClasses) {
        if (fromClass == null || toClasses.isEmpty()) {
            return null;
        }
        int idx = -1;
        for (int i = 0; i < fromClasses.size(); i++) {
            if (Objects.equals(fromClasses.get(i).getClassId(), fromClass.getClassId())) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            // fall back: match by class code then take next in to-year by display order
            for (int i = 0; i < toClasses.size(); i++) {
                if (toClasses.get(i).getClassCode() != null
                        && toClasses.get(i).getClassCode().equalsIgnoreCase(fromClass.getClassCode())) {
                    return (i + 1) < toClasses.size() ? toClasses.get(i + 1).getClassId() : null;
                }
            }
            return null;
        }
        int next = idx + 1;
        if (next >= toClasses.size()) {
            return null;
        }
        return toClasses.get(next).getClassId();
    }

    private void addTimeline(Student student, StudentTimelineEventType type, String title, String description) {
        StudentTimeline timeline = new StudentTimeline();
        timeline.setStudent(student);
        timeline.setEventType(type);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timelineRepository.save(timeline);
    }

    private PromotionBatch getBatch(Long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion batch not found: " + batchId));
    }

    private PromotionBatchResponse toBatchResponse(PromotionBatch batch) {
        return PromotionBatchResponse.builder()
                .id(batch.getBatchId())
                .batchCode(batch.getBatchCode())
                .batchNumber(batch.getBatchCode())
                .fromAcademicYearId(batch.getFromAcademicYearId())
                .toAcademicYearId(batch.getToAcademicYearId())
                .status(batch.getStatus())
                .plannedCount(batch.getPlannedCount())
                .processedCount(batch.getProcessedCount())
                .executedOn(batch.getExecutedOn())
                .createdAt(batch.getCreatedOn())
                .build();
    }

    private PromotionRecordResponse toRecordResponse(PromotionRecord record) {
        return PromotionRecordResponse.builder()
                .id(record.getRecordId())
                .batchId(record.getBatch().getBatchId())
                .studentId(record.getStudentId())
                .fromEnrollmentId(record.getFromEnrollmentId())
                .toEnrollmentId(record.getToEnrollmentId())
                .fromClassId(record.getFromClassId())
                .toClassId(record.getToClassId())
                .decision(record.getDecision())
                .reason(record.getReason())
                .build();
    }
}
