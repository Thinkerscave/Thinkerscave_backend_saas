package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.MaintenanceScheduleRequest;
import com.thinkerscave.platform.dto.response.MaintenanceScheduleResponse;
import com.thinkerscave.platform.service.MaintenanceService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance Schedule", description = "Manage platform and organization maintenance windows")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List all maintenance schedules")
    public ResponseEntity<ApiResponse<List<MaintenanceScheduleResponse>>> getAllMaintenanceSchedules() {
        return ResponseEntity.ok(ApiResponse.success("Maintenance schedules retrieved",
                maintenanceService.getAllMaintenanceSchedules()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create a maintenance schedule")
    public ResponseEntity<ApiResponse<MaintenanceScheduleResponse>> createMaintenanceSchedule(
            @Valid @RequestBody MaintenanceScheduleRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Maintenance schedule created",
                        maintenanceService.createMaintenanceSchedule(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a maintenance schedule")
    public ResponseEntity<ApiResponse<MaintenanceScheduleResponse>> updateMaintenanceSchedule(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Maintenance schedule updated",
                maintenanceService.updateMaintenanceSchedule(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete / archive maintenance schedule")
    public ResponseEntity<ApiResponse<Void>> deleteMaintenanceSchedule(@PathVariable Long id) {
        maintenanceService.deleteMaintenanceSchedule(id);
        return ResponseEntity.ok(ApiResponse.noContent("Maintenance schedule deleted"));
    }
}
