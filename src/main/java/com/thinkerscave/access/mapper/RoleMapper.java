package com.thinkerscave.access.mapper;

import com.thinkerscave.access.dto.response.RoleResponse;
import com.thinkerscave.access.entity.Role;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    @Mapping(target = "activeUserCount", ignore = true)
    RoleResponse toResponse(Role role);

    List<RoleResponse> toResponseList(List<Role> roles);
}
