package com.thinkerscave.common.menum.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.usrm.domain.User;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
	
	Optional<Role> findByRoleCode(String roleCode);

	boolean existsByRoleCode(String roleCode);
	
	List<Role> findByIsActiveTrue();

	Optional<Role> findByRoleName(String string);
}
