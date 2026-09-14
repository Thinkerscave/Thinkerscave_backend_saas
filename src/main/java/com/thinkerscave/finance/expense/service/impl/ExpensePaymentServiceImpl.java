package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.entity.FeePaymentMethod;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.*;
import com.thinkerscave.finance.expense.enums.ExpenseApprovalStatus;
import com.thinkerscave.finance.expense.repository.*;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.ExpensePaymentService;
import com.thinkerscave.finance.repository.FeePaymentMethodRepository;
import com.thinkerscave.shared.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpensePaymentServiceImpl implements ExpensePaymentService {
 private final ExpenseRepository expenses;private final ExpensePaymentRepository payments;private final FeePaymentMethodRepository methods;
 private final ExpenseAccessGuard guard;private final ExpenseDtoMapper mapper;private final AuditWriteService audit;
 @Transactional public PaymentResult record(Long id,PaymentRequest r,String key){guard.requireManage(ExpenseAccessGuard.EXPENSE_PAYMENT);if(key==null||key.isBlank())throw new BadRequestException("Idempotency-Key is required");
  Optional<ExpensePayment> existing=payments.findByIdempotencyKey(key);if(existing.isPresent()){ExpensePayment p=existing.get();if(!p.getExpense().getExpenseId().equals(id))throw new BadRequestException("Idempotency-Key belongs to another expense");return result(p,p.getExpense());}
  Expense e=expenses.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Expense not found: "+id));if(e.getApprovalStatus()!=ExpenseApprovalStatus.APPROVED)throw new BadRequestException("Payment requires an approved expense");
  try{ExpensePaymentService.validateNotOverpaid(r.amount(),e.getRemainingAmount());}catch(IllegalArgumentException ex){throw new BadRequestException(ex.getMessage());}
  ExpensePayment p=new ExpensePayment();p.setExpense(e);p.setAmount(r.amount());p.setPaidOn(r.paidOn());p.setIdempotencyKey(key);p.setReferenceNumber(r.referenceNumber());p.setRemarks(r.remarks());
  if(r.paymentMethodId()!=null){FeePaymentMethod m=methods.findById(r.paymentMethodId()).orElseThrow(()->new ResourceNotFoundException("Payment method not found"));p.setPaymentMethodId(m.getFeePaymentMethodId());p.setPaymentMethodName(m.getName());}
  p=payments.save(p);BigDecimal paid=payments.sumByExpenseId(id);e.setPaidAmount(paid);e.setRemainingAmount(e.getAmount().subtract(paid));e.setPaymentStatus(ExpensePaymentService.deriveStatus(paid,e.getAmount()));expenses.save(e);
  audit.record(AuditEventType.CREATE,"EXPENSE_PAYMENT","ExpensePayment",String.valueOf(p.getExpensePaymentId()),"Expense payment recorded");return result(p,e);}
 public List<PaymentResponse> list(Long id){if(!guard.canView(ExpenseAccessGuard.EXPENSES))guard.requireView(ExpenseAccessGuard.EXPENSE_PAYMENT);if(!expenses.existsById(id))throw new ResourceNotFoundException("Expense not found: "+id);return payments.findByExpense_ExpenseIdOrderByPaidOnAsc(id).stream().map(mapper::payment).toList();}
 private PaymentResult result(ExpensePayment p,Expense e){return new PaymentResult(mapper.payment(p),e.getPaymentStatus(),e.getPaidAmount(),e.getRemainingAmount());}
}
