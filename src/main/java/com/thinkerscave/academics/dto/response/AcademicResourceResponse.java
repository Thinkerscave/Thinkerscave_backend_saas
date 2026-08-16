package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.ResourceType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AcademicResourceResponse {

    private Long academicResourceId;
    private String name;
    private String code;
    private ResourceType resourceType;
    private Integer capacity;
    private Boolean active;
}
