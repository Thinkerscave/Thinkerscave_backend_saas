package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeePolicyRepository extends JpaRepository<FeePolicy, Long> {
    List<FeePolicy> findByOrganizationId(Long organizationId);
    Optional<FeePolicy> findByOrganizationIdAndCode(Long organizationId, String code);
}
