package com.thinkerscave.finance.expense.controller;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.enums.*;
import com.thinkerscave.finance.expense.numbering.ExpenseNumberService;
import com.thinkerscave.finance.expense.service.*;
import com.thinkerscave.shared.dto.*;
import com.thinkerscave.shared.util.PageRequestUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/v1/expenses") @RequiredArgsConstructor @PreAuthorize("isAuthenticated()")
public class ExpenseController {
 private final ExpenseOverviewService overview;private final ExpenseService expenses;private final ExpenseApprovalService approvals;
 private final ExpensePaymentService payments;private final ExpenseAttachmentService attachments;private final ExpenseHeadService heads;
 private final ExpenseCategoryService categories;private final ExpenseSettingsService settings;private final ExpenseNumberService numbers;
 @GetMapping("/overview") public ResponseEntity<ApiResponse<OverviewResponse>> overview(@RequestParam(required=false)String q,@RequestParam(required=false)LocalDate dateFrom,@RequestParam(required=false)LocalDate dateTo,@RequestParam(required=false)String datePreset,@RequestParam(required=false)Long categoryId,@RequestParam(required=false)Long expenseHeadId,@RequestParam(required=false)ExpenseApprovalStatus approvalStatus,@RequestParam(required=false)ExpensePaymentStatus paymentStatus){return ok("Expense overview",overview.overview(new Filter(q,dateFrom,dateTo,datePreset,categoryId,expenseHeadId,approvalStatus,paymentStatus)));}
 @GetMapping public ResponseEntity<ApiResponse<PageResponse<ListRow>>> list(@RequestParam(required=false)String q,@RequestParam(required=false)LocalDate dateFrom,@RequestParam(required=false)LocalDate dateTo,@RequestParam(required=false)String datePreset,@RequestParam(required=false)Long categoryId,@RequestParam(required=false)Long expenseHeadId,@RequestParam(required=false)ExpenseApprovalStatus approvalStatus,@RequestParam(required=false)ExpensePaymentStatus paymentStatus,@RequestParam(required=false)Integer page,@RequestParam(required=false)Integer size,@RequestParam(required=false)String sort){return ok("Expenses",overview.list(new Filter(q,dateFrom,dateTo,datePreset,categoryId,expenseHeadId,approvalStatus,paymentStatus),PageRequestUtil.of(page,size,sort==null?"expenseDate,desc":sort)));}
 @PostMapping public ResponseEntity<ApiResponse<DetailResponse>> create(@Valid @RequestBody CreateRequest r,@RequestHeader(value="Idempotency-Key",required=false)String key){return ResponseEntity.status(201).body(ApiResponse.created("Expense created",expenses.create(r,key)));}
 @GetMapping("/{id}") public ResponseEntity<ApiResponse<DetailResponse>> get(@PathVariable Long id){return ok("Expense",expenses.get(id));}
 @PutMapping("/{id}") public ResponseEntity<ApiResponse<DetailResponse>> update(@PathVariable Long id,@Valid @RequestBody UpdateRequest r){return ok("Expense updated",expenses.update(id,r));}
 @PostMapping("/{id}/submit") public ResponseEntity<ApiResponse<DetailResponse>> submit(@PathVariable Long id){return ok("Expense submitted",expenses.submit(id));}
 @PostMapping("/{id}/approve") public ResponseEntity<ApiResponse<DetailResponse>> approve(@PathVariable Long id){return ok("Expense approved",approvals.approve(id));}
 @PostMapping("/{id}/reject") public ResponseEntity<ApiResponse<DetailResponse>> reject(@PathVariable Long id,@Valid @RequestBody RejectRequest r){return ok("Expense rejected",approvals.reject(id,r));}
 @PostMapping("/{id}/return-to-draft") public ResponseEntity<ApiResponse<DetailResponse>> draft(@PathVariable Long id){return ok("Expense returned to draft",expenses.returnToDraft(id));}
 @PostMapping("/{id}/payments") public ResponseEntity<ApiResponse<PaymentResult>> pay(@PathVariable Long id,@Valid @RequestBody PaymentRequest r,@RequestHeader("Idempotency-Key")String key){return ResponseEntity.status(201).body(ApiResponse.created("Payment recorded",payments.record(id,r,key)));}
 @GetMapping("/{id}/payments") public ResponseEntity<ApiResponse<List<PaymentResponse>>> paymentList(@PathVariable Long id){return ok("Expense payments",payments.list(id));}
 @PostMapping(value="/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<ApiResponse<AttachmentResponse>> upload(@PathVariable Long id,@RequestPart MultipartFile file,@RequestParam(required=false)String kind){return ResponseEntity.status(201).body(ApiResponse.created("Attachment uploaded",attachments.upload(id,file,kind)));}
 @GetMapping("/{id}/attachments") public ResponseEntity<ApiResponse<List<AttachmentResponse>>> attachmentList(@PathVariable Long id){return ok("Expense attachments",attachments.list(id));}
 @GetMapping("/{id}/attachments/{documentId}/download") public ResponseEntity<byte[]> download(@PathVariable Long id,@PathVariable Long documentId){Download d=attachments.download(id,documentId);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+d.fileName()+"\"").contentType(MediaType.parseMediaType(d.contentType())).body(d.content());}
 @DeleteMapping("/{id}/attachments/{documentId}") public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable Long id,@PathVariable Long documentId){attachments.delete(id,documentId);return ResponseEntity.ok(ApiResponse.noContent("Attachment removed"));}
 @GetMapping("/heads") public ResponseEntity<ApiResponse<PageResponse<HeadResponse>>> headList(@RequestParam(required=false)String q,@RequestParam(required=false)Integer page,@RequestParam(required=false)Integer size,@RequestParam(required=false)String sort){return ok("Expense heads",heads.list(q,PageRequestUtil.of(page,size,sort)));}
 @GetMapping("/heads/lookups") public ResponseEntity<ApiResponse<List<HeadResponse>>> headLookups(){return ok("Expense head lookups",heads.lookups());}
 @GetMapping("/heads/{id}") public ResponseEntity<ApiResponse<HeadResponse>> head(@PathVariable Long id){return ok("Expense head",heads.get(id));}
 @PostMapping("/heads") public ResponseEntity<ApiResponse<HeadResponse>> createHead(@Valid @RequestBody HeadRequest r){return ResponseEntity.status(201).body(ApiResponse.created("Expense head created",heads.create(r)));}
 @PutMapping("/heads/{id}") public ResponseEntity<ApiResponse<HeadResponse>> updateHead(@PathVariable Long id,@Valid @RequestBody HeadRequest r){return ok("Expense head updated",heads.update(id,r));}
 @PatchMapping("/heads/{id}/status") public ResponseEntity<ApiResponse<HeadResponse>> headStatus(@PathVariable Long id,@Valid @RequestBody HeadStatusRequest r){return ok("Expense head status updated",heads.updateStatus(id,r));}
 @DeleteMapping("/heads/{id}") public ResponseEntity<ApiResponse<Void>> deleteHead(@PathVariable Long id){heads.delete(id);return ResponseEntity.ok(ApiResponse.noContent("Expense head deleted"));}
 @GetMapping("/categories") public ResponseEntity<ApiResponse<List<CategoryResponse>>> categoryList(){return ok("Expense categories",categories.list());}
 @GetMapping("/settings") public ResponseEntity<ApiResponse<SettingsResponse>> settings(){return ok("Expense settings",settings.get());}
 @PutMapping("/settings") public ResponseEntity<ApiResponse<SettingsResponse>> updateSettings(@Valid @RequestBody SettingsRequest r){return ok("Expense settings updated",settings.update(r));}
 @GetMapping("/settings/number-preview") public ResponseEntity<ApiResponse<String>> preview(@RequestParam(required=false)LocalDate expenseDate){return ok("Expense number preview",numbers.preview(expenseDate));}
 private static <T> ResponseEntity<ApiResponse<T>> ok(String m,T d){return ResponseEntity.ok(ApiResponse.success(m,d));}
}
