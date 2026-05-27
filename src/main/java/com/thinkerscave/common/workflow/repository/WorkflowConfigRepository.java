package com.thinkerscave.common.workflow.repository;

import com.thinkerscave.common.workflow.domain.WorkflowConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfig, Long> {

    Optional<WorkflowConfig> findByOrganizationIdAndWorkflowKey(Long organizationId, String workflowKey);

    List<WorkflowConfig> findByOrganizationId(Long organizationId);
}
