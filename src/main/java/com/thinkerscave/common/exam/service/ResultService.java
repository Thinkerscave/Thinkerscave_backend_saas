package com.thinkerscave.common.exam.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exam.domain.Exam;
import com.thinkerscave.common.exam.domain.ExamStatus;
import com.thinkerscave.common.exam.domain.ExamSubject;
import com.thinkerscave.common.exam.domain.GradeBoundary;
import com.thinkerscave.common.exam.domain.MarksEntry;
import com.thinkerscave.common.exam.domain.MarksStatus;
import com.thinkerscave.common.exam.domain.Result;
import com.thinkerscave.common.exam.domain.ResultStatus;
import com.thinkerscave.common.exam.dto.ResultDTO;
import com.thinkerscave.common.exam.repository.MarksEntryRepository;
import com.thinkerscave.common.exam.repository.ResultRepository;
import com.thinkerscave.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes per-student aggregated {@link Result} from approved/locked
 * {@link MarksEntry} rows, applies grade boundaries, and assigns ranks.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ResultService {

    private final ResultRepository resultRepository;
    private final MarksEntryRepository marksEntryRepository;
    private final ExamService examService;
    private final ExamMasterService examMasterService;
    private final AuditPublisher auditPublisher;

    public Page<ResultDTO> listByExam(Long examId, Pageable pageable) {
        return resultRepository.findByExamId(examId, pageable).map(this::toDto);
    }

    public List<ResultDTO> listByStudent(Long studentId) {
        return resultRepository.findByOrganizationIdAndStudentIdOrderByIdDesc(currentOrgId(), studentId)
                .stream().map(this::toDto).toList();
    }

    public ResultDTO getForStudent(Long examId, Long studentId) {
        return resultRepository.findByExamIdAndStudentId(examId, studentId)
                .map(this::toDto).orElse(null);
    }

    /**
     * Recompute results for every student in an exam from current marks.
     * Idempotent — safe to re-run after a marks correction.
     */
    @Transactional
    public int computeAll(Long examId) {
        Exam exam = examService.load(examId);
        if (exam.getStatus() == ExamStatus.PLANNED || exam.getStatus() == ExamStatus.SCHEDULED) {
            throw new BadRequestException("Exam must be at least in MARKS_ENTRY before computing results");
        }
        Long orgId = currentOrgId();

        // Group marks by student
        List<MarksEntry> all = marksEntryRepository.findAll().stream()
                .filter(m -> m.getExamId().equals(examId))
                .toList();

        Map<Long, List<MarksEntry>> byStudent = new HashMap<>();
        for (MarksEntry me : all) {
            byStudent.computeIfAbsent(me.getStudentId(), k -> new java.util.ArrayList<>()).add(me);
        }

        List<ExamSubject> subjects = examService.subjectsOf(examId);
        List<GradeBoundary> boundaries = exam.getGradingScaleId() != null
                ? examMasterService.boundariesOf(exam.getGradingScaleId())
                : List.of();

        int count = 0;
        for (Map.Entry<Long, List<MarksEntry>> entry : byStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<MarksEntry> rows = entry.getValue();

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal max = BigDecimal.ZERO;
            boolean anyAbsent = false;
            boolean anyFail = false;
            Long enrollmentId = rows.isEmpty() ? null : rows.get(0).getEnrollmentId();

            for (MarksEntry me : rows) {
                if (me.isAbsent()) { anyAbsent = true; continue; }
                if (me.getStatus() != MarksStatus.APPROVED && me.getStatus() != MarksStatus.LOCKED) continue;
                BigDecimal obtained = me.getMarksObtained() != null ? me.getMarksObtained() : BigDecimal.ZERO;
                total = total.add(obtained);
                ExamSubject sub = subjects.stream()
                        .filter(s -> s.getSubjectId().equals(me.getSubjectId()))
                        .findFirst().orElse(null);
                if (sub != null) {
                    max = max.add(sub.getMaxMarks());
                    if (obtained.compareTo(sub.getPassingMarks()) < 0) anyFail = true;
                }
            }

            BigDecimal percentage = max.compareTo(BigDecimal.ZERO) > 0
                    ? total.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String gradeCode = null;
            BigDecimal gpa = null;
            for (GradeBoundary b : boundaries) {
                if (percentage.compareTo(b.getMinPercent()) >= 0 && percentage.compareTo(b.getMaxPercent()) <= 0) {
                    gradeCode = b.getGradeCode();
                    gpa = b.getGradePoint();
                    break;
                }
            }

            Result r = resultRepository.findByExamIdAndStudentId(examId, studentId).orElseGet(() -> {
                Result nr = new Result();
                nr.setOrganizationId(orgId);
                nr.setExamId(examId);
                nr.setStudentId(studentId);
                return nr;
            });
            r.setEnrollmentId(enrollmentId);
            r.setTotalMarks(total);
            r.setMaxMarks(max);
            r.setPercentage(percentage);
            r.setGpa(gpa);
            r.setGradeCode(gradeCode);
            r.setStatus(anyAbsent ? ResultStatus.ABSENT : anyFail ? ResultStatus.FAIL : ResultStatus.PASS);
            resultRepository.save(r);
            count++;
        }

        // Compute ranks (class-level)
        assignRanks(examId);

        auditPublisher.publish(AuditEventType.UPDATE, "result.compute",
                "Result", examId, "Computed " + count + " student results");
        return count;
    }

    @Transactional
    public ResultDTO declare(Long examId) {
        Exam exam = examService.load(examId);
        if (exam.getStatus() != ExamStatus.EVALUATION) {
            throw new BadRequestException("Exam must be in EVALUATION to declare results");
        }
        exam.setStatus(ExamStatus.RESULT_DECLARED);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "exam.result_declared",
                "Exam", exam.getId(), "Results declared for " + exam.getCode());
        return null;
    }

    private void assignRanks(Long examId) {
        List<Result> all = resultRepository.findByExamId(examId, Pageable.unpaged()).getContent();
        all.sort((a, b) -> {
            BigDecimal pa = a.getPercentage() != null ? a.getPercentage() : BigDecimal.ZERO;
            BigDecimal pb = b.getPercentage() != null ? b.getPercentage() : BigDecimal.ZERO;
            return pb.compareTo(pa);
        });
        int rank = 0;
        BigDecimal lastPct = null;
        int sequence = 0;
        for (Result r : all) {
            sequence++;
            BigDecimal pct = r.getPercentage() != null ? r.getPercentage() : BigDecimal.ZERO;
            if (lastPct == null || pct.compareTo(lastPct) != 0) {
                rank = sequence;
                lastPct = pct;
            }
            r.setClassRank(rank);
            resultRepository.save(r);
        }
    }

    private ResultDTO toDto(Result r) {
        return ResultDTO.builder()
                .id(r.getId())
                .examId(r.getExamId())
                .studentId(r.getStudentId())
                .enrollmentId(r.getEnrollmentId())
                .totalMarks(r.getTotalMarks())
                .maxMarks(r.getMaxMarks())
                .percentage(r.getPercentage())
                .gpa(r.getGpa())
                .gradeCode(r.getGradeCode())
                .classRank(r.getClassRank())
                .sectionRank(r.getSectionRank())
                .status(r.getStatus())
                .remarks(r.getRemarks())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
