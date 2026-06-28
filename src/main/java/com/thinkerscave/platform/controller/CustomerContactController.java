package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.CustomerContactRequest;
import com.thinkerscave.platform.dto.response.CustomerContactResponse;
import com.thinkerscave.platform.service.CustomerService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/customer-contacts")
@RequiredArgsConstructor
@Tag(name = "Customer Contact Management", description = "Manage customer contact records")
public class CustomerContactController {

    private final CustomerService customerService;

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update a customer contact")
    public ResponseEntity<ApiResponse<CustomerContactResponse>> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody CustomerContactRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Contact updated successfully", customerService.updateContact(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Delete a customer contact")
    public ResponseEntity<ApiResponse<Void>> deleteContact(@PathVariable Long id) {
        customerService.deleteContact(id);
        return ResponseEntity.ok(ApiResponse.noContent("Contact deleted successfully"));
    }
}
