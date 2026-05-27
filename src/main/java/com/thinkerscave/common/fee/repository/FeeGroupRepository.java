package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeGroupRepository extends JpaRepository<FeeGroup, Long> {
    List<FeeGroup> findByOrganizationId(Long organizationId);
    Optional<FeeGroup> findByOrganizationIdAndCode(Long organizationId, String code);
}
