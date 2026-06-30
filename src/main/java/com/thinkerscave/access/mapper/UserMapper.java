package com.thinkerscave.access.mapper;

import com.thinkerscave.access.dto.response.UserSummaryResponse;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    UserSummaryResponse toSummary(User user);

    List<UserSummaryResponse> toSummaryList(List<User> users);

    default List<UserSummaryResponse.UserRoleSummary> mapRoles(User user) {
        if (user.getUserRoles() == null) return List.of();
        return user.getUserRoles().stream()
                .filter(UserRole::getActive)
                .map(ur -> UserSummaryResponse.UserRoleSummary.builder()
                        .roleId(ur.getRole().getId())
                        .roleName(ur.getRole().getRoleName())
                        .roleCode(ur.getRole().getRoleCode())
                        .roleType(ur.getRole().getRoleType().name())
                        .primaryRole(ur.getPrimaryRole())
                        .build())
                .toList();
    }
}
