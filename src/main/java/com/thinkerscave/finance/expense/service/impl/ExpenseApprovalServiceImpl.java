package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.*;
import com.thinkerscave.finance.expense.enums.*;
import com.thinkerscave.finance.expense.repository.*;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.ExpenseApprovalService;
import com.thinkerscave.shared.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpenseApprovalServiceImpl implements ExpenseApprovalService {
 private final ExpenseRepository expenses;private final ExpensePaymentRepository payments;private final ExpenseApprovalEventRepository events;
 private final ExpenseAccessGuard guard;private final ExpenseDtoMapper mapper;private final AuditWriteService audit;
 @Transactional public DetailResponse approve(Long id){guard.requireApprove(ExpenseAccessGuard.EXPENSE_APPROVAL);Expense e=require(id);checkPending(e);ExpenseApprovalStatus from=e.getApprovalStatus();e.setApprovalStatus(ExpenseApprovalStatus.APPROVED);e.setApprovedOn(Instant.now());e.setApprovedBy(guard.currentUsername());expenses.save(e);event(e,from,ExpenseApprovalEventType.APPROVED,null);audit.record(AuditEventType.APPROVAL,"EXPENSE_APPROVE","Expense",String.valueOf(id),"Expense approved");return mapper.detail(e);}
 @Transactional public DetailResponse reject(Long id,RejectRequest r){guard.requireApprove(ExpenseAccessGuard.EXPENSE_APPROVAL);Expense e=require(id);checkPending(e);if(payments.existsByExpense_ExpenseId(id))throw new BadRequestException("Paid expense cannot be rejected");ExpenseApprovalStatus from=e.getApprovalStatus();e.setApprovalStatus(ExpenseApprovalStatus.REJECTED);e.setRejectedOn(Instant.now());e.setRejectedBy(guard.currentUsername());e.setRejectionReason(r.remarks());expenses.save(e);event(e,from,ExpenseApprovalEventType.REJECTED,r.remarks());audit.record(AuditEventType.APPROVAL,"EXPENSE_REJECT","Expense",String.valueOf(id),"Expense rejected");return mapper.detail(e);}
 private Expense require(Long id){return expenses.findById(id).orElseThrow(()->new ResourceNotFoundException("Expense not found: "+id));}
 private void checkPending(Expense e){if(e.getApprovalStatus()!=ExpenseApprovalStatus.PENDING_APPROVAL)throw new BadRequestException("Expense is not pending approval");}
 private void event(Expense e,ExpenseApprovalStatus from,ExpenseApprovalEventType type,String remarks){ExpenseApprovalEvent a=new ExpenseApprovalEvent();a.setExpense(e);a.setEventType(type);a.setFromStatus(from);a.setToStatus(e.getApprovalStatus());a.setActor(guard.currentUsername());a.setRemarks(remarks);a.setOccurredOn(Instant.now());events.save(a);}
}
