package com.thinkerscave.common.admission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationStatusUpdateResponse {
    private int updatedCount;
    private int studentsCreated;
    private List<String> invalidApplicationIds;
}
