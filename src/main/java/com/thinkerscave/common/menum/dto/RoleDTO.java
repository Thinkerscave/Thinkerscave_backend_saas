package com.thinkerscave.common.menum.dto;

import java.util.Date;

import com.thinkerscave.common.enums.RoleType;

import lombok.Data;

@Data
public class RoleDTO {
    private Long roleId;
    private String roleName;
    private String roleCode;
    private String description;
    private Boolean isActive;
    private RoleType roleType;
    private Long organizationId;
    private String createdBy;
    private Date lastModifiedDate; 
}
