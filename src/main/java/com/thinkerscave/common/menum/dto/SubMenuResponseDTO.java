package com.thinkerscave.common.menum.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.thinkerscave.common.menum.domain.Privilege;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubMenuResponseDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long subMenuId;
    private String subMenuName;
    private String subMenuCode;
    private String subMenuUrl;
    private String subMenuIcon;
    private Integer subMenuOrder;
    private Boolean subMenuIsActive;
    private String subMenuDescription;
    private Long menuId;
    private String menuName;
    private String menuCode;
    private String createdBy;
    private Date lastUpdatedOn;
    private List<Privilege> privileges;
}
