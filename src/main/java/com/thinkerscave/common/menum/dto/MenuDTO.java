package com.thinkerscave.common.menum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuDTO {

    private String name;
    private String description;
    private String url;
    private String icon;
    private Integer order;
    private Boolean isActive;

}
