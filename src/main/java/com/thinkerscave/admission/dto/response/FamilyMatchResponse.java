package com.thinkerscave.admission.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMatchResponse {
    private boolean matched;
    private Long parentId;
    private String parentName;
    private String mobileNumber;
    private String email;
    @Builder.Default
    private List<SiblingSummary> students = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SiblingSummary {
        private Long studentId;
        private String studentName;
        private String className;
        private String studentCode;
    }
}
