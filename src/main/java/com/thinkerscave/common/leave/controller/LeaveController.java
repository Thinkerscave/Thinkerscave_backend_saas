package com.thinkerscave.common.leave.controller;

import com.thinkerscave.common.leave.dto.LeaveRequestDTO;
import com.thinkerscave.common.leave.dto.LeaveResponseDTO;
import com.thinkerscave.common.leave.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leave")
@Tag(name = "Leave Management", description = "APIs for staff leave request management")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @Operation(summary = "Apply for leave")
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TEACHER','STAFF')")
    public ResponseEntity<LeaveResponseDTO> applyLeave(
            @Valid @RequestBody LeaveRequestDTO dto,
            Authentication auth) {
        String appliedBy = auth != null ? auth.getName() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(dto, appliedBy));
    }

    @Operation(summary = "Get all leave requests (Admin/Manager)")
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<List<LeaveResponseDTO>> getAllLeaveRequests() {
        return ResponseEntity.ok(leaveService.getAllLeaveRequests());
    }

    @Operation(summary = "Get my own leave requests")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveResponseDTO>> getMyLeaveRequests(Authentication auth) {
        return ResponseEntity.ok(leaveService.getMyLeaveRequests(auth.getName()));
    }

    @Operation(summary = "Approve a leave request")
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LeaveResponseDTO> approveLeave(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(leaveService.approveLeave(id, auth.getName()));
    }

    @Operation(summary = "Reject a leave request")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<LeaveResponseDTO> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return ResponseEntity.ok(leaveService.rejectLeave(id, reason, auth.getName()));
    }

    @Operation(summary = "Cancel my leave request (only PENDING)")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelLeave(@PathVariable Long id, Authentication auth) {
        leaveService.cancelLeave(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
