package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
public interface ExpenseOverviewService { OverviewResponse overview(Filter filter); PageResponse<ListRow> list(Filter filter,Pageable pageable); }
