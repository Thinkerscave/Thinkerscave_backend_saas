package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.request.SalaryComponentRequest;
import com.thinkerscave.finance.payroll.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.payroll.dto.response.SalaryComponentResponse;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SalaryComponentService {
    PageResponse<SalaryComponentResponse> list(String q, ComponentType type, ComponentStatus status, Pageable pageable);
    List<SalaryComponentResponse> lookups();
    SalaryComponentResponse get(Long id);
    SalaryComponentResponse create(SalaryComponentRequest request);
    SalaryComponentResponse update(Long id, SalaryComponentRequest request);
    SalaryComponentResponse updateStatus(Long id, StatusUpdateRequest request);
    void delete(Long id);
}
