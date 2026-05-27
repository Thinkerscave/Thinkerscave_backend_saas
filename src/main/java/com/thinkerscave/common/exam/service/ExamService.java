package com.thinkerscave.common.exam.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.exam.domain.Exam;
import com.thinkerscave.common.exam.domain.ExamSchedule;
import com.thinkerscave.common.exam.domain.ExamStatus;
import com.thinkerscave.common.exam.domain.ExamSubject;
import com.thinkerscave.common.exam.dto.ExamDTO;
import com.thinkerscave.common.exam.dto.ExamScheduleDTO;
import com.thinkerscave.common.exam.dto.ExamSubjectDTO;
import com.thinkerscave.common.exam.repository.ExamRepository;
import com.thinkerscave.common.exam.repository.ExamScheduleRepository;
import com.thinkerscave.common.exam.repository.ExamSubjectRepository;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages {@link Exam} headers along with their {@link ExamSubject} rows
 * and {@link ExamSchedule} sittings. Status transitions are guarded.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final AuditPublisher auditPublisher;

    public Page<ExamDTO> listByYear(Long academicYearId, Pageable pageable) {
        return examRepository.findByOrganizationIdAndAcademicYearId(currentOrgId(), academicYearId, pageable)
                .map(this::toDto);
    }

    public ExamDTO get(Long id) {
        Exam exam = load(id);
        ExamDTO dto = toDto(exam);
        dto.setSubjects(examSubjectRepository.findByExamId(id).stream().map(this::toDto).toList());
        dto.setSchedules(examScheduleRepository.findByExamIdOrderByExamDateAscStartTimeAsc(id)
                .stream().map(this::toDto).toList());
        return dto;
    }

    @Transactional
    public ExamDTO save(ExamDTO dto) {
        Long orgId = currentOrgId();
        Exam exam;
        boolean creating = dto.getId() == null;
        if (creating) {
            examRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(x -> { throw new ConflictException("Exam with code '" + dto.getCode() + "' already exists"); });
            exam = new Exam();
            exam.setOrganizationId(orgId);
        } else {
            exam = load(dto.getId());
        }

        if (dto.getEndDate() != null && dto.getStartDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("Exam end date cannot be before start date");
        }

        exam.setCode(dto.getCode());
        exam.setName(dto.getName());
        exam.setExamTypeId(dto.getExamTypeId());
        exam.setAcademicYearId(dto.getAcademicYearId());
        exam.setClassId(dto.getClassId());
        exam.setSectionId(dto.getSectionId());
        exam.setStartDate(dto.getStartDate());
        exam.setEndDate(dto.getEndDate());
        exam.setGradingScaleId(dto.getGradingScaleId());
        exam.setReportCardTemplateId(dto.getReportCardTemplateId());
        exam.setStatus(dto.getStatus() != null ? dto.getStatus() : ExamStatus.PLANNED);
        exam.setInstructions(dto.getInstructions());
        Exam saved = examRepository.save(exam);

        // Replace subjects + schedules (delete-then-insert)
        if (!creating) {
            examSubjectRepository.deleteByExamId(saved.getId());
            // schedules: delete via bulk
            List<ExamSchedule> existing = examScheduleRepository.findByExamIdOrderByExamDateAscStartTimeAsc(saved.getId());
            examScheduleRepository.deleteAll(existing);
        }
        if (dto.getSubjects() != null) {
            for (ExamSubjectDTO s : dto.getSubjects()) {
                ExamSubject es = new ExamSubject();
                es.setExamId(saved.getId());
                es.setSubjectId(s.getSubjectId());
                es.setMaxMarks(s.getMaxMarks());
                es.setPassingMarks(s.getPassingMarks());
                es.setWeightagePercent(s.getWeightagePercent());
                es.setOptional(s.isOptional());
                examSubjectRepository.save(es);
            }
        }
        if (dto.getSchedules() != null) {
            for (ExamScheduleDTO sc : dto.getSchedules()) {
                ExamSchedule es = new ExamSchedule();
                es.setExamId(saved.getId());
                es.setSubjectId(sc.getSubjectId());
                es.setExamDate(sc.getExamDate());
                es.setStartTime(sc.getStartTime());
                es.setEndTime(sc.getEndTime());
                es.setRoom(sc.getRoom());
                es.setInvigilatorStaffId(sc.getInvigilatorStaffId());
                es.setNotes(sc.getNotes());
                examScheduleRepository.save(es);
            }
        }

        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "exam.create" : "exam.update",
                "Exam", saved.getId(), "Exam " + saved.getCode());
        return get(saved.getId());
    }

    @Transactional
    public ExamDTO transitionStatus(Long id, ExamStatus target) {
        Exam exam = load(id);
        if (exam.getStatus() == target) return toDto(exam);
        // Forward-only transitions; ARCHIVED/CANCELLED allowed from any non-final state.
        if (target == ExamStatus.PLANNED) {
            throw new BadRequestException("Cannot transition back to PLANNED");
        }
        exam.setStatus(target);
        Exam saved = examRepository.save(exam);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "exam.status_change",
                "Exam", saved.getId(), "Exam " + saved.getCode() + " → " + target);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Exam exam = load(id);
        if (exam.getStatus() != ExamStatus.PLANNED && exam.getStatus() != ExamStatus.CANCELLED) {
            throw new BadRequestException("Cannot delete exam in status " + exam.getStatus());
        }
        examSubjectRepository.deleteByExamId(id);
        List<ExamSchedule> sched = examScheduleRepository.findByExamIdOrderByExamDateAscStartTimeAsc(id);
        examScheduleRepository.deleteAll(sched);
        examRepository.delete(exam);
        auditPublisher.publish(AuditEventType.DELETE, "exam.delete",
                "Exam", id, "Exam " + exam.getCode() + " deleted");
    }

    // package-private — used by MarksEntryService & ResultService
    Exam load(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + id));
    }

    List<ExamSubject> subjectsOf(Long examId) {
        return examSubjectRepository.findByExamId(examId);
    }

    private ExamDTO toDto(Exam e) {
        return ExamDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .examTypeId(e.getExamTypeId())
                .academicYearId(e.getAcademicYearId())
                .classId(e.getClassId())
                .sectionId(e.getSectionId())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .gradingScaleId(e.getGradingScaleId())
                .reportCardTemplateId(e.getReportCardTemplateId())
                .status(e.getStatus())
                .instructions(e.getInstructions())
                .subjects(new ArrayList<>())
                .schedules(new ArrayList<>())
                .build();
    }

    private ExamSubjectDTO toDto(ExamSubject es) {
        return ExamSubjectDTO.builder()
                .id(es.getId())
                .subjectId(es.getSubjectId())
                .maxMarks(es.getMaxMarks())
                .passingMarks(es.getPassingMarks())
                .weightagePercent(es.getWeightagePercent())
                .optional(es.isOptional())
                .build();
    }

    private ExamScheduleDTO toDto(ExamSchedule s) {
        return ExamScheduleDTO.builder()
                .id(s.getId())
                .subjectId(s.getSubjectId())
                .examDate(s.getExamDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .room(s.getRoom())
                .invigilatorStaffId(s.getInvigilatorStaffId())
                .notes(s.getNotes())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
