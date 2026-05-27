package com.thinkerscave.common.exam.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.exam.dto.ResultDTO;
import com.thinkerscave.common.exam.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam/results")
@Tag(name = "Exam Results", description = "Aggregate results, grades, ranks")
@RequiredArgsConstructor
@Slf4j
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/by-exam/{examId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF','TEACHER') or hasAuthority('RESULT_VIEW')")
    @Operation(summary = "List results for an exam (paged)")
    public ResponseEntity<ApiResponse<PageResponse<ResultDTO>>> byExam(
            @PathVariable Long examId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                resultService.listByExam(examId, PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/by-student/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF','TEACHER','STUDENT','PARENT') or hasAuthority('RESULT_VIEW')")
    @Operation(summary = "List all results for a student")
    public ResponseEntity<ApiResponse<List<ResultDTO>>> byStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(resultService.listByStudent(studentId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF','TEACHER','STUDENT','PARENT') or hasAuthority('RESULT_VIEW')")
    @Operation(summary = "Get a specific student's result for an exam")
    public ResponseEntity<ApiResponse<ResultDTO>> get(@RequestParam Long examId, @RequestParam Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(resultService.getForStudent(examId, studentId)));
    }

    @PostMapping("/{examId}/compute")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESULT_COMPUTE')")
    @Operation(summary = "Recompute results & ranks for an entire exam")
    public ResponseEntity<ApiResponse<Integer>> compute(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success("Results computed",
                resultService.computeAll(examId)));
    }

    @PostMapping("/{examId}/declare")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('RESULT_DECLARE')")
    @Operation(summary = "Mark exam as RESULT_DECLARED")
    public ResponseEntity<ApiResponse<ResultDTO>> declare(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success("Results declared",
                resultService.declare(examId)));
    }
}
