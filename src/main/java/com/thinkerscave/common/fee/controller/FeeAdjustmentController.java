package com.thinkerscave.common.fee.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.fee.domain.ReminderChannel;
import com.thinkerscave.common.fee.domain.RefundStatus;
import com.thinkerscave.common.fee.dto.FeeAdjustmentDTO;
import com.thinkerscave.common.fee.dto.FeeRefundDTO;
import com.thinkerscave.common.fee.dto.FeeReminderDTO;
import com.thinkerscave.common.fee.service.FeeAdjustmentService;
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
@RequestMapping("/api/v1/fee/adjustments")
@Tag(name = "Fee Adjustments / Refunds / Reminders",
     description = "Discounts, scholarships, waivers, refund workflow & reminders")
@RequiredArgsConstructor
@Slf4j
public class FeeAdjustmentController {

    private final FeeAdjustmentService feeAdjustmentService;

    // ----------------------------------------------------- Adjustments ----

    @Operation(summary = "List adjustments for a student")
    @GetMapping("/by-student/{studentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_ADJUSTMENT_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FeeAdjustmentDTO>>> listAdjustments(
            @PathVariable Long studentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                feeAdjustmentService.listAdjustments(studentId, PageRequestUtil.of(page, size, sort)))));
    }

    @Operation(summary = "Create a fee adjustment (discount/scholarship/waiver/penalty/etc)")
    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_ADJUSTMENT_EDIT')")
    public ResponseEntity<ApiResponse<FeeAdjustmentDTO>> createAdjustment(@Valid @RequestBody FeeAdjustmentDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Adjustment created",
                feeAdjustmentService.createAdjustment(dto)));
    }

    // ---------------------------------------------------------- Refunds ---

    @Operation(summary = "List refunds by status")
    @GetMapping("/refunds")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_REFUND_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<FeeRefundDTO>>> listRefunds(
            @RequestParam(defaultValue = "REQUESTED") RefundStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                feeAdjustmentService.listRefunds(status, PageRequestUtil.of(page, size, sort)))));
    }

    @Operation(summary = "Request a refund")
    @PostMapping("/refunds")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_REFUND_REQUEST')")
    public ResponseEntity<ApiResponse<FeeRefundDTO>> requestRefund(@Valid @RequestBody FeeRefundDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Refund requested",
                feeAdjustmentService.requestRefund(dto)));
    }

    @Operation(summary = "Approve a pending refund")
    @PostMapping("/refunds/{id}/approve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_REFUND_APPROVE')")
    public ResponseEntity<ApiResponse<FeeRefundDTO>> approveRefund(@PathVariable Long id,
                                                                   @RequestParam(required = false) Long approvedByUserId) {
        return ResponseEntity.ok(ApiResponse.success("Refund approved",
                feeAdjustmentService.approveRefund(id, approvedByUserId)));
    }

    @Operation(summary = "Mark an approved refund as processed (paid out)")
    @PostMapping("/refunds/{id}/process")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_REFUND_PROCESS')")
    public ResponseEntity<ApiResponse<FeeRefundDTO>> processRefund(@PathVariable Long id,
                                                                   @RequestParam(required = false) String payoutReference) {
        return ResponseEntity.ok(ApiResponse.success("Refund processed",
                feeAdjustmentService.processRefund(id, payoutReference)));
    }

    @Operation(summary = "Reject a refund request")
    @PostMapping("/refunds/{id}/reject")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_REFUND_APPROVE')")
    public ResponseEntity<ApiResponse<FeeRefundDTO>> rejectRefund(@PathVariable Long id,
                                                                  @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success("Refund rejected",
                feeAdjustmentService.rejectRefund(id, reason)));
    }

    // -------------------------------------------------------- Reminders ---

    @Operation(summary = "List reminders dispatched for an invoice")
    @GetMapping("/reminders/by-invoice/{invoiceId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_INVOICE_VIEW')")
    public ResponseEntity<ApiResponse<List<FeeReminderDTO>>> listReminders(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(feeAdjustmentService.listReminders(invoiceId)));
    }

    @Operation(summary = "Record a reminder dispatch (called by the reminder job)")
    @PostMapping("/reminders/dispatched")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','SYSTEM') or hasAuthority('FEE_REMINDER_DISPATCH')")
    public ResponseEntity<ApiResponse<FeeReminderDTO>> recordDispatch(
            @RequestParam Long invoiceId,
            @RequestParam ReminderChannel channel,
            @RequestParam String recipient,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "true") boolean success,
            @RequestParam(required = false) String failureReason) {
        return ResponseEntity.ok(ApiResponse.created("Reminder recorded",
                feeAdjustmentService.recordReminderDispatch(invoiceId, channel, recipient,
                        subject, success, failureReason)));
    }
}
