package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.CustomerContactRequest;
import com.thinkerscave.platform.dto.request.CustomerRequest;
import com.thinkerscave.platform.dto.request.CustomerStatusUpdateRequest;
import com.thinkerscave.platform.dto.response.CustomerContactResponse;
import com.thinkerscave.platform.dto.response.CustomerDetailResponse;
import com.thinkerscave.platform.dto.response.CustomerResponse;
import com.thinkerscave.platform.dto.response.OrganizationSummaryResponse;
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.CustomerType;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.service.CustomerService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/platform/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Manage platform customers")
public class CustomerController {

    private final CustomerService customerService;
    private final OrganizationRepository organizationRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List customers with filters and pagination")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getCustomers(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) CustomerType customerType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved",
                customerService.getCustomers(status, customerType, search, activeOnly, pageable)));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Customer module dashboard statistics")
    public ResponseEntity<ApiResponse<com.thinkerscave.platform.dto.response.CustomerDashboardResponse>> getCustomerDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Customer dashboard retrieved", customerService.getCustomerDashboard()));
    }

    @GetMapping("/metadata")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Customer module metadata (statuses, types)")
    public ResponseEntity<ApiResponse<com.thinkerscave.platform.dto.response.CustomerMetadataResponse>> getCustomerMetadata() {
        return ResponseEntity.ok(ApiResponse.success("Customer metadata retrieved", customerService.getCustomerMetadata()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get customer detail")
    public ResponseEntity<ApiResponse<CustomerDetailResponse>> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer detail retrieved", customerService.getCustomerById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Create a new customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Customer created successfully", customerService.createCustomer(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", customerService.updateCustomer(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Archive customer")
    public ResponseEntity<ApiResponse<Void>> archiveCustomer(@PathVariable Long id) {
        customerService.archiveCustomer(id);
        return ResponseEntity.ok(ApiResponse.noContent("Customer archived successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Update customer status")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomerStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer status updated",
                customerService.updateCustomerStatus(id, request.getStatus())));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Restore archived customer")
    public ResponseEntity<ApiResponse<Void>> restoreCustomer(@PathVariable Long id) {
        customerService.restoreCustomer(id);
        return ResponseEntity.ok(ApiResponse.noContent("Customer restored successfully"));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Permanently delete archived customer")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteCustomer(@PathVariable Long id) {
        customerService.permanentlyDeleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.noContent("Customer permanently deleted"));
    }

    @GetMapping("/{id}/organizations")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get organizations belonging to a customer")
    public ResponseEntity<ApiResponse<List<OrganizationSummaryResponse>>> getCustomerOrganizations(@PathVariable Long id) {
        List<OrganizationSummaryResponse> orgs = organizationRepository.findByCustomer_IdAndActiveTrue(id)
                .stream().map(o -> OrganizationSummaryResponse.builder()
                        .id(o.getId())
                        .organizationCode(o.getOrganizationCode())
                        .organizationName(o.getOrganizationName())
                        .shortName(o.getShortName())
                        .institutionType(o.getInstitutionType())
                        .status(o.getStatus())
                        .email(o.getEmail())
                        .mobileNumber(o.getMobileNumber())
                        .city(o.getCity())
                        .state(o.getState())
                        .country(o.getCountry())
                        .logoUrl(o.getLogoUrl())
                        .onboardingCompleted(o.getOnboardingCompleted())
                        .active(o.getActive())
                        .tenantIdentifier(o.getTenantRegistry() != null ? o.getTenantRegistry().getTenantIdentifier() : null)
                        .createdOn(o.getCreatedOn())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Customer organizations retrieved", orgs));
    }

    // ── Customer Contacts ─────────────────────────────────────────────────────

    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List contacts for a customer")
    public ResponseEntity<ApiResponse<List<CustomerContactResponse>>> getContacts(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Contacts retrieved", customerService.getContacts(id)));
    }

    @PostMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Add a contact to a customer")
    public ResponseEntity<ApiResponse<CustomerContactResponse>> addContact(
            @PathVariable Long id,
            @Valid @RequestBody CustomerContactRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Contact added successfully", customerService.addContact(id, request)));
    }
}
