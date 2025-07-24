package com.thinkerscave.common.role.repository;


import com.thinkerscave.common.role.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Role entity operations.
 * Provides methods to fetch, search, and filter roles.
 *
 * @author Sandeep
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

	/** Returns all active roles. */
	@Query("SELECT r FROM role_master r WHERE r.isActive = true")
	List<Role> findAllRoles();

	/** Finds a role by its name. */
	Optional<Role> findByRoleName(String roleName);

	/** Finds a role by its code. */
	Optional<Role> findByRoleCode(String roleCode);
}