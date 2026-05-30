package com.thinkerscave.common.rbac.mapper;

import com.thinkerscave.common.rbac.domain.UserResponsibility;
import com.thinkerscave.common.rbac.dto.UserResponsibilityDTO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface RbacMapper {

    UserResponsibilityDTO toDto(UserResponsibility entity);

    List<UserResponsibilityDTO> toDtoList(List<UserResponsibility> entities);

    UserResponsibility toEntity(UserResponsibilityDTO dto);
}
