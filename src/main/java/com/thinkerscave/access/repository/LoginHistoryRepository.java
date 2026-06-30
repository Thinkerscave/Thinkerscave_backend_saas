package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.LoginHistory;
import com.thinkerscave.access.enums.LoginStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUser_IdOrderByLoginTimeDesc(Long userId, Pageable pageable);

    Page<LoginHistory> findByUser_IdAndStatusOrderByLoginTimeDesc(Long userId, LoginStatus status, Pageable pageable);

    @Query("SELECT lh FROM LoginHistory lh WHERE lh.user.organizationId = :orgId ORDER BY lh.loginTime DESC")
    Page<LoginHistory> findByOrganizationId(@Param("orgId") Long orgId, Pageable pageable);

    @Query("SELECT lh FROM LoginHistory lh WHERE lh.user.organizationId = :orgId AND lh.status = :status ORDER BY lh.loginTime DESC")
    Page<LoginHistory> findByOrganizationIdAndStatus(@Param("orgId") Long orgId, @Param("status") LoginStatus status, Pageable pageable);

    @Query("SELECT COUNT(lh) FROM LoginHistory lh WHERE lh.user.id = :userId AND lh.status = 'FAILED' AND lh.loginTime > :since")
    long countRecentFailures(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
