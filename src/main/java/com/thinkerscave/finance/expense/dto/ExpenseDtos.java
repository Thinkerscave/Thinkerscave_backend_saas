package com.thinkerscave.finance.expense.dto;

import com.thinkerscave.finance.expense.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class ExpenseDtos {
 private ExpenseDtos(){}
 public record Filter(String q,LocalDate dateFrom,LocalDate dateTo,String datePreset,Long categoryId,Long expenseHeadId,
  ExpenseApprovalStatus approvalStatus,ExpensePaymentStatus paymentStatus){}
 public record PaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal amount,@NotNull LocalDate paidOn,Long paymentMethodId,
  String referenceNumber,String remarks){}
 public record CreateRequest(@NotNull Long expenseHeadId,@NotNull LocalDate expenseDate,@NotNull @DecimalMin("0.01") BigDecimal amount,
  String vendorName,String vendorInvoiceNumber,@NotNull Long requesterStaffId,String remarks,boolean saveAsDraft,PaymentRequest initialPayment){}
 public record UpdateRequest(Long expenseHeadId,LocalDate expenseDate,@DecimalMin("0.01") BigDecimal amount,String vendorName,
  String vendorInvoiceNumber,Long requesterStaffId,String remarks){}
 public record RejectRequest(@NotBlank @Size(max=1000) String remarks){}
 public record HeadRequest(@NotBlank @Size(max=150) String name,@NotNull Long expenseCategoryId,Long defaultRequesterStaffId,
  @Size(max=500) String description,@NotNull ExpenseHeadStatus status){}
 public record HeadStatusRequest(@NotNull ExpenseHeadStatus status){}
 public record SettingsRequest(@NotNull Boolean approvalRequired,@NotBlank String numberPrefix,@NotBlank String numberYearFormat,
  @Min(1) @Max(10) int numberPadWidth,@Min(1) long numberStart,@NotNull Boolean numberSequencePerYear,
  Long defaultPaymentMethodId,@Min(1) long maxAttachmentBytes,@NotBlank String allowedAttachmentContentTypes){}
 public record CategoryResponse(Long expenseCategoryId,String code,String name,String description,Integer sortOrder){}
 public record HeadResponse(Long expenseHeadId,String name,CategoryResponse category,Long defaultRequesterStaffId,
  String defaultRequesterName,String description,ExpenseHeadStatus status){}
 public record StaffResponse(Long staffId,String staffCode,String name){}
 public record ListRow(Long expenseId,LocalDate date,String expenseNumber,String headName,String categoryName,String vendorName,
  BigDecimal amount,ExpenseApprovalStatus approvalStatus,ExpensePaymentStatus paymentStatus,BigDecimal paidAmount,BigDecimal remainingAmount){}
 public record PaymentResponse(Long expensePaymentId,BigDecimal amount,LocalDate paidOn,Long paymentMethodId,String paymentMethodName,
  String referenceNumber,String remarks,OffsetDateTime createdOn){}
 public record PaymentResult(PaymentResponse payment,ExpensePaymentStatus paymentStatus,BigDecimal paidAmount,BigDecimal remainingAmount){}
 public record ApprovalEventResponse(Long id,ExpenseApprovalEventType eventType,ExpenseApprovalStatus fromStatus,
  ExpenseApprovalStatus toStatus,String actor,String remarks,OffsetDateTime occurredOn){}
 public record AttachmentResponse(Long managedDocumentId,String fileName,String contentType,String kind,OffsetDateTime uploadedOn,Long byteSize){}
 public record DetailResponse(Long expenseId,String expenseNumber,LocalDate expenseDate,Long expenseHeadId,String headName,
  Long expenseCategoryId,String categoryCode,String categoryName,BigDecimal amount,String vendorName,String vendorInvoiceNumber,
  StaffResponse requester,String remarks,ExpenseApprovalStatus approvalStatus,ExpensePaymentStatus paymentStatus,
  BigDecimal paidAmount,BigDecimal remainingAmount,List<PaymentResponse> payments,List<ApprovalEventResponse> approvalEvents,
  List<AttachmentResponse> attachments){}
 public record OverviewResponse(BigDecimal totalExpenseAmount,BigDecimal pendingApprovalAmount,long pendingApprovalCount,
  BigDecimal pendingPaymentAmount,long pendingPaymentCount,BigDecimal paidAmount,long paidCount,String periodLabel){}
 public record SettingsResponse(Boolean approvalRequired,String numberPrefix,String numberYearFormat,Integer numberPadWidth,Long numberStart,
  Boolean numberSequencePerYear,Long defaultPaymentMethodId,Long maxAttachmentBytes,String allowedAttachmentContentTypes){}
 public record Download(byte[] content,String fileName,String contentType){}
}
