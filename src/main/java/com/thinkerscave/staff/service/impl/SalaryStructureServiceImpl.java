package com.thinkerscave.staff.service.impl;

import com.thinkerscave.staff.dto.request.SalaryStructureRequest;
import com.thinkerscave.staff.dto.response.SalaryStructureResponse;
import com.thinkerscave.staff.service.SalaryStructureService;

import java.util.List;

/**
 * @deprecated Staff-owned salary structures are retired. Use Finance Payroll APIs.
 * Not a Spring bean — retained only as a compile-time stub.
 */
@Deprecated
public class SalaryStructureServiceImpl implements SalaryStructureService {

    private static final String MSG = "Use Finance Payroll APIs";

    @Override
    public Long createSalaryStructure(SalaryStructureRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public void updateSalaryStructure(Long id, SalaryStructureRequest request) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public SalaryStructureResponse getCurrentSalaryStructure(Long staffId) {
        throw new UnsupportedOperationException(MSG);
    }

    @Override
    public List<SalaryStructureResponse> getSalaryHistory(Long staffId) {
        throw new UnsupportedOperationException(MSG);
    }
}
