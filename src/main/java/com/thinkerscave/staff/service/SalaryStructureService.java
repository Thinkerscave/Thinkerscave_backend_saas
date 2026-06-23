package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.request.SalaryStructureRequest;
import com.thinkerscave.staff.dto.response.SalaryStructureResponse;

import java.util.List;

public interface SalaryStructureService {

    Long createSalaryStructure(SalaryStructureRequest request);

    void updateSalaryStructure(Long id, SalaryStructureRequest request);

    SalaryStructureResponse getCurrentSalaryStructure(Long staffId);

    List<SalaryStructureResponse> getSalaryHistory(Long staffId);
}
