package com.thinkerscave.finance.expense.repository;
import com.thinkerscave.finance.expense.entity.ExpenseHead;
import com.thinkerscave.finance.expense.enums.ExpenseHeadStatus;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ExpenseHeadRepository extends JpaRepository<ExpenseHead,Long>, JpaSpecificationExecutor<ExpenseHead> {
 boolean existsByNameIgnoreCaseAndExpenseHeadIdNot(String name,Long id);
 boolean existsByNameIgnoreCase(String name);
 List<ExpenseHead> findByStatusOrderByNameAsc(ExpenseHeadStatus status);
}
