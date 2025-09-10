package com.thinkerscave.common.menum.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuOrderDTO {
    private Long menuId;
    private String menuName;
    private String menuCode;
    private Integer menuOrder;
    private List<SubMenuOrderDTO> subMenus;
}
