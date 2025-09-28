package com.thinkerscave.common.menum.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SideMenuDTO {
    private String label;
    private String icon;
    private String routerLink;
    private List<SideMenuDTO> items; 
    private List<String> privileges;
}
