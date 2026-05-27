package com.thinkerscave.common.fee.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.fee.dto.FeeGroupDTO;
import com.thinkerscave.common.fee.dto.FeeHeadDTO;
import com.thinkerscave.common.fee.dto.FeePolicyDTO;
import com.thinkerscave.common.fee.service.FeeMasterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST APIs for fee master data — fee heads, groups, policies. Power the
 * "Fee Setup" workspace in the admin console.
 */
@RestController
@RequestMapping("/api/v1/fee/masters")
@Tag(name = "Fee Masters", description = "Fee heads, groups, and policy templates")
@RequiredArgsConstructor
@Slf4j
public class FeeMasterController {

    private final FeeMasterService feeMasterService;

    // -------------------------------------------------------- Fee Head ----

    @Operation(summary = "List fee heads")
    @GetMapping("/heads")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_MASTER_VIEW')")
    public ResponseEntity<ApiResponse<List<FeeHeadDTO>>> listHeads() {
        return ResponseEntity.ok(ApiResponse.success(feeMasterService.listFeeHeads()));
    }

    @Operation(summary = "Get a fee head by id")
    @GetMapping("/heads/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_MASTER_VIEW')")
    public ResponseEntity<ApiResponse<FeeHeadDTO>> getHead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feeMasterService.getFeeHead(id)));
    }

    @Operation(summary = "Create or update a fee head")
    @PostMapping("/heads")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_MASTER_EDIT')")
    public ResponseEntity<ApiResponse<FeeHeadDTO>> saveHead(@Valid @RequestBody FeeHeadDTO dto) {
        FeeHeadDTO saved = feeMasterService.saveFeeHead(dto);
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Fee head created" : "Fee head updated", saved));
    }

    @Operation(summary = "Delete a fee head")
    @DeleteMapping("/heads/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_MASTER_EDIT')")
    public ResponseEntity<ApiResponse<Void>> deleteHead(@PathVariable Long id) {
        feeMasterService.deleteFeeHead(id);
        return ResponseEntity.ok(ApiResponse.success("Fee head deleted", null));
    }

    // ------------------------------------------------------- Fee Group ----

    @Operation(summary = "List fee groups")
    @GetMapping("/groups")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_MASTER_VIEW')")
    public ResponseEntity<ApiResponse<List<FeeGroupDTO>>> listGroups() {
        return ResponseEntity.ok(ApiResponse.success(feeMasterService.listFeeGroups()));
    }

    @Operation(summary = "Create or update a fee group")
    @PostMapping("/groups")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_MASTER_EDIT')")
    public ResponseEntity<ApiResponse<FeeGroupDTO>> saveGroup(@Valid @RequestBody FeeGroupDTO dto) {
        FeeGroupDTO saved = feeMasterService.saveFeeGroup(dto);
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Fee group created" : "Fee group updated", saved));
    }

    @Operation(summary = "Delete a fee group")
    @DeleteMapping("/groups/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_MASTER_EDIT')")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        feeMasterService.deleteFeeGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Fee group deleted", null));
    }

    // ------------------------------------------------------ Fee Policy ----

    @Operation(summary = "List fee policies")
    @GetMapping("/policies")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_MASTER_VIEW')")
    public ResponseEntity<ApiResponse<List<FeePolicyDTO>>> listPolicies() {
        return ResponseEntity.ok(ApiResponse.success(feeMasterService.listFeePolicies()));
    }

    @Operation(summary = "Get a fee policy by id")
    @GetMapping("/policies/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('FEE_MASTER_VIEW')")
    public ResponseEntity<ApiResponse<FeePolicyDTO>> getPolicy(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feeMasterService.getFeePolicy(id)));
    }

    @Operation(summary = "Create or update a fee policy")
    @PostMapping("/policies")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_MASTER_EDIT')")
    public ResponseEntity<ApiResponse<FeePolicyDTO>> savePolicy(@Valid @RequestBody FeePolicyDTO dto) {
        FeePolicyDTO saved = feeMasterService.saveFeePolicy(dto);
        return ResponseEntity.ok(ApiResponse.success(
                dto.getId() == null ? "Fee policy created" : "Fee policy updated", saved));
    }

    @Operation(summary = "Delete a fee policy")
    @DeleteMapping("/policies/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('FEE_MASTER_EDIT')")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        feeMasterService.deleteFeePolicy(id);
        return ResponseEntity.ok(ApiResponse.success("Fee policy deleted", null));
    }
}
