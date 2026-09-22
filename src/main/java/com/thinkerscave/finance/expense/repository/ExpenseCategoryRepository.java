package com.thinkerscave.finance.expense.repository;
import com.thinkerscave.finance.expense.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory,Long> {
 List<ExpenseCategory> findByActiveTrueOrderBySortOrderAscNameAsc();
}
