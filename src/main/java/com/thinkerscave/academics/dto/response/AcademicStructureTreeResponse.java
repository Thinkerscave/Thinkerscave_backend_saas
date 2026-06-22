package com.thinkerscave.academics.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AcademicStructureTreeResponse {
    private String stage;
    private List<ClassNode> classes;

    @Getter
    @Setter
    @Builder
    public static class ClassNode {
        private Long classId;
        private String className;
        private String classCode;
        private Integer displayOrder;
        private List<SectionNode> sections;
    }

    @Getter
    @Setter
    @Builder
    public static class SectionNode {
        private Long sectionId;
        private String sectionName;
        private Integer capacity;
    }
}
