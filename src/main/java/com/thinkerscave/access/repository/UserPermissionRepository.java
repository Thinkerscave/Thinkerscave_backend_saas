package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUser_IdAndActiveTrue(Long userId);

    Optional<UserPermission> findByUser_IdAndMenu_IdAndActiveTrue(Long userId, Long menuId);

    Optional<UserPermission> findByUser_IdAndMenu_Id(Long userId, Long menuId);

    boolean existsByUser_IdAndMenu_Id(Long userId, Long menuId);

    @Query("SELECT up FROM UserPermission up JOIN FETCH up.menu WHERE up.user.id = :userId AND up.active = true ORDER BY up.menu.displayOrder ASC")
    List<UserPermission> findActiveWithMenu(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.user.id = :userId")
    void deleteAllByUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserPermission up WHERE up.menu.id = :menuId")
    void deleteByMenu_Id(@Param("menuId") Long menuId);
}
