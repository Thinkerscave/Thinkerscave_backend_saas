package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.SalaryStructure;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {
    List<SalaryStructure> findByStatusOrderByNameAsc(ComponentStatus status);

    @Query("""
            SELECT s FROM SalaryStructure s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
              AND (:status IS NULL OR s.status = :status)
            """)
    Page<SalaryStructure> search(@Param("q") String q,
                                 @Param("status") ComponentStatus status,
                                 Pageable pageable);

    @Query("""
            SELECT s FROM SalaryStructure s
            WHERE (:status IS NULL OR s.status = :status)
            """)
    Page<SalaryStructure> filter(@Param("status") ComponentStatus status, Pageable pageable);
}
