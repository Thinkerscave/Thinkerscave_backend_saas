package com.thinkerscave.common.rbac.repository;

import com.thinkerscave.common.rbac.domain.Responsibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponsibilityRepository
        extends JpaRepository<Responsibility, Long>, JpaSpecificationExecutor<Responsibility> {

    Optional<Responsibility> findByOrganizationIdAndCode(Long organizationId, String code);

    List<Responsibility> findByOrganizationId(Long organizationId);
}
