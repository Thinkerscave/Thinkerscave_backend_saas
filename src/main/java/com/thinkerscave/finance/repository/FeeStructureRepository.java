package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeStructure;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeeStructureRepository extends JpaRepository<FeeStructure, Long> {

    Optional<FeeStructure> findByAcademicYearIdAndClassIdAndStatus(
            Long academicYearId, Long classId, FeeMasterStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FeeStructure s WHERE s.feeStructureId = :id")
    Optional<FeeStructure> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT s FROM FeeStructure s
            WHERE (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
              AND (:yearId IS NULL OR s.academicYearId = :yearId)
              AND (:classId IS NULL OR s.classId = :classId)
              AND (:status IS NULL OR s.status = :status)
            """)
    Page<FeeStructure> search(@Param("q") String q,
                              @Param("yearId") Long yearId,
                              @Param("classId") Long classId,
                              @Param("status") FeeMasterStatus status,
                              Pageable pageable);

    List<FeeStructure> findByStatus(FeeMasterStatus status);
}
