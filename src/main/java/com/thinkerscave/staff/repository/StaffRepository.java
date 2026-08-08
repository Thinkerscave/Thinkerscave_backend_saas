package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByStaffCode(String staffCode);

    Optional<Staff> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT s FROM Staff s
            WHERE (:staffType IS NULL OR s.staffType = :staffType)
              AND (:employmentCategory IS NULL OR s.employmentCategory = :employmentCategory)
              AND (:employmentStatus IS NULL OR s.employmentStatus = :employmentStatus)
              AND (:designation IS NULL OR LOWER(s.designation) LIKE LOWER(CONCAT('%', CAST(:designation AS string), '%')))
              AND (:keyword IS NULL OR
                   LOWER(s.firstName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                   LOWER(s.lastName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                   LOWER(s.staffCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                   LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
              AND s.active = true
            """)
    Page<Staff> searchStaff(
            @Param("staffType") StaffType staffType,
            @Param("employmentCategory") EmploymentCategory employmentCategory,
            @Param("employmentStatus") EmploymentStatus employmentStatus,
            @Param("designation") String designation,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByStaffType(StaffType staffType);

    long countByEmploymentStatus(EmploymentStatus employmentStatus);

    long countByEmploymentCategory(EmploymentCategory employmentCategory);

    long countByActive(Boolean active);

    @Query("SELECT COUNT(s) FROM Staff s WHERE s.active = true")
    long countActiveStaff();

    Optional<Staff> findByUser_Id(Long userId);

    List<Staff> findByActiveTrueAndEmploymentStatus(EmploymentStatus employmentStatus);
}
