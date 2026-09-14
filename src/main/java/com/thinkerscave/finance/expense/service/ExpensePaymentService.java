package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.enums.ExpensePaymentStatus;
import java.math.BigDecimal;
import java.util.List;
public interface ExpensePaymentService {
 PaymentResult record(Long expenseId,PaymentRequest request,String idempotencyKey); List<PaymentResponse> list(Long expenseId);
 static ExpensePaymentStatus deriveStatus(BigDecimal paid,BigDecimal total){
  if(paid==null||paid.signum()==0)return ExpensePaymentStatus.UNPAID;
  return paid.compareTo(total)>=0?ExpensePaymentStatus.PAID:ExpensePaymentStatus.PARTIALLY_PAID;
 }
 static void validateNotOverpaid(BigDecimal payment,BigDecimal remaining){
  if(payment==null||payment.signum()<=0)throw new IllegalArgumentException("Payment amount must be positive");
  if(payment.compareTo(remaining)>0)throw new IllegalArgumentException("Payment exceeds remaining amount");
 }
}
