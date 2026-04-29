package com.thinkerscave.common.course.dto;

import com.thinkerscave.common.course.enums.ContainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureTemplateDTO {
    private ContainerType rootType;
    private String rootPrefix;
    private Integer rootStartRange;
    private Integer rootEndRange;

    private ContainerType childType;
    private String childPrefix;
    private List<String> childNames;
}
