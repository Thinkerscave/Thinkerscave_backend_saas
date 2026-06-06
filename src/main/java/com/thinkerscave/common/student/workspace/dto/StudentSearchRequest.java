package com.thinkerscave.common.student.workspace.dto;

import lombok.Data;

@Data
public class StudentSearchRequest {
    private String keyword;
    private String classId;
    private String sectionId;
    private String status;
    private String parentName;
}
