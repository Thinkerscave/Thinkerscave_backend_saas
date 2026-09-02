package com.thinkerscave.platform.controller;

import com.thinkerscave.retention.LoginHistoryRetentionTask;
import com.thinkerscave.retention.RetentionTrigger;
import com.thinkerscave.retention.dto.RetentionPurgeResult;
import com.thinkerscave.retention.dto.RetentionTaskStatus;
import com.thinkerscave.retention.service.RetentionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/platform/retention")
@RequiredArgsConstructor
@Tag(name = "Data retention", description = "Scheduled and manual deletion of short-lived operational data")
public class RetentionController {

    private final RetentionService retentionService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List retention tasks and their last cleanup runs")
    public ResponseEntity<ApiResponse<List<RetentionTaskStatus>>> list() {
        return ResponseEntity.ok(ApiResponse.success(retentionService.listTasks()));
    }

    @GetMapping("/{taskKey}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Retention status for one dataset")
    public ResponseEntity<ApiResponse<RetentionTaskStatus>> get(@PathVariable String taskKey) {
        return ResponseEntity.ok(ApiResponse.success(retentionService.getTask(taskKey)));
    }

    @PostMapping("/{taskKey}/run")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete rows older than the retention window now")
    public ResponseEntity<ApiResponse<RetentionPurgeResult>> run(@PathVariable String taskKey) {
        String task = taskKey == null || taskKey.isBlank() ? LoginHistoryRetentionTask.KEY : taskKey;
        RetentionPurgeResult result = retentionService.run(task, RetentionTrigger.MANUAL, null, currentUsername());
        return ResponseEntity.ok(ApiResponse.success(result.getSummary(), result));
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "super-admin";
    }
}
