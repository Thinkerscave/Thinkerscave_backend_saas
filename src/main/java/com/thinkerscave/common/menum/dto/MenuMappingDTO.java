package com.thinkerscave.common.menum.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuMappingDTO {
	private Long menuId;
	private String menuName;
	private String menuIcon;
	private List<SubMenuMappingDTO> subMenus;
}
