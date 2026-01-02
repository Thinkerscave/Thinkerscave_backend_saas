package com.thinkerscave.common.course.dto;

import com.thinkerscave.common.course.enums.ContainerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Academic Container (Class, Section, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicContainerDTO {
    private Long containerId;
    private ContainerType containerType;
    private String containerCode;
    private String containerName;
    private Long organisationId;
    private Long academicYearId;
    private Long courseId;
    private Long parentContainerId;
    private Integer level;
    private Integer capacity;
    private Integer currentStrength;
    private List<AcademicContainerDTO> childContainers;
}
