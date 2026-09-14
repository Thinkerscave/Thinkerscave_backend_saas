package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.enums.ExpensePaymentStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ExpensePaymentServiceTest {
 @Test void derivesPaymentStatuses(){
  assertEquals(ExpensePaymentStatus.UNPAID,ExpensePaymentService.deriveStatus(BigDecimal.ZERO,new BigDecimal("100")));
  assertEquals(ExpensePaymentStatus.PARTIALLY_PAID,ExpensePaymentService.deriveStatus(new BigDecimal("25"),new BigDecimal("100")));
  assertEquals(ExpensePaymentStatus.PAID,ExpensePaymentService.deriveStatus(new BigDecimal("100"),new BigDecimal("100")));
 }
 @Test void rejectsOverpayment(){
  assertThrows(IllegalArgumentException.class,()->ExpensePaymentService.validateNotOverpaid(new BigDecimal("101"),new BigDecimal("100")));
  assertDoesNotThrow(()->ExpensePaymentService.validateNotOverpaid(new BigDecimal("100"),new BigDecimal("100")));
 }
}
