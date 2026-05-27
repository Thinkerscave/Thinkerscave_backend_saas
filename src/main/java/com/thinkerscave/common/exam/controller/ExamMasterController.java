package com.thinkerscave.common.exam.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.exam.dto.ExamTypeDTO;
import com.thinkerscave.common.exam.dto.GradingScaleDTO;
import com.thinkerscave.common.exam.dto.ReportCardTemplateDTO;
import com.thinkerscave.common.exam.service.ExamMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam/masters")
@Tag(name = "Exam Masters", description = "Exam types, grading scales, report card templates")
@RequiredArgsConstructor
@Slf4j
public class ExamMasterController {

    private final ExamMasterService examMasterService;

    // ------ Exam Type ------
    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_MASTER_VIEW')")
    @Operation(summary = "List exam types")
    public ResponseEntity<ApiResponse<List<ExamTypeDTO>>> listTypes() {
        return ResponseEntity.ok(ApiResponse.success(examMasterService.listExamTypes()));
    }

    @GetMapping("/types/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_MASTER_VIEW')")
    @Operation(summary = "Get exam type")
    public ResponseEntity<ApiResponse<ExamTypeDTO>> getType(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(examMasterService.getExamType(id)));
    }

    @PostMapping("/types")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_MASTER_EDIT')")
    @Operation(summary = "Create or update exam type")
    public ResponseEntity<ApiResponse<ExamTypeDTO>> saveType(@Valid @RequestBody ExamTypeDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Exam type created" : "Exam type updated",
                examMasterService.saveExamType(dto)));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_MASTER_EDIT')")
    @Operation(summary = "Delete exam type")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable Long id) {
        examMasterService.deleteExamType(id);
        return ResponseEntity.ok(ApiResponse.success("Exam type deleted", null));
    }

    // ------ Grading Scale ------
    @GetMapping("/grading-scales")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_MASTER_VIEW')")
    @Operation(summary = "List grading scales")
    public ResponseEntity<ApiResponse<List<GradingScaleDTO>>> listScales() {
        return ResponseEntity.ok(ApiResponse.success(examMasterService.listGradingScales()));
    }

    @GetMapping("/grading-scales/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_MASTER_VIEW')")
    @Operation(summary = "Get grading scale with boundaries")
    public ResponseEntity<ApiResponse<GradingScaleDTO>> getScale(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(examMasterService.getGradingScale(id)));
    }

    @PostMapping("/grading-scales")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_MASTER_EDIT')")
    @Operation(summary = "Save grading scale (with boundaries)")
    public ResponseEntity<ApiResponse<GradingScaleDTO>> saveScale(@Valid @RequestBody GradingScaleDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Grading scale created" : "Grading scale updated",
                examMasterService.saveGradingScale(dto)));
    }

    @DeleteMapping("/grading-scales/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_MASTER_EDIT')")
    @Operation(summary = "Delete grading scale")
    public ResponseEntity<ApiResponse<Void>> deleteScale(@PathVariable Long id) {
        examMasterService.deleteGradingScale(id);
        return ResponseEntity.ok(ApiResponse.success("Grading scale deleted", null));
    }

    // ------ Report Card Template ------
    @GetMapping("/report-card-templates")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_MASTER_VIEW')")
    @Operation(summary = "List report card templates")
    public ResponseEntity<ApiResponse<List<ReportCardTemplateDTO>>> listTemplates() {
        return ResponseEntity.ok(ApiResponse.success(examMasterService.listReportCardTemplates()));
    }

    @GetMapping("/report-card-templates/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_MASTER_VIEW')")
    @Operation(summary = "Get report card template")
    public ResponseEntity<ApiResponse<ReportCardTemplateDTO>> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(examMasterService.getReportCardTemplate(id)));
    }

    @PostMapping("/report-card-templates")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_MASTER_EDIT')")
    @Operation(summary = "Save report card template")
    public ResponseEntity<ApiResponse<ReportCardTemplateDTO>> saveTemplate(@Valid @RequestBody ReportCardTemplateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Template created" : "Template updated",
                examMasterService.saveReportCardTemplate(dto)));
    }

    @DeleteMapping("/report-card-templates/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_MASTER_EDIT')")
    @Operation(summary = "Delete report card template")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        examMasterService.deleteReportCardTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted", null));
    }
}
