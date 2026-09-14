package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.SalaryComponent;
import com.thinkerscave.finance.payroll.enums.ComponentStatus;
import com.thinkerscave.finance.payroll.enums.ComponentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, Long> {
    Optional<SalaryComponent> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<SalaryComponent> findByStatusOrderBySortOrderAscNameAsc(ComponentStatus status);

    @Query("""
            SELECT c FROM SalaryComponent c
            WHERE (LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:type IS NULL OR c.componentType = :type)
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<SalaryComponent> search(@Param("q") String q,
                                 @Param("type") ComponentType type,
                                 @Param("status") ComponentStatus status,
                                 Pageable pageable);

    @Query("""
            SELECT c FROM SalaryComponent c
            WHERE (:type IS NULL OR c.componentType = :type)
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<SalaryComponent> filter(@Param("type") ComponentType type,
                                 @Param("status") ComponentStatus status,
                                 Pageable pageable);
}
