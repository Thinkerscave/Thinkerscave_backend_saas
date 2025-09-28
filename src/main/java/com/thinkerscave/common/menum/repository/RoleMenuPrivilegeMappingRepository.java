package com.thinkerscave.common.menum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.domain.RoleMenuPrivilegeMapping;

@Repository
public interface RoleMenuPrivilegeMappingRepository extends JpaRepository<RoleMenuPrivilegeMapping, Long> {

	void deleteByRole(Role roleId);

	@Query("SELECT r FROM RoleMenuPrivilegeMapping r " 
			+ "JOIN FETCH r.subMenu s " 
			+ "JOIN FETCH s.menu m "
			+ "JOIN FETCH r.privilege p " 
			+ "WHERE r.role.roleId = :roleId " 
			+ "AND m.isActive = true "
			+ "AND s.isActive = true")
	List<RoleMenuPrivilegeMapping> findByRoleId(@Param("roleId") Long roleId);

}
