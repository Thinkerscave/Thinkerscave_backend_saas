package com.thinkerscave.finance.payroll.service;

import com.thinkerscave.finance.payroll.dto.request.SalaryStructureRequest;
import com.thinkerscave.finance.payroll.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.payroll.dto.response.SalaryStructureResponse;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SalaryStructureService {
    PageResponse<SalaryStructureResponse> list(String q, ComponentStatus status, Pageable pageable);
    List<SalaryStructureResponse> lookups();
    SalaryStructureResponse get(Long id);
    SalaryStructureResponse create(SalaryStructureRequest request);
    SalaryStructureResponse update(Long id, SalaryStructureRequest request);
    SalaryStructureResponse updateStatus(Long id, StatusUpdateRequest request);
    void delete(Long id);
}
