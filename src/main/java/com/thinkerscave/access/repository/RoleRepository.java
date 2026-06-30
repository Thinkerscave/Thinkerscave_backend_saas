package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    Optional<Role> findByRoleCode(String roleCode);

    Optional<Role> findByRoleName(String roleName);

    boolean existsByRoleCode(String roleCode);

    boolean existsByRoleCodeAndIdNot(String roleCode, Long id);

    boolean existsByRoleNameAndIdNot(String roleName, Long id);

    List<Role> findByActiveTrueOrderByDisplayOrderAsc();

    List<Role> findByRoleTypeAndActiveTrueOrderByDisplayOrderAsc(RoleType roleType);

    Page<Role> findByActive(Boolean active, Pageable pageable);

    Optional<Role> findByRoleType(RoleType roleType);
}
