package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.SalaryStructureItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryStructureItemRepository extends JpaRepository<SalaryStructureItem, Long> {
    List<SalaryStructureItem> findBySalaryStructure_SalaryStructureIdOrderBySalaryStructureItemIdAsc(Long structureId);
    void deleteBySalaryStructure_SalaryStructureId(Long structureId);
    boolean existsBySalaryComponent_SalaryComponentId(Long componentId);
}
