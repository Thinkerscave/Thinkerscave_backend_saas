package com.thinkerscave.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardSearchResponseDTO {

    private String query;
    private List<DashboardSearchResultDTO> results;
    private List<String> supportedCategories;
}
