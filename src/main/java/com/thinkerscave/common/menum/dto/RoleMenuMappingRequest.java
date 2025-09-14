package com.thinkerscave.common.menum.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuMappingRequest {
    private Long roleId;
    private List<SubMenuPrivilegeDTO> subMenuPrivileges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubMenuPrivilegeDTO {
        private Long subMenuId;
        private List<Long> privilegeIds;
    }
}

