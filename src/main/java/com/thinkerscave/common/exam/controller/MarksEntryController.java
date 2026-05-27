package com.thinkerscave.common.exam.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.exam.domain.MarksStatus;
import com.thinkerscave.common.exam.dto.MarksEntryDTO;
import com.thinkerscave.common.exam.service.MarksEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exam/marks")
@Tag(name = "Marks Entry", description = "Per-subject marks entry workflow (enter → submit → approve → lock)")
@RequiredArgsConstructor
@Slf4j
public class MarksEntryController {

    private final MarksEntryService marksEntryService;

    @GetMapping("/by-subject")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF','TEACHER') or hasAuthority('MARKS_VIEW')")
    @Operation(summary = "List marks rows for an exam-subject")
    public ResponseEntity<ApiResponse<List<MarksEntryDTO>>> bySubject(
            @RequestParam Long examId, @RequestParam Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success(marksEntryService.listForSubject(examId, subjectId)));
    }

    @GetMapping("/by-student")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF','TEACHER','STUDENT','PARENT') or hasAuthority('MARKS_VIEW')")
    @Operation(summary = "List a student's marks for an exam")
    public ResponseEntity<ApiResponse<List<MarksEntryDTO>>> byStudent(
            @RequestParam Long examId, @RequestParam Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(marksEntryService.listForStudent(examId, studentId)));
    }

    @PostMapping("/upsert")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','TEACHER') or hasAuthority('MARKS_EDIT')")
    @Operation(summary = "Bulk upsert marks rows for an exam-subject")
    public ResponseEntity<ApiResponse<List<MarksEntryDTO>>> upsert(
            @RequestParam Long examId, @RequestParam Long subjectId,
            @Valid @RequestBody List<MarksEntryDTO> entries) {
        return ResponseEntity.ok(ApiResponse.success("Marks saved",
                marksEntryService.upsertBatch(examId, subjectId, entries)));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','TEACHER') or hasAuthority('MARKS_SUBMIT')")
    @Operation(summary = "Submit marks for approval")
    public ResponseEntity<ApiResponse<Void>> submit(@RequestParam Long examId, @RequestParam Long subjectId) {
        marksEntryService.submit(examId, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Marks submitted", null));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('MARKS_APPROVE')")
    @Operation(summary = "Approve submitted marks")
    public ResponseEntity<ApiResponse<Void>> approve(@RequestParam Long examId, @RequestParam Long subjectId,
                                                     @RequestParam(required = false) Long approvedByUserId) {
        marksEntryService.approve(examId, subjectId, approvedByUserId);
        return ResponseEntity.ok(ApiResponse.success("Marks approved", null));
    }

    @PostMapping("/lock")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('MARKS_LOCK')")
    @Operation(summary = "Lock approved marks (no further edits)")
    public ResponseEntity<ApiResponse<Void>> lock(@RequestParam Long examId, @RequestParam Long subjectId) {
        marksEntryService.lock(examId, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Marks locked", null));
    }

    @PostMapping("/reopen")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('MARKS_LOCK')")
    @Operation(summary = "Reopen locked marks for correction")
    public ResponseEntity<ApiResponse<Void>> reopen(@RequestParam Long examId, @RequestParam Long subjectId) {
        marksEntryService.reopen(examId, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Marks reopened", null));
    }

    @GetMapping("/summary/{examId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('MARKS_VIEW')")
    @Operation(summary = "Counts of marks rows by status for an exam")
    public ResponseEntity<ApiResponse<Map<MarksStatus, Long>>> summary(@PathVariable Long examId) {
        return ResponseEntity.ok(ApiResponse.success(marksEntryService.statusSummary(examId)));
    }
}
