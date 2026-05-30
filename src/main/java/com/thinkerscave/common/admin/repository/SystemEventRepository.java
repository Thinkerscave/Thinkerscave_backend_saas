package com.thinkerscave.common.admin.repository;

import com.thinkerscave.common.admin.domain.SystemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEvent, Long> {

    List<SystemEvent> findTop50ByOrderByOccurredAtDesc();

    List<SystemEvent> findTop50ByOrganizationIdOrderByOccurredAtDesc(Long organizationId);

    long countByResolvedFalse();

    long countByResolvedFalseAndSeverityIn(Collection<String> severities);
}