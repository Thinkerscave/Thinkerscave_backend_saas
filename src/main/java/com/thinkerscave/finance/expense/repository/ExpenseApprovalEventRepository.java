package com.thinkerscave.finance.expense.repository;
import com.thinkerscave.finance.expense.entity.ExpenseApprovalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExpenseApprovalEventRepository extends JpaRepository<ExpenseApprovalEvent,Long> {
 List<ExpenseApprovalEvent> findByExpense_ExpenseIdOrderByOccurredOnAsc(Long id);
}
