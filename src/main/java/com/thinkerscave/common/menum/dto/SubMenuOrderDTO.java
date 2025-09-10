package com.thinkerscave.common.menum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubMenuOrderDTO {
    private Long subMenuId;
    private String subMenuName;
    private String subMenuCode;
    private Integer subMenuOrder;
}
