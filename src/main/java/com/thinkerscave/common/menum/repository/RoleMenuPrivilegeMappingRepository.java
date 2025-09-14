package com.thinkerscave.common.menum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.domain.RoleMenuPrivilegeMapping;

@Repository
public interface RoleMenuPrivilegeMappingRepository extends JpaRepository<RoleMenuPrivilegeMapping, Long> {

	void deleteByRole(Role roleId);

}
