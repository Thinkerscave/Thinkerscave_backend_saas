package com.thinkerscave.finance.expense.numbering;
import java.time.LocalDate;
public interface ExpenseNumberService { String nextNumber(LocalDate expenseDate); String preview(LocalDate expenseDate); }
