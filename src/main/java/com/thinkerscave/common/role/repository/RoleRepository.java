package com.thinkerscave.common.role.repository;


import com.thinkerscave.common.role.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
	
	@Query("SELECT r FROM Role r WHERE r.isActive = true")
	List<Role> findAllRoles();

	Optional<Role> findByRoleName(String roleName);

	Optional<Role> findByRoleCode(String roleCode);


}
