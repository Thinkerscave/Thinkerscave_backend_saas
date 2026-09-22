package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
public interface ExpenseApprovalService { DetailResponse approve(Long id); DetailResponse reject(Long id,RejectRequest request); }
