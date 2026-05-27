package com.thinkerscave.common.exam.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enums.GenericStatus;
import com.thinkerscave.common.exam.domain.ExamType;
import com.thinkerscave.common.exam.domain.GradeBoundary;
import com.thinkerscave.common.exam.domain.GradingScale;
import com.thinkerscave.common.exam.domain.ReportCardTemplate;
import com.thinkerscave.common.exam.dto.ExamTypeDTO;
import com.thinkerscave.common.exam.dto.GradeBoundaryDTO;
import com.thinkerscave.common.exam.dto.GradingScaleDTO;
import com.thinkerscave.common.exam.dto.ReportCardTemplateDTO;
import com.thinkerscave.common.exam.repository.ExamTypeRepository;
import com.thinkerscave.common.exam.repository.GradeBoundaryRepository;
import com.thinkerscave.common.exam.repository.GradingScaleRepository;
import com.thinkerscave.common.exam.repository.ReportCardTemplateRepository;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD for exam-master entities: {@link ExamType}, {@link GradingScale} (with
 * boundaries), and {@link ReportCardTemplate}. Edited together in the
 * Exam Setup workspace.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExamMasterService {

    private final ExamTypeRepository examTypeRepository;
    private final GradingScaleRepository gradingScaleRepository;
    private final GradeBoundaryRepository gradeBoundaryRepository;
    private final ReportCardTemplateRepository reportCardTemplateRepository;
    private final AuditPublisher auditPublisher;

    // ------------------------------------------------------- Exam Type ----

    public List<ExamTypeDTO> listExamTypes() {
        return examTypeRepository.findByOrganizationId(currentOrgId()).stream().map(this::toDto).toList();
    }

    public ExamTypeDTO getExamType(Long id) { return toDto(loadType(id)); }

    @Transactional
    public ExamTypeDTO saveExamType(ExamTypeDTO dto) {
        Long orgId = currentOrgId();
        ExamType e;
        boolean creating = dto.getId() == null;
        if (creating) {
            examTypeRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(x -> { throw new ConflictException("Exam type with code '" + dto.getCode() + "' already exists"); });
            e = new ExamType();
            e.setOrganizationId(orgId);
        } else {
            e = loadType(dto.getId());
        }
        e.setCode(dto.getCode());
        e.setName(dto.getName());
        e.setDescription(dto.getDescription());
        e.setWeightagePercent(dto.getWeightagePercent());
        e.setFinalTerm(dto.isFinalTerm());
        e.setDisplayOrder(dto.getDisplayOrder());
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        ExamType saved = examTypeRepository.save(e);
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "exam_type.create" : "exam_type.update",
                "ExamType", saved.getId(), "Exam type " + saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public void deleteExamType(Long id) {
        ExamType e = loadType(id);
        examTypeRepository.delete(e);
        auditPublisher.publish(AuditEventType.DELETE, "exam_type.delete",
                "ExamType", id, "Exam type " + e.getCode() + " deleted");
    }

    // ---------------------------------------------------- Grading Scale ---

    public List<GradingScaleDTO> listGradingScales() {
        return gradingScaleRepository.findByOrganizationId(currentOrgId()).stream()
                .map(this::toDtoWithBoundaries).toList();
    }

    public GradingScaleDTO getGradingScale(Long id) {
        return toDtoWithBoundaries(loadScale(id));
    }

    @Transactional
    public GradingScaleDTO saveGradingScale(GradingScaleDTO dto) {
        Long orgId = currentOrgId();
        GradingScale gs;
        boolean creating = dto.getId() == null;
        if (creating) {
            gradingScaleRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(x -> { throw new ConflictException("Grading scale with code '" + dto.getCode() + "' already exists"); });
            gs = new GradingScale();
            gs.setOrganizationId(orgId);
        } else {
            gs = loadScale(dto.getId());
        }
        gs.setCode(dto.getCode());
        gs.setName(dto.getName());
        gs.setDescription(dto.getDescription());
        gs.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        GradingScale saved = gradingScaleRepository.save(gs);

        // Replace boundaries (delete-then-insert)
        if (!creating) gradeBoundaryRepository.deleteByGradingScaleId(saved.getId());
        if (dto.getBoundaries() != null) {
            for (GradeBoundaryDTO b : dto.getBoundaries()) {
                GradeBoundary gb = new GradeBoundary();
                gb.setGradingScaleId(saved.getId());
                gb.setGradeCode(b.getGradeCode());
                gb.setGradeLabel(b.getGradeLabel());
                gb.setMinPercent(b.getMinPercent());
                gb.setMaxPercent(b.getMaxPercent());
                gb.setGradePoint(b.getGradePoint());
                gb.setPass(b.isPass());
                gb.setDisplayOrder(b.getDisplayOrder());
                gradeBoundaryRepository.save(gb);
            }
        }

        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "grading_scale.save" : "grading_scale.update",
                "GradingScale", saved.getId(), "Grading scale " + saved.getCode());
        return toDtoWithBoundaries(saved);
    }

    @Transactional
    public void deleteGradingScale(Long id) {
        GradingScale gs = loadScale(id);
        gradeBoundaryRepository.deleteByGradingScaleId(id);
        gradingScaleRepository.delete(gs);
        auditPublisher.publish(AuditEventType.DELETE, "grading_scale.delete",
                "GradingScale", id, "Grading scale " + gs.getCode() + " deleted");
    }

    // ----------------------------------------------- Report Card Template -

    public List<ReportCardTemplateDTO> listReportCardTemplates() {
        return reportCardTemplateRepository.findByOrganizationId(currentOrgId()).stream()
                .map(this::toDto).toList();
    }

    public ReportCardTemplateDTO getReportCardTemplate(Long id) {
        return toDto(loadTemplate(id));
    }

    @Transactional
    public ReportCardTemplateDTO saveReportCardTemplate(ReportCardTemplateDTO dto) {
        Long orgId = currentOrgId();
        ReportCardTemplate t;
        boolean creating = dto.getId() == null;
        if (creating) {
            reportCardTemplateRepository.findByOrganizationIdAndCode(orgId, dto.getCode())
                    .ifPresent(x -> { throw new ConflictException("Report card template with code '" + dto.getCode() + "' already exists"); });
            t = new ReportCardTemplate();
            t.setOrganizationId(orgId);
        } else {
            t = loadTemplate(dto.getId());
        }
        t.setCode(dto.getCode());
        t.setName(dto.getName());
        t.setDescription(dto.getDescription());
        t.setHeaderText(dto.getHeaderText());
        t.setFooterText(dto.getFooterText());
        t.setShowAttendance(dto.isShowAttendance());
        t.setShowRank(dto.isShowRank());
        t.setShowRemarks(dto.isShowRemarks());
        t.setShowCoCurricular(dto.isShowCoCurricular());
        t.setLayoutDefinition(dto.getLayoutDefinition());
        t.setStatus(dto.getStatus() != null ? dto.getStatus() : GenericStatus.ACTIVE);
        ReportCardTemplate saved = reportCardTemplateRepository.save(t);
        auditPublisher.publish(creating ? AuditEventType.CREATE : AuditEventType.UPDATE,
                creating ? "report_card_template.create" : "report_card_template.update",
                "ReportCardTemplate", saved.getId(), "Report card template " + saved.getCode());
        return toDto(saved);
    }

    @Transactional
    public void deleteReportCardTemplate(Long id) {
        ReportCardTemplate t = loadTemplate(id);
        reportCardTemplateRepository.delete(t);
        auditPublisher.publish(AuditEventType.DELETE, "report_card_template.delete",
                "ReportCardTemplate", id, "Report card template " + t.getCode() + " deleted");
    }

    // ----------------------------------------------------------- Helpers --

    GradingScale loadScale(Long id) {
        return gradingScaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingScale not found: " + id));
    }

    private ExamType loadType(Long id) {
        return examTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExamType not found: " + id));
    }

    private ReportCardTemplate loadTemplate(Long id) {
        return reportCardTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportCardTemplate not found: " + id));
    }

    public List<GradeBoundary> boundariesOf(Long scaleId) {
        return gradeBoundaryRepository.findByGradingScaleIdOrderByDisplayOrderAsc(scaleId);
    }

    private ExamTypeDTO toDto(ExamType e) {
        return ExamTypeDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .weightagePercent(e.getWeightagePercent())
                .finalTerm(e.isFinalTerm())
                .displayOrder(e.getDisplayOrder())
                .status(e.getStatus())
                .build();
    }

    private GradingScaleDTO toDtoWithBoundaries(GradingScale gs) {
        List<GradeBoundaryDTO> bs = new ArrayList<>();
        for (GradeBoundary b : boundariesOf(gs.getId())) {
            bs.add(GradeBoundaryDTO.builder()
                    .id(b.getId())
                    .gradeCode(b.getGradeCode())
                    .gradeLabel(b.getGradeLabel())
                    .minPercent(b.getMinPercent())
                    .maxPercent(b.getMaxPercent())
                    .gradePoint(b.getGradePoint())
                    .pass(b.isPass())
                    .displayOrder(b.getDisplayOrder())
                    .build());
        }
        return GradingScaleDTO.builder()
                .id(gs.getId())
                .code(gs.getCode())
                .name(gs.getName())
                .description(gs.getDescription())
                .status(gs.getStatus())
                .boundaries(bs)
                .build();
    }

    private ReportCardTemplateDTO toDto(ReportCardTemplate t) {
        return ReportCardTemplateDTO.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .description(t.getDescription())
                .headerText(t.getHeaderText())
                .footerText(t.getFooterText())
                .showAttendance(t.isShowAttendance())
                .showRank(t.isShowRank())
                .showRemarks(t.isShowRemarks())
                .showCoCurricular(t.isShowCoCurricular())
                .layoutDefinition(t.getLayoutDefinition())
                .status(t.getStatus())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
