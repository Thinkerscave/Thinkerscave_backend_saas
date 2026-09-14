package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.StudentFeeAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentFeeAccountRepository extends JpaRepository<StudentFeeAccount, Long> {

    Optional<StudentFeeAccount> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM StudentFeeAccount a WHERE a.studentId = :studentId AND a.academicYearId = :academicYearId")
    Optional<StudentFeeAccount> findForUpdate(@Param("studentId") Long studentId,
                                              @Param("academicYearId") Long academicYearId);
}
