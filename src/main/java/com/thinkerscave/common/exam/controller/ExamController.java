package com.thinkerscave.common.exam.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.exam.domain.ExamStatus;
import com.thinkerscave.common.exam.dto.ExamDTO;
import com.thinkerscave.common.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exams", description = "Exam definitions (with subjects & schedules)")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamService examService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_VIEW')")
    @Operation(summary = "List exams for an academic year")
    public ResponseEntity<ApiResponse<PageResponse<ExamDTO>>> list(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                examService.listByYear(academicYearId, PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('EXAM_VIEW')")
    @Operation(summary = "Get exam with subjects & schedules")
    public ResponseEntity<ApiResponse<ExamDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(examService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_EDIT')")
    @Operation(summary = "Create or update exam (with subjects & schedules)")
    public ResponseEntity<ApiResponse<ExamDTO>> save(@Valid @RequestBody ExamDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Exam created" : "Exam updated", examService.save(dto)));
    }

    @PostMapping("/{id}/status/{target}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_EDIT')")
    @Operation(summary = "Transition exam to a new lifecycle status")
    public ResponseEntity<ApiResponse<ExamDTO>> transition(@PathVariable Long id, @PathVariable ExamStatus target) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                examService.transitionStatus(id, target)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('EXAM_EDIT')")
    @Operation(summary = "Delete a PLANNED or CANCELLED exam")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        examService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Exam deleted", null));
    }
}
