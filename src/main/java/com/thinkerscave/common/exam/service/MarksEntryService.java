package com.thinkerscave.common.exam.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exam.domain.Exam;
import com.thinkerscave.common.exam.domain.ExamStatus;
import com.thinkerscave.common.exam.domain.ExamSubject;
import com.thinkerscave.common.exam.domain.MarksEntry;
import com.thinkerscave.common.exam.domain.MarksStatus;
import com.thinkerscave.common.exam.dto.MarksEntryDTO;
import com.thinkerscave.common.exam.repository.MarksEntryRepository;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow for entering, submitting, approving, and locking student marks.
 * Validates against the {@link ExamSubject} max marks.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MarksEntryService {

    private final MarksEntryRepository marksEntryRepository;
    private final ExamService examService;
    private final AuditPublisher auditPublisher;

    public List<MarksEntryDTO> listForSubject(Long examId, Long subjectId) {
        return marksEntryRepository.findByExamIdAndSubjectId(examId, subjectId)
                .stream().map(this::toDto).toList();
    }

    public List<MarksEntryDTO> listForStudent(Long examId, Long studentId) {
        return marksEntryRepository.findByExamIdAndStudentId(examId, studentId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public List<MarksEntryDTO> upsertBatch(Long examId, Long subjectId, List<MarksEntryDTO> entries) {
        Exam exam = examService.load(examId);
        if (exam.getStatus() == ExamStatus.RESULT_DECLARED
                || exam.getStatus() == ExamStatus.ARCHIVED
                || exam.getStatus() == ExamStatus.CANCELLED) {
            throw new BadRequestException("Cannot edit marks for exam in status " + exam.getStatus());
        }
        ExamSubject subject = findSubject(exam, subjectId);
        BigDecimal maxMarks = subject.getMaxMarks();
        Long orgId = currentOrgId();

        List<MarksEntryDTO> out = new ArrayList<>();
        for (MarksEntryDTO dto : entries) {
            if (dto.getMarksObtained() != null && dto.getMarksObtained().compareTo(maxMarks) > 0) {
                throw new BadRequestException("Marks " + dto.getMarksObtained() +
                        " exceed max " + maxMarks + " for student " + dto.getStudentId());
            }
            MarksEntry me = marksEntryRepository
                    .findByExamIdAndSubjectIdAndStudentId(examId, subjectId, dto.getStudentId())
                    .orElseGet(() -> {
                        MarksEntry n = new MarksEntry();
                        n.setOrganizationId(orgId);
                        n.setExamId(examId);
                        n.setSubjectId(subjectId);
                        n.setStudentId(dto.getStudentId());
                        n.setEnrollmentId(dto.getEnrollmentId());
                        n.setStatus(MarksStatus.IN_PROGRESS);
                        return n;
                    });

            if (me.getStatus() == MarksStatus.LOCKED) {
                throw new BadRequestException("Marks already LOCKED for student " + dto.getStudentId());
            }
            me.setMaxMarks(maxMarks);
            me.setAbsent(dto.isAbsent());
            me.setMarksObtained(dto.isAbsent() ? null : dto.getMarksObtained());
            me.setGradeCode(dto.getGradeCode());
            me.setRemarks(dto.getRemarks());
            me.setEnteredByUserId(dto.getEnteredByUserId());
            me.setStatus(MarksStatus.IN_PROGRESS);
            out.add(toDto(marksEntryRepository.save(me)));
        }
        auditPublisher.publish(AuditEventType.UPDATE, "marks.upsert",
                "MarksEntry", examId + ":" + subjectId,
                "Saved " + entries.size() + " marks rows");
        return out;
    }

    @Transactional
    public void submit(Long examId, Long subjectId) {
        transition(examId, subjectId, MarksStatus.SUBMITTED, "marks.submit");
    }

    @Transactional
    public void approve(Long examId, Long subjectId, Long approvedByUserId) {
        Exam exam = examService.load(examId);
        List<MarksEntry> rows = marksEntryRepository.findByExamIdAndSubjectId(examId, subjectId);
        if (rows.isEmpty()) throw new BadRequestException("No marks to approve");
        for (MarksEntry me : rows) {
            if (me.getStatus() != MarksStatus.SUBMITTED && me.getStatus() != MarksStatus.APPROVED) {
                throw new BadRequestException("Cannot approve marks in status " + me.getStatus());
            }
            me.setStatus(MarksStatus.APPROVED);
            me.setApprovedByUserId(approvedByUserId);
            marksEntryRepository.save(me);
        }
        auditPublisher.publish(AuditEventType.APPROVAL, "marks.approve",
                "MarksEntry", examId + ":" + subjectId,
                "Approved marks for exam " + exam.getCode());
    }

    @Transactional
    public void lock(Long examId, Long subjectId) {
        transition(examId, subjectId, MarksStatus.LOCKED, "marks.lock");
    }

    @Transactional
    public void reopen(Long examId, Long subjectId) {
        List<MarksEntry> rows = marksEntryRepository.findByExamIdAndSubjectId(examId, subjectId);
        for (MarksEntry me : rows) {
            me.setStatus(MarksStatus.REOPENED);
            marksEntryRepository.save(me);
        }
        auditPublisher.publish(AuditEventType.UPDATE, "marks.reopen",
                "MarksEntry", examId + ":" + subjectId, "Marks reopened");
    }

    public Map<MarksStatus, Long> statusSummary(Long examId) {
        Map<MarksStatus, Long> out = new HashMap<>();
        for (MarksStatus s : MarksStatus.values()) {
            out.put(s, marksEntryRepository.countByExamIdAndStatus(examId, s));
        }
        return out;
    }

    // -- helpers --

    private void transition(Long examId, Long subjectId, MarksStatus target, String action) {
        List<MarksEntry> rows = marksEntryRepository.findByExamIdAndSubjectId(examId, subjectId);
        if (rows.isEmpty()) throw new BadRequestException("No marks rows for this subject");
        for (MarksEntry me : rows) {
            me.setStatus(target);
            marksEntryRepository.save(me);
        }
        auditPublisher.publish(AuditEventType.UPDATE, action,
                "MarksEntry", examId + ":" + subjectId, "Marks → " + target);
    }

    private ExamSubject findSubject(Exam exam, Long subjectId) {
        return examService.subjectsOf(exam.getId()).stream()
                .filter(s -> s.getSubjectId().equals(subjectId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject " + subjectId + " is not in exam " + exam.getCode()));
    }

    private MarksEntryDTO toDto(MarksEntry me) {
        return MarksEntryDTO.builder()
                .id(me.getId())
                .examId(me.getExamId())
                .subjectId(me.getSubjectId())
                .studentId(me.getStudentId())
                .enrollmentId(me.getEnrollmentId())
                .marksObtained(me.getMarksObtained())
                .maxMarks(me.getMaxMarks())
                .gradeCode(me.getGradeCode())
                .absent(me.isAbsent())
                .status(me.getStatus())
                .enteredByUserId(me.getEnteredByUserId())
                .approvedByUserId(me.getApprovedByUserId())
                .remarks(me.getRemarks())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
