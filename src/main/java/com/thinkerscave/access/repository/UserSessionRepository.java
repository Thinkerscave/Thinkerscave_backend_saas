package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.UserSession;
import com.thinkerscave.access.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshToken(String refreshToken);

    List<UserSession> findByUser_IdAndStatusOrderByLoginAtDesc(Long userId, SessionStatus status);

    Page<UserSession> findByUser_IdOrderByLoginAtDesc(Long userId, Pageable pageable);

    long countByUser_IdAndStatus(Long userId, SessionStatus status);

    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.status = 'ACTIVE' ORDER BY us.loginAt DESC")
    List<UserSession> findActiveSessions(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserSession us SET us.status = 'LOGGED_OUT', us.logoutAt = :logoutAt WHERE us.user.id = :userId AND us.status = 'ACTIVE'")
    void terminateAllActiveSessions(@Param("userId") Long userId, @Param("logoutAt") LocalDateTime logoutAt);

    @Modifying
    @Query("UPDATE UserSession us SET us.status = 'EXPIRED' WHERE us.status = 'ACTIVE' AND us.loginAt < :cutoff")
    void expireOldSessions(@Param("cutoff") LocalDateTime cutoff);
}
