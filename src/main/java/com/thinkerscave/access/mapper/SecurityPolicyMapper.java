package com.thinkerscave.access.mapper;

import com.thinkerscave.access.dto.response.SecurityPolicyResponse;
import com.thinkerscave.access.entity.SecurityPolicy;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SecurityPolicyMapper {

    SecurityPolicyResponse toResponse(SecurityPolicy policy);
}
