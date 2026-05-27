package com.thinkerscave.common.workflow.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.workflow.dto.WorkflowConfigDTO;
import com.thinkerscave.common.workflow.service.WorkflowConfigService;
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
@RequestMapping("/api/v1/workflow-configs")
@Tag(name = "Workflow Configuration", description = "Per-organization workflow / approval configuration")
@RequiredArgsConstructor
@Slf4j
public class WorkflowConfigController {

    private final WorkflowConfigService workflowConfigService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('WORKFLOW_CONFIG_VIEW')")
    public ResponseEntity<ApiResponse<List<WorkflowConfigDTO>>> list() {
        return ResponseEntity.ok(ApiResponse.success(workflowConfigService.list()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('WORKFLOW_CONFIG_VIEW')")
    public ResponseEntity<ApiResponse<WorkflowConfigDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(workflowConfigService.get(id)));
    }

    @GetMapping("/by-key/{workflowKey}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lookup a workflow configuration by its key")
    public ResponseEntity<ApiResponse<WorkflowConfigDTO>> byKey(@PathVariable String workflowKey) {
        return workflowConfigService.findByKey(workflowKey)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Not configured", null)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('WORKFLOW_CONFIG_EDIT')")
    public ResponseEntity<ApiResponse<WorkflowConfigDTO>> save(
            @Valid @RequestBody WorkflowConfigDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Workflow config created" : "Workflow config updated",
                workflowConfigService.save(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN') or hasAuthority('WORKFLOW_CONFIG_EDIT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        workflowConfigService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Workflow config deleted", null));
    }
}
