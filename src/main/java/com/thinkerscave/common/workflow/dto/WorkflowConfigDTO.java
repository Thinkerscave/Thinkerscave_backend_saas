package com.thinkerscave.common.workflow.dto;

import com.thinkerscave.common.enums.GenericStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowConfigDTO {
    private Long id;
    private String workflowKey;
    private String displayName;
    private String description;
    private boolean requireApproval;
    private Integer approvalLevels;
    private String approverRoleCodes;
    private Integer autoCloseAfterDays;
    private GenericStatus status;
}
