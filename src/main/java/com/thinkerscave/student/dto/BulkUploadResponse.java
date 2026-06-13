package com.thinkerscave.student.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkUploadResponse {

    private Integer totalRecords;

    private Integer successCount;

    private Integer failureCount;

    private List<String> errors = new ArrayList<>();
}