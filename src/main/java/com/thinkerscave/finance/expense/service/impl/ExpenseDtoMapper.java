package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.*;
import com.thinkerscave.finance.expense.repository.*;
import com.thinkerscave.shared.document.entity.ManagedDocument;
import com.thinkerscave.shared.document.enums.*;
import com.thinkerscave.shared.document.repository.ManagedDocumentRepository;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.*;

@Component @RequiredArgsConstructor
class ExpenseDtoMapper {
 private final ExpensePaymentRepository payments;private final ExpenseApprovalEventRepository events;private final ManagedDocumentRepository documents;private final StaffRepository staffRepository;
 DetailResponse detail(Expense e){Staff s=staffRepository.findById(e.getRequesterStaffId()).orElseThrow(()->new ResourceNotFoundException("Staff not found: "+e.getRequesterStaffId()));return new DetailResponse(e.getExpenseId(),e.getExpenseNumber(),e.getExpenseDate(),e.getExpenseHead().getExpenseHeadId(),e.getHeadNameSnapshot(),e.getCategory().getExpenseCategoryId(),e.getCategoryCodeSnapshot(),e.getCategoryNameSnapshot(),e.getAmount(),e.getVendorName(),e.getVendorInvoiceNumber(),new StaffResponse(s.getStaffId(),s.getStaffCode(),name(s)),e.getRemarks(),e.getApprovalStatus(),e.getPaymentStatus(),e.getPaidAmount(),e.getRemainingAmount(),payments.findByExpense_ExpenseIdOrderByPaidOnAsc(e.getExpenseId()).stream().map(this::payment).toList(),events.findByExpense_ExpenseIdOrderByOccurredOnAsc(e.getExpenseId()).stream().map(this::event).toList(),documents.findByDocumentTypeAndOwnerTypeAndOwnerIdAndStatusOrderByCreatedOnAsc(DocumentType.EXPENSE_ATTACHMENT,"EXPENSE",e.getExpenseId(),ManagedDocumentStatus.ACTIVE).stream().map(this::attachment).toList());}
 PaymentResponse payment(ExpensePayment p){return new PaymentResponse(p.getExpensePaymentId(),p.getAmount(),p.getPaidOn(),p.getPaymentMethodId(),p.getPaymentMethodName(),p.getReferenceNumber(),p.getRemarks(),offset(p.getCreatedOn()));}
 AttachmentResponse attachment(ManagedDocument d){return new AttachmentResponse(d.getManagedDocumentId(),d.getFileName(),d.getContentType(),d.getPeriodKey(),offset(d.getCreatedOn()),d.getByteSize());}
 private ApprovalEventResponse event(ExpenseApprovalEvent e){return new ApprovalEventResponse(e.getExpenseApprovalEventId(),e.getEventType(),e.getFromStatus(),e.getToStatus(),e.getActor(),e.getRemarks(),e.getOccurredOn().atOffset(ZoneOffset.UTC));}
 private OffsetDateTime offset(LocalDateTime d){return d==null?null:d.atZone(ZoneId.systemDefault()).toOffsetDateTime();}
 private String name(Staff s){return (s.getFirstName()+" "+(s.getMiddleName()==null?"":s.getMiddleName())+" "+s.getLastName()).replaceAll("\\s+"," ").trim();}
}
