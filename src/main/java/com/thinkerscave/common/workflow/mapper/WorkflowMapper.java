package com.thinkerscave.common.workflow.mapper;

import com.thinkerscave.common.workflow.domain.WorkflowConfig;
import com.thinkerscave.common.workflow.dto.WorkflowConfigDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface WorkflowMapper {

    WorkflowConfigDTO toDto(WorkflowConfig config);

    List<WorkflowConfigDTO> toDtoList(List<WorkflowConfig> configs);

    WorkflowConfig toEntity(WorkflowConfigDTO dto);
}
