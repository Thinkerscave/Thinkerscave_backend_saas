package com.thinkerscave.common.menum.dto;

import java.util.List;

import com.thinkerscave.common.menum.domain.Privilege;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubMenuMappingDTO {
	private Long subMenuId;
    private String subMenuName;
    private String subMenuUrl;
    private List<Privilege> privileges;
}
