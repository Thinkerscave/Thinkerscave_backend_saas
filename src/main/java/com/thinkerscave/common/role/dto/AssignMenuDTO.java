package com.thinkerscave.common.role.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignMenuDTO {
    private String roleCode;
    private List<String> menuCodes;
}
