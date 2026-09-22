package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.Expense;
public interface ExpenseService {
 DetailResponse create(CreateRequest request,String idempotencyKey); DetailResponse get(Long id); DetailResponse update(Long id,UpdateRequest request);
 DetailResponse submit(Long id); DetailResponse returnToDraft(Long id); Expense require(Long id); DetailResponse toDetail(Expense expense);
}
