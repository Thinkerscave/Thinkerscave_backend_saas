package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChapterRequest {

    @NotNull(message = "Chapter number is mandatory")
    private Integer chapterNumber;

    @NotBlank(message = "Chapter name is mandatory")
    private String chapterName;

    private Integer estimatedHours;
    private Integer displayOrder;
    private String remarks;
}
