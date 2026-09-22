package com.thinkerscave.finance.expense.service.impl;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.CategoryResponse;
import com.thinkerscave.finance.expense.repository.ExpenseCategoryRepository;
import com.thinkerscave.finance.expense.security.ExpenseAccessGuard;
import com.thinkerscave.finance.expense.service.ExpenseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {
 private final ExpenseCategoryRepository repository; private final ExpenseAccessGuard guard;
 public List<CategoryResponse> list(){if(!guard.canView(ExpenseAccessGuard.EXPENSE_HEADS))guard.requireView(ExpenseAccessGuard.EXPENSES);
  return repository.findByActiveTrueOrderBySortOrderAscNameAsc().stream().map(c->new CategoryResponse(c.getExpenseCategoryId(),c.getCode(),c.getName(),c.getDescription(),c.getSortOrder())).toList();}
}
