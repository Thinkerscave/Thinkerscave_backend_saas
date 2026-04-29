package com.thinkerscave.common.usrm.dto;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
/**
 * 🛡️ InternalRoleDTO - Internal Data Carrier for Role Information
 * 
 * 🏗️ Purpose:
 * This DTO is used internally within the usrm module to transition role data
 * from the menum module's domain to user-focused responses. It ensures that 
 * we only expose necessary fields to the security context while maintaining
 * internal data integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalRoleDTO {
    private String roleCode;
    private String roleName;
    private String description;
}
