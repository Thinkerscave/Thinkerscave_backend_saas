package com.thinkerscave.common.menum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubMenuRequestDTO {
	private Long subMenuId;
    private String subMenuName;
    private String subMenuCode;
    private String subMenuUrl;
    private String subMenuIcon;
    private Integer subMenuOrder;
    private Boolean subMenuIsActive;
    private String subMenuDescription;
    private Long menuId;
}
