package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.SecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {

    Optional<SecurityPolicy> findByOrganizationId(Long organizationId);

    boolean existsByOrganizationId(Long organizationId);
}
