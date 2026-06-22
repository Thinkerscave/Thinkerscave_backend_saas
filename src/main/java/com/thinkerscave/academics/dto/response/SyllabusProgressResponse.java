package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SyllabusProgressResponse {
    private Long syllabusId;
    private String title;
    private int totalTopics;
    private int completedTopics;
    private int inProgressTopics;
    private int notStartedTopics;
    private double completionPercentage;
}
