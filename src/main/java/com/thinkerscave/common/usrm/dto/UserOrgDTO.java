package com.thinkerscave.common.usrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserOrgDTO {
    private Long orgId;
    private String orgName;
    private String orgCode;
}
