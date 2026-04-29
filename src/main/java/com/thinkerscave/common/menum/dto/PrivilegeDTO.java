package com.thinkerscave.common.menum.dto;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivilegeDTO {
    private Long privilegeId;
    private String privilegeName;
}
