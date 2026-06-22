package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ChapterResponse {
    private Long chapterId;
    private Integer chapterNumber;
    private String chapterName;
    private Integer estimatedHours;
    private Integer displayOrder;
    private Boolean active;
    private List<TopicResponse> topics;
}
