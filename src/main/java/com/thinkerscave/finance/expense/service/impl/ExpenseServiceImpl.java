package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.*;
import com.thinkerscave.finance.expense.enums.*;
import com.thinkerscave.finance.expense.numbering.ExpenseNumberService;
import com.thinkerscave.finance.expense.repository.*;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.*;
import com.thinkerscave.shared.exceptions.*;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpenseServiceImpl implements ExpenseService {
 private final ExpenseRepository repository;private final ExpenseHeadRepository heads;private final StaffRepository staff;
 private final ExpenseSettingsService settings;private final ExpenseNumberService numbers;private final ExpensePaymentService paymentService;
 private final ExpenseApprovalEventRepository events;private final ExpenseAccessGuard guard;private final ExpenseDtoMapper mapper;private final AuditWriteService audit;
 @Transactional public DetailResponse create(CreateRequest r,String key){guard.requireManage(ExpenseAccessGuard.EXPENSES);ExpenseHead h=head(r.expenseHeadId());if(h.getStatus()!=ExpenseHeadStatus.ACTIVE)throw new BadRequestException("Expense head is inactive");Staff s=activeStaff(r.requesterStaffId());ExpenseConfiguration c=settings.requireConfig();
  Expense e=new Expense();e.setExpenseNumber(numbers.nextNumber(r.expenseDate()));e.setExpenseDate(r.expenseDate());e.setExpenseHead(h);e.setCategory(h.getCategory());snapshot(e,h);e.setAmount(r.amount());e.setPaidAmount(BigDecimal.ZERO);e.setRemainingAmount(r.amount());e.setPaymentStatus(ExpensePaymentStatus.UNPAID);e.setVendorName(r.vendorName());e.setVendorInvoiceNumber(r.vendorInvoiceNumber());e.setRequesterStaffId(s.getStaffId());e.setRemarks(r.remarks());
  e.setApprovalStatus(Boolean.TRUE.equals(c.getApprovalRequired())?(r.saveAsDraft()?ExpenseApprovalStatus.DRAFT:ExpenseApprovalStatus.PENDING_APPROVAL):ExpenseApprovalStatus.APPROVED);if(e.getApprovalStatus()==ExpenseApprovalStatus.PENDING_APPROVAL)e.setSubmittedOn(Instant.now());if(e.getApprovalStatus()==ExpenseApprovalStatus.APPROVED){e.setApprovedOn(Instant.now());e.setApprovedBy(guard.currentUsername());}
  e=repository.save(e);if(e.getApprovalStatus()!=ExpenseApprovalStatus.DRAFT)addEvent(e,null,e.getApprovalStatus(),e.getApprovalStatus()==ExpenseApprovalStatus.APPROVED?ExpenseApprovalEventType.APPROVED:ExpenseApprovalEventType.SUBMITTED,null);
  if(r.initialPayment()!=null){if(e.getApprovalStatus()!=ExpenseApprovalStatus.APPROVED)throw new BadRequestException("Initial payment requires an approved expense");paymentService.record(e.getExpenseId(),r.initialPayment(),key);e=require(e.getExpenseId());}
  audit.record(AuditEventType.CREATE,"EXPENSE_CREATE","Expense",String.valueOf(e.getExpenseId()),"Expense created");return mapper.detail(e);}
 public DetailResponse get(Long id){guard.requireView(ExpenseAccessGuard.EXPENSES);return mapper.detail(require(id));}
 @Transactional public DetailResponse update(Long id,UpdateRequest r){guard.requireManage(ExpenseAccessGuard.EXPENSES);Expense e=require(id);boolean financialLocked=e.getApprovalStatus()==ExpenseApprovalStatus.APPROVED;
  if(financialLocked){
    if(r.amount()!=null&&r.amount().compareTo(e.getAmount())!=0)throw new BadRequestException("Amount is locked after approval; reject/return and resubmit to correct");
    if(r.expenseHeadId()!=null&&!r.expenseHeadId().equals(e.getExpenseHead().getExpenseHeadId()))throw new BadRequestException("Expense head and category are locked after approval; reject/return and resubmit to correct");
    e.setRemarks(r.remarks());
  }else if(e.getApprovalStatus()==ExpenseApprovalStatus.REJECTED){
    throw new BadRequestException("Return rejected expense to draft before editing financial fields");
  }else{
    ExpenseHead h=r.expenseHeadId()==null?e.getExpenseHead():head(r.expenseHeadId());
    if(h.getStatus()!=ExpenseHeadStatus.ACTIVE)throw new BadRequestException("Expense head is inactive");
    if(r.expenseDate()!=null)e.setExpenseDate(r.expenseDate());
    if(r.amount()!=null){e.setAmount(r.amount());e.setRemainingAmount(r.amount().subtract(e.getPaidAmount()==null?BigDecimal.ZERO:e.getPaidAmount()));}
    e.setExpenseHead(h);e.setCategory(h.getCategory());snapshot(e,h);
    if(r.requesterStaffId()!=null)e.setRequesterStaffId(activeStaff(r.requesterStaffId()).getStaffId());
    e.setVendorName(r.vendorName());e.setVendorInvoiceNumber(r.vendorInvoiceNumber());e.setRemarks(r.remarks());
  }
  repository.save(e);audit.record(AuditEventType.UPDATE,"EXPENSE_UPDATE","Expense",String.valueOf(id),"Expense updated");return mapper.detail(e);}
 @Transactional public DetailResponse submit(Long id){guard.requireManage(ExpenseAccessGuard.EXPENSES);Expense e=require(id);if(e.getApprovalStatus()!=ExpenseApprovalStatus.DRAFT)throw new BadRequestException("Only draft expenses can be submitted");ExpenseApprovalStatus from=e.getApprovalStatus();e.setApprovalStatus(ExpenseApprovalStatus.PENDING_APPROVAL);e.setSubmittedOn(Instant.now());repository.save(e);addEvent(e,from,e.getApprovalStatus(),ExpenseApprovalEventType.SUBMITTED,null);return mapper.detail(e);}
 @Transactional public DetailResponse returnToDraft(Long id){guard.requireManage(ExpenseAccessGuard.EXPENSES);Expense e=require(id);if(e.getApprovalStatus()!=ExpenseApprovalStatus.REJECTED)throw new BadRequestException("Only rejected expenses can return to draft");ExpenseApprovalStatus from=e.getApprovalStatus();e.setApprovalStatus(ExpenseApprovalStatus.DRAFT);repository.save(e);addEvent(e,from,e.getApprovalStatus(),ExpenseApprovalEventType.RETURNED_TO_DRAFT,null);return mapper.detail(e);}
 public Expense require(Long id){return repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Expense not found: "+id));}
 public DetailResponse toDetail(Expense e){return mapper.detail(e);}
 private ExpenseHead head(Long id){return heads.findById(id).orElseThrow(()->new ResourceNotFoundException("Expense head not found: "+id));}
 private Staff activeStaff(Long id){Staff s=staff.findById(id).orElseThrow(()->new ResourceNotFoundException("Staff not found: "+id));if(!Boolean.TRUE.equals(s.getActive()))throw new BadRequestException("Staff is inactive");return s;}
 private void snapshot(Expense e,ExpenseHead h){e.setHeadNameSnapshot(h.getName());e.setCategoryCodeSnapshot(h.getCategory().getCode());e.setCategoryNameSnapshot(h.getCategory().getName());}
 private void addEvent(Expense e,ExpenseApprovalStatus from,ExpenseApprovalStatus to,ExpenseApprovalEventType type,String remarks){ExpenseApprovalEvent a=new ExpenseApprovalEvent();a.setExpense(e);a.setFromStatus(from);a.setToStatus(to);a.setEventType(type);a.setActor(guard.currentUsername());a.setRemarks(remarks);a.setOccurredOn(Instant.now());events.save(a);}
}
