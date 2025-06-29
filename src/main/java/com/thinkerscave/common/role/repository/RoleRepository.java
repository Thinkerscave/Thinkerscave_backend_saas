package com.thinkerscave.common.role.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thinkerscave.common.role.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
    Optional<Role> findByRoleCode(String roleCode);
}
