package com.thinkerscave.finance.expense.repository;

import com.thinkerscave.finance.expense.entity.Expense;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense,Long>, JpaSpecificationExecutor<Expense> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select e from Expense e where e.expenseId=:id")
 Optional<Expense> findByIdForUpdate(@Param("id") Long id);
 long countByExpenseHead_ExpenseHeadId(Long headId);
}
