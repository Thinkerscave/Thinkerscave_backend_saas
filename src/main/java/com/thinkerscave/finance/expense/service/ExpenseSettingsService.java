package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.finance.expense.entity.ExpenseConfiguration;
public interface ExpenseSettingsService { SettingsResponse get(); SettingsResponse update(SettingsRequest request); ExpenseConfiguration requireConfig(); }
