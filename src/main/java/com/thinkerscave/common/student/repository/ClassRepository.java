package com.thinkerscave.common.student.repository;

import com.thinkerscave.common.student.domain.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    // Multi-tenant: always scope class lookups to the caller's organization
    List<ClassEntity> findByOrganizationId(Long organizationId);
}
