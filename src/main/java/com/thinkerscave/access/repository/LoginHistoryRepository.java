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
import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUser_IdOrderByLoginTimeDesc(Long userId, Pageable pageable);

    @Query("SELECT COUNT(lh) FROM LoginHistory lh WHERE lh.user.id = :userId AND lh.status = 'FAILED' AND lh.loginTime > :since")
    long countRecentFailures(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    Optional<LoginHistory> findTopByUser_IdAndStatusAndLogoutTimeIsNullOrderByLoginTimeDesc(Long userId, LoginStatus status);

    @Query(value = """
            SELECT lh FROM LoginHistory lh JOIN FETCH lh.user u
            WHERE u.id = :userId
              AND lh.loginTime >= :fromTime AND lh.loginTime <= :toTime
              AND (:status IS NULL OR lh.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY lh.loginTime DESC
            """,
            countQuery = """
            SELECT COUNT(lh) FROM LoginHistory lh
            WHERE lh.user.id = :userId
              AND lh.loginTime >= :fromTime AND lh.loginTime <= :toTime
              AND (:status IS NULL OR lh.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(COALESCE(lh.user.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(lh.user.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(lh.user.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LoginHistory> findByUserIdAndWindow(
            @Param("userId") Long userId,
            @Param("status") LoginStatus status,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("search") String search,
            Pageable pageable);

    @Query(value = """
            SELECT lh FROM LoginHistory lh JOIN FETCH lh.user u
            WHERE u.organizationId = :orgId
              AND lh.loginTime >= :fromTime AND lh.loginTime <= :toTime
              AND (:status IS NULL OR lh.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY lh.loginTime DESC
            """,
            countQuery = """
            SELECT COUNT(lh) FROM LoginHistory lh
            WHERE lh.user.organizationId = :orgId
              AND lh.loginTime >= :fromTime AND lh.loginTime <= :toTime
              AND (:status IS NULL OR lh.status = :status)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(COALESCE(lh.user.displayName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(lh.user.username, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(lh.user.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LoginHistory> findByOrganizationIdAndWindow(
            @Param("orgId") Long orgId,
            @Param("status") LoginStatus status,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("search") String search,
            Pageable pageable);
}
