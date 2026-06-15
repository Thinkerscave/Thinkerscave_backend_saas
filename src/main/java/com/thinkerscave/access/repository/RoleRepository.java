package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);
    Optional<Role> findByRoleName(String roleName);
}
