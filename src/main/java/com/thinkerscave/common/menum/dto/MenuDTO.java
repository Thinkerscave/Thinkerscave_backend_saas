package com.thinkerscave.common.menum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuDTO {

    private String name;
    private String description;
    private String url;
    private String icon;
    private String menuCode;
    private Integer order;
    private Boolean isActive;


}

// Record not a class