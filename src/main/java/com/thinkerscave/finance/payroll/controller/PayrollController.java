package com.thinkerscave.finance.payroll.controller;

import com.thinkerscave.finance.payroll.dto.request.EmployeeSalaryRequest;
import com.thinkerscave.finance.payroll.dto.request.PayrollGenerateRequest;
import com.thinkerscave.finance.payroll.dto.request.PayrollPaymentRequest;
import com.thinkerscave.finance.payroll.dto.request.PayrollSettingsRequest;
import com.thinkerscave.finance.payroll.dto.request.SalaryComponentRequest;
import com.thinkerscave.finance.payroll.dto.request.SalaryStructureRequest;
import com.thinkerscave.finance.payroll.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.payroll.dto.response.EmployeeSalaryResponse;
import com.thinkerscave.finance.payroll.dto.response.LegacyMigrationInventoryResponse;
import com.thinkerscave.finance.payroll.dto.response.MyPayrollSummaryResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollEmployeeRowResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollGenerateResult;
import com.thinkerscave.finance.payroll.dto.response.PayrollOverviewResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollPaymentResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollRunResponse;
import com.thinkerscave.finance.payroll.dto.response.PayrollSettingsResponse;
import com.thinkerscave.finance.payroll.dto.response.SalaryComponentResponse;
import com.thinkerscave.finance.payroll.dto.response.SalaryStructureResponse;
import com.thinkerscave.finance.payroll.dto.response.EmployeePayrollDetailResponse;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import com.thinkerscave.finance.payroll.migration.LegacyPayrollMigrationService;
import com.thinkerscave.finance.payroll.service.EmployeeSalaryService;
import com.thinkerscave.finance.payroll.service.PayrollApprovalService;
import com.thinkerscave.finance.payroll.service.PayrollGenerationService;
import com.thinkerscave.finance.payroll.service.PayrollMeService;
import com.thinkerscave.finance.payroll.service.PayrollOverviewService;
import com.thinkerscave.finance.payroll.service.PayrollPaymentService;
import com.thinkerscave.finance.payroll.service.PayrollSettingsService;
import com.thinkerscave.finance.payroll.service.PayslipService;
import com.thinkerscave.finance.payroll.service.SalaryComponentService;
import com.thinkerscave.finance.payroll.service.SalaryStructureService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import com.thinkerscave.shared.util.PageRequestUtil;
import com.thinkerscave.staff.enums.EmploymentCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("financePayrollController")
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PayrollController {

    private final PayrollOverviewService overviewService;
    private final SalaryComponentService componentService;
    private final SalaryStructureService structureService;
    private final EmployeeSalaryService employeeSalaryService;
    private final PayrollGenerationService generationService;
    private final PayrollApprovalService approvalService;
    private final PayrollPaymentService paymentService;
    private final PayslipService payslipService;
    private final PayrollSettingsService settingsService;
    private final PayrollMeService meService;
    private final LegacyPayrollMigrationService legacyMigrationService;

    // --- Overview ---
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PayrollOverviewResponse>> overview(
            @RequestParam Integer year, @RequestParam Integer month) {
        return ResponseEntity.ok(ApiResponse.success("Payroll overview", overviewService.overview(year, month)));
    }

    /** Optional recent-activity strip; empty until a shared audit feed is wired. */
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<Object>>> activities(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success("Payroll activities", List.of()));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<PageResponse<PayrollEmployeeRowResponse>>> employees(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) EmploymentCategory employmentCategory,
            @RequestParam(required = false) PaymentType paymentType,
            @RequestParam(required = false) EmployeePayrollStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Payroll employees",
                overviewService.employees(year, month, employmentCategory, paymentType, status, q,
                        PageRequestUtil.of(page, size, sort))));
    }

    // --- Components ---
    @GetMapping("/components")
    public ResponseEntity<ApiResponse<PageResponse<SalaryComponentResponse>>> listComponents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ComponentType type,
            @RequestParam(required = false) ComponentStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Salary components",
                componentService.list(q, type, status, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/components/lookups")
    public ResponseEntity<ApiResponse<List<SalaryComponentResponse>>> componentLookups() {
        return ResponseEntity.ok(ApiResponse.success("Salary component lookups", componentService.lookups()));
    }

    @GetMapping("/components/{id}")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> getComponent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Salary component", componentService.get(id)));
    }

    @PostMapping("/components")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> createComponent(
            @Valid @RequestBody SalaryComponentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created("Salary component created",
                componentService.create(request)));
    }

    @PutMapping("/components/{id}")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> updateComponent(
            @PathVariable Long id, @Valid @RequestBody SalaryComponentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Salary component updated", componentService.update(id, request)));
    }

    @PatchMapping("/components/{id}/status")
    public ResponseEntity<ApiResponse<SalaryComponentResponse>> componentStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Salary component status updated",
                componentService.updateStatus(id, request)));
    }

    @DeleteMapping("/components/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComponent(@PathVariable Long id) {
        componentService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Salary component deleted"));
    }

    // --- Structures ---
    @GetMapping("/structures")
    public ResponseEntity<ApiResponse<PageResponse<SalaryStructureResponse>>> listStructures(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ComponentStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Salary structures",
                structureService.list(q, status, PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/structures/lookups")
    public ResponseEntity<ApiResponse<List<SalaryStructureResponse>>> structureLookups() {
        return ResponseEntity.ok(ApiResponse.success("Salary structure lookups", structureService.lookups()));
    }

    @GetMapping("/structures/{id}")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> getStructure(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure", structureService.get(id)));
    }

    @PostMapping("/structures")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> createStructure(
            @Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created("Salary structure created",
                structureService.create(request)));
    }

    @PutMapping("/structures/{id}")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> updateStructure(
            @PathVariable Long id, @Valid @RequestBody SalaryStructureRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure updated", structureService.update(id, request)));
    }

    @PatchMapping("/structures/{id}/status")
    public ResponseEntity<ApiResponse<SalaryStructureResponse>> structureStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Salary structure status updated",
                structureService.updateStatus(id, request)));
    }

    @DeleteMapping("/structures/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStructure(@PathVariable Long id) {
        structureService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Salary structure deleted"));
    }

    // --- Employee salary ---
    @GetMapping("/employees/{staffId}/salary")
    public ResponseEntity<ApiResponse<EmployeeSalaryResponse>> getSalary(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Employee salary", employeeSalaryService.getCurrent(staffId)));
    }

    @GetMapping("/employees/{staffId}/salary/history")
    public ResponseEntity<ApiResponse<List<EmployeeSalaryResponse>>> salaryHistory(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success("Employee salary history", employeeSalaryService.history(staffId)));
    }

    @PostMapping("/employees/{staffId}/salary")
    public ResponseEntity<ApiResponse<EmployeeSalaryResponse>> assignSalary(
            @PathVariable Long staffId, @Valid @RequestBody EmployeeSalaryRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created("Employee salary assigned",
                employeeSalaryService.assign(staffId, request)));
    }

    @GetMapping("/employees/{staffId}/payroll-history")
    public ResponseEntity<ApiResponse<PageResponse<EmployeePayrollDetailResponse>>> payrollHistory(
            @PathVariable Long staffId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("Employee payroll history",
                overviewService.payrollHistory(staffId, PageRequestUtil.of(page, size, sort))));
    }

    // --- Runs ---
    @PostMapping("/runs/generate")
    public ResponseEntity<ApiResponse<PayrollGenerateResult>> generate(
            @Valid @RequestBody PayrollGenerateRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.status(201).body(ApiResponse.created("Payroll generated",
                generationService.generate(request, idempotencyKey)));
    }

    @PostMapping("/runs/{runId}/recalculate")
    public ResponseEntity<ApiResponse<PayrollGenerateResult>> recalculate(
            @PathVariable Long runId,
            @RequestBody(required = false) PayrollGenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll recalculated",
                generationService.recalculate(runId, request)));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getRun(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.success("Payroll run", generationService.getRun(runId)));
    }

    @PostMapping("/runs/{runId}/approve")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> approve(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.success("Payroll approved", approvalService.approve(runId)));
    }

    @PostMapping("/runs/{runId}/return")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> returnRun(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.success("Payroll returned", approvalService.returnForCorrection(runId)));
    }

    // --- Payments / payslip ---
    @PostMapping("/employee-payrolls/{id}/payments")
    public ResponseEntity<ApiResponse<PayrollPaymentResponse>> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody PayrollPaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.status(201).body(ApiResponse.created("Payment recorded",
                paymentService.recordPayment(id, request, idempotencyKey)));
    }

    @GetMapping("/employee-payrolls/{id}/payslip")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Long id) {
        byte[] pdf = payslipService.loadStoredPayslip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // --- Settings ---
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<PayrollSettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success("Payroll settings", settingsService.get()));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<PayrollSettingsResponse>> updateSettings(
            @Valid @RequestBody PayrollSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll settings updated", settingsService.update(request)));
    }

    // --- Me ---
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<MyPayrollSummaryResponse>> mySummary() {
        return ResponseEntity.ok(ApiResponse.success("My payroll summary", meService.summary()));
    }

    @GetMapping("/me/history")
    public ResponseEntity<ApiResponse<PageResponse<PayrollEmployeeRowResponse>>> myHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success("My payroll history",
                meService.history(PageRequestUtil.of(page, size, sort))));
    }

    @GetMapping("/me/employee-payrolls/{id}")
    public ResponseEntity<ApiResponse<EmployeePayrollDetailResponse>> myDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("My employee payroll", meService.detail(id)));
    }

    @GetMapping("/me/employee-payrolls/{id}/payslip")
    public ResponseEntity<byte[]> myPayslip(@PathVariable Long id) {
        byte[] pdf = meService.payslip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // --- Admin migrate (optional) ---
    @GetMapping("/admin/legacy-migration/inventory")
    public ResponseEntity<ApiResponse<LegacyMigrationInventoryResponse>> migrationInventory() {
        return ResponseEntity.ok(ApiResponse.success("Legacy payroll inventory", legacyMigrationService.inventory()));
    }

    @PostMapping("/admin/legacy-migration/migrate")
    public ResponseEntity<ApiResponse<LegacyMigrationInventoryResponse>> migrateLegacy() {
        return ResponseEntity.ok(ApiResponse.success("Legacy payroll migrated", legacyMigrationService.migrate()));
    }
}
