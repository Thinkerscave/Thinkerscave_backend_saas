package com.thinkerscave.common.payroll.service;

import com.thinkerscave.common.payroll.dto.PayrollDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PayrollService {
    List<PayrollDTO> getAllPayroll();

    Page<PayrollDTO> getAllPayroll(Pageable pageable);

    PayrollDTO getByStaffId(Long staffId);

    PayrollDTO saveOrUpdate(PayrollDTO dto, String updatedBy);

    Map<String, Object> runPayroll(String runBy);
}
