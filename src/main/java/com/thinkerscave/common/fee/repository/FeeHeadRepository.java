package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeHead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeHeadRepository extends JpaRepository<FeeHead, Long>, JpaSpecificationExecutor<FeeHead> {
    List<FeeHead> findByOrganizationId(Long organizationId);
    Optional<FeeHead> findByOrganizationIdAndCode(Long organizationId, String code);
}
