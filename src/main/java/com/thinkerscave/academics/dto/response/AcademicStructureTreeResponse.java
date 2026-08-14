package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.AcademicStage;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AcademicStructureTreeResponse {
    private AcademicStage stage;
    private List<ClassNode> classes;

    @Getter
    @Setter
    @Builder
    public static class ClassNode {
        private Long classId;
        private String name;
        private String code;
        private Boolean active;
        private Integer displayOrder;
        private List<SectionNode> sections;

        public String getClassName() {
            return name;
        }

        public String getClassCode() {
            return code;
        }
    }

    @Getter
    @Setter
    @Builder
    public static class SectionNode {
        private Long sectionId;
        private String name;
        private String code;
        private Integer capacity;
        private Boolean active;

        public String getSectionName() {
            return name;
        }
    }
}
