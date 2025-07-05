package com.thinkerscave.common.orgm.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrgRegistrationResponse {
    private String message;
    private String orgCode;
    private String userCode;
}
