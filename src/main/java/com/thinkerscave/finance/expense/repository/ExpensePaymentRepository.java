package com.thinkerscave.finance.expense.repository;
import com.thinkerscave.finance.expense.entity.ExpensePayment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.*;
public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment,Long> {
 Optional<ExpensePayment> findByIdempotencyKey(String key);
 List<ExpensePayment> findByExpense_ExpenseIdOrderByPaidOnAsc(Long id);
 boolean existsByExpense_ExpenseId(Long id);
 @Query("select coalesce(sum(p.amount),0) from ExpensePayment p where p.expense.expenseId=:id")
 BigDecimal sumByExpenseId(@Param("id") Long id);
}
