package com.thinkerscave.access.mapper;

import com.thinkerscave.access.dto.response.MenuResponse;
import com.thinkerscave.access.entity.Menu;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper {

    @Mapping(target = "parentMenuId", source = "parentMenu.id")
    @Mapping(target = "parentMenuName", source = "parentMenu.menuName")
    @Mapping(target = "featureId", source = "feature.id")
    @Mapping(target = "featureCode", source = "feature.featureCode")
    @Mapping(target = "featureName", source = "feature.featureName")
    @Mapping(target = "children", ignore = true)
    MenuResponse toResponse(Menu menu);

    @Mapping(target = "parentMenuId", source = "parentMenu.id")
    @Mapping(target = "parentMenuName", source = "parentMenu.menuName")
    @Mapping(target = "featureId", source = "feature.id")
    @Mapping(target = "featureCode", source = "feature.featureCode")
    @Mapping(target = "featureName", source = "feature.featureName")
    @Mapping(target = "children", ignore = true)
    List<MenuResponse> toResponseList(List<Menu> menus);
}
