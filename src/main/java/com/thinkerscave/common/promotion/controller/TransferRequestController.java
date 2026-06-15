package com.thinkerscave.common.promotion.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.promotion.domain.TransferStatus;
import com.thinkerscave.common.promotion.dto.TransferRequestDTO;
import com.thinkerscave.common.promotion.service.TransferRequestService;
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
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Transfer / school-leaving certificate workflow")
@RequiredArgsConstructor
@Slf4j
public class TransferRequestController {

    private final TransferRequestService transferService;

//    @GetMapping
//    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('TRANSFER_VIEW')")
//    public ResponseEntity<ApiResponse<PageResponse<TransferRequestDTO>>> list(
//            @RequestParam(required = false) Integer page,
//            @RequestParam(required = false) Integer size,
//            @RequestParam(required = false) String sort) {
//        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
//                transferService.list(PageRequestUtil.of(page, size, sort)))));
//    }
//
//    @GetMapping("/student/{studentId}")
//    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('TRANSFER_VIEW')")
//    public ResponseEntity<ApiResponse<List<TransferRequestDTO>>> forStudent(@PathVariable Long studentId) {
//        return ResponseEntity.ok(ApiResponse.success(transferService.listForStudent(studentId)));
//    }
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('TRANSFER_VIEW')")
//    public ResponseEntity<ApiResponse<TransferRequestDTO>> get(@PathVariable Long id) {
//        return ResponseEntity.ok(ApiResponse.success(transferService.get(id)));
//    }
//
//    @PostMapping
//    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('TRANSFER_EDIT')")
//    public ResponseEntity<ApiResponse<TransferRequestDTO>> create(
//            @Valid @RequestBody TransferRequestDTO dto) {
//        return ResponseEntity.ok(ApiResponse.created("Transfer request created",
//                transferService.create(dto)));
//    }
//
//    @PatchMapping("/{id}/status")
//    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('TRANSFER_APPROVE')")
//    @Operation(summary = "Transition transfer request status")
//    public ResponseEntity<ApiResponse<TransferRequestDTO>> transition(
//            @PathVariable Long id,
//            @RequestParam TransferStatus target,
//            @RequestParam(required = false) Long actorUserId,
//            @RequestParam(required = false) String remarks) {
//        return ResponseEntity.ok(ApiResponse.success("Status updated",
//                transferService.transition(id, target, actorUserId, remarks)));
//    }
}
