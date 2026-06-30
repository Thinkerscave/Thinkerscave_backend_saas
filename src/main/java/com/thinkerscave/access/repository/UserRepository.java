package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserCode(String userCode);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Page<User> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<User> findByOrganizationIdAndStatus(Long organizationId, UserStatus status, Pageable pageable);

    long countByOrganizationIdAndStatus(Long organizationId, UserStatus status);

    @Query("SELECT u FROM User u WHERE u.organizationId = :orgId AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByOrganization(@Param("orgId") Long orgId, @Param("search") String search, Pageable pageable);
}
