package com.thinkerscave.finance.expense.service;
import com.thinkerscave.finance.expense.dto.ExpenseDtos.*;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface ExpenseHeadService {
 PageResponse<HeadResponse> list(String q,Pageable pageable); List<HeadResponse> lookups(); HeadResponse get(Long id);
 HeadResponse create(HeadRequest request); HeadResponse update(Long id,HeadRequest request);
 HeadResponse updateStatus(Long id,HeadStatusRequest request); void delete(Long id);
}
