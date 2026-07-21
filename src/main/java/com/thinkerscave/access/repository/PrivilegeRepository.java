package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.Privilege;
import com.thinkerscave.access.enums.PrivilegeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByPrivilegeCode(String privilegeCode);

    Optional<Privilege> findByPrivilegeType(PrivilegeType privilegeType);

    boolean existsByPrivilegeCode(String privilegeCode);
}
